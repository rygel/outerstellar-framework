package io.github.rygel.outerstellar.world;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Imports world cities data from PostgreSQL dump into H2 database.
 * Provides both command-line and programmatic interfaces.
 */
public class WorldCitiesImporter {

    private static final Logger LOG = LoggerFactory.getLogger(WorldCitiesImporter.class);
    private static final int BATCH_SIZE = 1000;

    private final DataSource dataSource;
    private final PostgresqlToH2Converter converter;

    /**
     * Creates a new importer with the specified H2 database URL.
     *
     * @param h2Url JDBC URL for the H2 database (e.g., "jdbc:h2:file:./world-cities")
     */
    public WorldCitiesImporter(String h2Url) {
        this(h2Url, null, null);
    }

    /**
     * Creates a new importer with the specified H2 database URL and credentials.
     *
     * @param h2Url JDBC URL for the H2 database
     * @param username database username (can be null)
     * @param password database password (can be null)
     */
    public WorldCitiesImporter(String h2Url, String username, String password) {
        this.dataSource = createDataSource(h2Url, username, password);
        this.converter = new PostgresqlToH2Converter();
    }

    /**
     * Creates a new importer with an existing DataSource.
     *
     * @param dataSource the DataSource to use
     */
    public WorldCitiesImporter(DataSource dataSource) {
        this.dataSource = dataSource;
        this.converter = new PostgresqlToH2Converter();
    }

    private DataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }
        config.setMaximumPoolSize(5);
        config.setAutoCommit(false);

        Properties props = new Properties();
        props.setProperty("MODE", "PostgreSQL");
        config.setDataSourceProperties(props);

        return new HikariDataSource(config);
    }

    /**
     * Imports the world cities SQL file into the H2 database.
     *
     * @param postgresqlSqlPath path to the PostgreSQL SQL dump file
     * @return the number of rows imported
     * @throws IOException if an I/O error occurs
     * @throws SQLException if a database error occurs
     */
    public int importFromFile(Path postgresqlSqlPath) throws IOException, SQLException {
        LOG.info("Converting PostgreSQL SQL to H2 format...");
        Path convertedSql = Files.createTempFile("world-cities-h2-", ".sql");

        try {
            converter.convert(postgresqlSqlPath, convertedSql);
            LOG.info("Conversion complete. SQL written to: {}", convertedSql);

            return executeSqlFile(convertedSql);
        } finally {
            try {
                Files.deleteIfExists(convertedSql);
            } catch (IOException e) {
                LOG.warn("Failed to delete temporary file: {}", convertedSql, e);
            }
        }
    }

    /**
     * Imports the world cities SQL from an InputStream into the H2 database.
     *
     * @param postgresqlSqlStream InputStream containing the PostgreSQL SQL dump
     * @return the number of rows imported
     * @throws IOException if an I/O error occurs
     * @throws SQLException if a database error occurs
     */
    public int importFromStream(InputStream postgresqlSqlStream) throws IOException, SQLException {
        Path tempInput = Files.createTempFile("world-cities-pg-", ".sql");
        Files.copy(postgresqlSqlStream, tempInput,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try {
            return importFromFile(tempInput);
        } finally {
            try {
                Files.deleteIfExists(tempInput);
            } catch (IOException e) {
                LOG.warn("Failed to delete temporary file: {}", tempInput, e);
            }
        }
    }

    /**
     * Executes a SQL file against the database.
     */
    private int executeSqlFile(Path sqlFile) throws IOException, SQLException {
        LOG.info("Executing SQL file: {}", sqlFile);
        int totalRows = 0;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = Files.newBufferedReader(sqlFile)) {

            StringBuilder batch = new StringBuilder();
            String line;
            int statementCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }

                batch.append(line).append(" ");

                if (line.endsWith(";")) {
                    String sql = batch.toString().trim();
                    batch.setLength(0);

                    if (!sql.isEmpty()) {
                        try {
                            if (sql.toLowerCase().startsWith("insert")) {
                                stmt.addBatch(sql);
                                statementCount++;

                                if (statementCount % BATCH_SIZE == 0) {
                                    int[] results = stmt.executeBatch();
                                    totalRows += sum(results);
                                    conn.commit();
                                    LOG.debug("Executed batch of {} statements, total rows: {}",
                                        BATCH_SIZE, totalRows);
                                }
                            } else {
                                stmt.execute(sql);
                            }
                        } catch (SQLException e) {
                            LOG.warn("Failed to execute SQL: {}", sql.substring(0,
                                Math.min(100, sql.length())), e);
                            // Continue with next statement
                        }
                    }
                }
            }

            // Execute remaining batch
            if (statementCount % BATCH_SIZE != 0) {
                int[] results = stmt.executeBatch();
                totalRows += sum(results);
                conn.commit();
            }

            LOG.info("SQL execution complete. Total rows affected: {}", totalRows);
            return totalRows;
        }
    }

    /**
     * Sums an array of integers, treating negative values (errors) as 0.
     */
    private int sum(int[] values) {
        int sum = 0;
        for (int value : values) {
            if (value > 0) {
                sum += value;
            }
        }
        return sum;
    }

    /**
     * Checks if the database has been initialized with world cities data.
     *
     * @return true if the cities table exists and has data
     */
    public boolean isInitialized() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_NAME = 'CITIES'")) {

            if (rs.next() && rs.getInt(1) > 0) {
                try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM CITIES")) {
                    if (countRs.next()) {
                        return countRs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            LOG.debug("Database not initialized", e);
        }
        return false;
    }

    /**
     * Gets statistics about the imported data.
     *
     * @return a formatted string with statistics
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("World Cities Database Statistics:\n");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String[] tables = {"CITIES", "COUNTRIES", "STATES", "REGIONS", "SUBREGIONS"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        stats.append(String.format("  %s: %,d rows%n",
                            table, rs.getInt(1)));
                    }
                } catch (SQLException e) {
                    stats.append(String.format("  %s: not found%n", table));
                }
            }
        } catch (SQLException e) {
            stats.append("  Error: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Command-line entry point.
     * Usage: java WorldCitiesImporter <postgresql-sql-file> <h2-database-path>
     */
    /**
     * Command-line entry point.
     * Usage: java WorldCitiesImporter <postgresql-sql-file> <h2-database-path>
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.info("Usage: WorldCitiesImporter <postgresql-sql-file> <h2-database-path>");
            LOG.info("Example: WorldCitiesImporter data/world.sql/world.sql jdbc:h2:file:./world-cities");
            System.exit(1);
        }

        Path sqlFile = Paths.get(args[0]);
        String h2Url = args[1];

        if (!Files.exists(sqlFile)) {
            LOG.error("SQL file not found: {}", sqlFile);
            System.exit(1);
        }

        if (!h2Url.startsWith("jdbc:h2:")) {
            h2Url = "jdbc:h2:file:" + h2Url;
        }

        try {
            LOG.info("Starting import from {} to {}", sqlFile, h2Url);

            WorldCitiesImporter importer = new WorldCitiesImporter(h2Url);
            int rows = importer.importFromFile(sqlFile);

            LOG.info("Import complete!");
            LOG.info("Rows imported: {}", rows);
            LOG.info(importer.getStatistics());

        } catch (Exception e) {
            LOG.error("Import failed", e);
            System.exit(1);
        }
    }
}
