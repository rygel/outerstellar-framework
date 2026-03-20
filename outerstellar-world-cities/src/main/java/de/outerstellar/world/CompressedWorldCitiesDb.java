package de.outerstellar.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.GZIPInputStream;

/**
 * Creates and manages a compressed H2 database from the world cities SQL.
 * Uses H2's native compression features for optimal storage.
 */
public final class CompressedWorldCitiesDb {

    private static final Logger LOG = LoggerFactory.getLogger(CompressedWorldCitiesDb.class);

    private CompressedWorldCitiesDb() {
        // Utility class
    }

    /**
     * Creates a compressed H2 database from a gzipped or plain SQL file.
     *
     * @param sqlInputPath path to the SQL file (can be .sql or .sql.gz)
     * @param dbOutputPath path for the H2 database (without .mv.db extension)
     * @param compact if true, creates a compact version with only essential data
     * @throws IOException if file operation fails
     * @throws SQLException if database operation fails
     */
    public static void createCompressedDatabase(Path sqlInputPath, Path dbOutputPath,
                                                 boolean compact) throws IOException, SQLException {
        String dbPath = dbOutputPath.toString();
        String url = "jdbc:h2:file:" + dbPath
            + ";DB_CLOSE_DELAY=-1"
            + ";COMPRESS=TRUE"              // Compress data pages
            + ";PAGE_SIZE=4096"            // Optimize page size
            + ";CACHE_SIZE=8192";         // Reasonable cache

        LOG.info("Creating compressed H2 database:");
        LOG.info("  Input:  {}", sqlInputPath);
        LOG.info("  Output: {}", dbPath);
        LOG.info("  Compact mode: {}", compact);

        // Delete existing database files
        deleteIfExists(Paths.get(dbPath + ".mv.db"));
        deleteIfExists(Paths.get(dbPath + ".trace.db"));

        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {

            LOG.info("Importing SQL data...");

            // Execute the SQL script
            executeSqlScript(conn, sqlInputPath, compact);

            // Create indexes for better query performance
            LOG.info("Creating indexes...");
            createIndexes(stmt);

            // Compact the database
            LOG.info("Compacting database...");
            stmt.execute("CHECKPOINT");
            stmt.execute("SHUTDOWN COMPACT");

            LOG.info("Database creation complete");
        }

        // Report size
        long dbSize = Files.size(Paths.get(dbPath + ".mv.db"));
        LOG.info("Final database size: {} bytes ({} MB)",
            dbSize, String.format("%.1f", dbSize / 1024.0 / 1024.0));
    }

    /**
     * Executes an SQL script, optionally filtering for compact mode.
     */
    private static void executeSqlScript(Connection conn, Path sqlPath, boolean compact)
        throws IOException, SQLException {

        boolean isGzipped = sqlPath.toString().endsWith(".gz");

        try (InputStream fileIn = Files.newInputStream(sqlPath);
             InputStream gzipIn = isGzipped ? new GZIPInputStream(fileIn) : fileIn;
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn, StandardCharsets.UTF_8))) {

            Statement stmt = conn.createStatement();
            StringBuilder batch = new StringBuilder();
            String line;
            int batchCount = 0;
            int totalLines = 0;

            while ((line = reader.readLine()) != null) {
                totalLines++;

                // Skip comments and empty lines
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                // In compact mode, filter out unnecessary columns
                if (compact) {
                    line = filterForCompactMode(line);
                    if (line == null) {
                        continue;
                    }
                }

                batch.append(line).append(" ");

                if (trimmed.endsWith(";")) {
                    String sql = batch.toString().trim();
                    batch.setLength(0);

                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            batchCount++;

                            if (batchCount % 1000 == 0) {
                                LOG.debug("Executed {} statements...", batchCount);
                            }
                        } catch (SQLException e) {
                            LOG.warn("Failed to execute SQL (continuing): {}", e.getMessage());
                        }
                    }
                }
            }

            LOG.info("Executed {} SQL statements from {} lines", batchCount, totalLines);
        }
    }

    /**
     * Filters SQL statements for compact mode (removes optional columns).
     */
    private static String filterForCompactMode(String line) {
        // Remove columns we don't need from CREATE TABLE
        if (line.contains("native VARCHAR")
            || line.contains("translations CLOB")
            || line.contains("wikiDataId VARCHAR")
            || line.contains("flag SMALLINT")
            || line.contains("emoji VARCHAR")
            || line.contains("emojiU VARCHAR")
            || line.contains("gdp BIGINT")
            || line.contains("postal_code_format VARCHAR")
            || line.contains("postal_code_regex VARCHAR")
            || line.contains("tld VARCHAR")
            || line.contains("currency_name VARCHAR")
            || line.contains("currency_symbol VARCHAR")
            || line.contains("timezones CLOB")
            || line.contains("phonecode VARCHAR")
            || line.contains("nationality VARCHAR")
            || line.contains("area_sq_km DOUBLE")
            || line.contains("capital VARCHAR")
            || line.contains("region VARCHAR")
            || line.contains("subregion VARCHAR")
            || line.contains("type VARCHAR")
            || line.contains("level INTEGER")
            || line.contains("parent_id BIGINT")
            || line.contains("fipsCode VARCHAR")
            || line.contains("iso2 VARCHAR")
            || line.contains("iso3 CHAR")
            || line.contains("numeric_code CHAR")
            || line.contains("region_id BIGINT")
            || line.contains("subregion_id BIGINT")) {
            return null;
        }

        return line;
    }

    /**
     * Creates indexes for common query patterns.
     */
    private static void createIndexes(Statement stmt) throws SQLException {
        // Cities indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_name ON cities(name)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_country ON cities(country_code)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_coords ON cities(latitude, longitude)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_population ON cities(population DESC)");

        // Countries indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_countries_code ON countries(iso2)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_countries_name ON countries(name)");

        LOG.info("Indexes created successfully");
    }

    /**
     * Gets statistics about the database.
     *
     * @param dbPath path to the database (without .mv.db extension)
     * @return formatted statistics string
     * @throws SQLException if database operation fails
     */
    public static String getStatistics(String dbPath) throws SQLException {
        String url = "jdbc:h2:file:" + dbPath + ";DB_CLOSE_DELAY=-1";
        StringBuilder stats = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {

            stats.append("Compressed World Cities Database Statistics:\n");
            stats.append("=" .repeat(50)).append("\n");

            // Table counts
            String[] tables = {"CITIES", "COUNTRIES", "STATES", "REGIONS", "SUBREGIONS"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        stats.append(String.format("%-15s: %,10d rows%n",
                            table, rs.getLong(1)));
                    }
                } catch (SQLException e) {
                    stats.append(String.format("%-15s: not found%n", table));
                }
            }

            // Database size info
            stats.append("\n");
            try (ResultSet rs = stmt.executeQuery(
                "SELECT SETTING_NAME, SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS "
                    + "WHERE SETTING_NAME IN ('info.PAGE_SIZE', 'info.PAGE_COUNT')")) {
                while (rs.next()) {
                    stats.append(String.format("%-15s: %s%n",
                        rs.getString(1), rs.getString(2)));
                }
            }
        }

        return stats.toString();
    }

    /**
     * Deletes a file if it exists.
     */
    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("Could not delete file: {}", path);
        }
    }

    /**
     * Command-line entry point.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.info("CompressedWorldCitiesDb - Create compressed H2 database");
            LOG.info("");
            LOG.info("Usage: CompressedWorldCitiesDb <input-sql> <output-db> [options]");
            LOG.info("");
            LOG.info("Arguments:");
            LOG.info("  input-sql    Path to SQL file (.sql or .sql.gz)");
            LOG.info("  output-db    Path for H2 database (without .mv.db extension)");
            LOG.info("");
            LOG.info("Options:");
            LOG.info("  --compact    Create compact version with essential columns only");
            LOG.info("  --full       Create full version with all columns (default)");
            LOG.info("");
            LOG.info("Examples:");
            LOG.info("  CompressedWorldCitiesDb data/world-cities-h2.sql data/world-cities");
            LOG.info("  CompressedWorldCitiesDb data/world-cities-h2.sql.gz data/world-cities-compact --compact");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);
        boolean compact = false;

        // Parse options
        for (int i = 2; i < args.length; i++) {
            if ("--compact".equals(args[i])) {
                compact = true;
            } else if ("--full".equals(args[i])) {
                compact = false;
            }
        }

        if (!inputPath.toFile().exists()) {
            LOG.error("Error: Input file not found: {}", inputPath);
            System.exit(1);
        }

        try {
            createCompressedDatabase(inputPath, outputPath, compact);
            LOG.info(getStatistics(outputPath.toString()));
        } catch (Exception e) {
            LOG.error("Error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
