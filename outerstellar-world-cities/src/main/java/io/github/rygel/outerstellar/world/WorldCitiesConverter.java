package io.github.rygel.outerstellar.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Unified pipeline for converting PostgreSQL world cities dump to compressed H2 database.
 * 
 * This class provides a single entry point that orchestrates the entire conversion process:
 * 1. Convert PostgreSQL SQL syntax to H2-compatible format
 * 2. Optionally compress the SQL file with GZIP
 * 3. Create a compressed H2 database from the SQL
 * 
 * Usage:
 * <pre>
 * // Simple conversion
 * WorldCitiesConverter.convert(
 *     Paths.get("world.sql"),
 *     Paths.get("world-cities")
 * );
 * 
 * // With options
 * ConversionOptions options = ConversionOptions.builder()
 *     .compact(true)
 *     .gzipIntermediate(true)
 *     .build();
 *     
 * WorldCitiesConverter.convert(
 *     Paths.get("world.sql"),
 *     Paths.get("world-cities"),
 *     options
 * );
 * </pre>
 */
public final class WorldCitiesConverter {

    private static final Logger LOG = LoggerFactory.getLogger(WorldCitiesConverter.class);

    private WorldCitiesConverter() {
        // Utility class
    }

    /**
     * Converts PostgreSQL world cities SQL to compressed H2 database with default options.
     *
     * @param postgresqlSqlPath path to the PostgreSQL SQL dump file
     * @param outputDbPath path for the output H2 database (without .mv.db extension)
     * @throws IOException if file operation fails
     * @throws SQLException if database operation fails
     */
    public static void convert(Path postgresqlSqlPath, Path outputDbPath) 
            throws IOException, SQLException {
        convert(postgresqlSqlPath, outputDbPath, ConversionOptions.defaults());
    }

    /**
     * Converts PostgreSQL world cities SQL to compressed H2 database with custom options.
     *
     * @param postgresqlSqlPath path to the PostgreSQL SQL dump file
     * @param outputDbPath path for the output H2 database (without .mv.db extension)
     * @param options conversion options
     * @throws IOException if file operation fails
     * @throws SQLException if database operation fails
     */
    public static void convert(Path postgresqlSqlPath, Path outputDbPath, ConversionOptions options) 
            throws IOException, SQLException {
        
        long startTime = System.currentTimeMillis();
        
        LOG.info("Starting world cities conversion pipeline");
        LOG.info("Input: {}", postgresqlSqlPath);
        LOG.info("Output: {}", outputDbPath);
        LOG.info("Options: {}", options);

        // Step 1: Convert PostgreSQL to H2 SQL
        LOG.info("Step 1/3: Converting PostgreSQL SQL to H2 format...");
        Path h2SqlPath = convertToH2Sql(postgresqlSqlPath, options);
        
        // Step 2: Optionally compress SQL file
        Path sqlToImport = h2SqlPath;
        if (options.gzipIntermediate()) {
            LOG.info("Step 2/3: Compressing SQL file...");
            sqlToImport = compressSqlFile(h2SqlPath);
        } else {
            LOG.info("Step 2/3: Skipping SQL compression (disabled)");
        }
        
        // Step 3: Create compressed H2 database
        LOG.info("Step 3/3: Creating compressed H2 database...");
        createH2Database(sqlToImport, outputDbPath, options);
        
        // Cleanup temporary files if needed
        if (options.cleanupIntermediate()) {
            cleanupIntermediateFiles(h2SqlPath);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        LOG.info("Conversion complete in {} seconds", String.format("%.1f", duration / 1000.0));
        
        // Print statistics
        printStatistics(outputDbPath);
    }

    /**
     * Converts PostgreSQL SQL to H2-compatible SQL.
     */
    private static Path convertToH2Sql(Path postgresqlSqlPath, ConversionOptions options) 
            throws IOException {
        Path h2SqlPath = options.workingDirectory().resolve("world-cities-h2-converted.sql");
        
        PostgresqlToH2Converter converter = new PostgresqlToH2Converter();
        converter.convert(postgresqlSqlPath, h2SqlPath);
        
        LOG.info("H2 SQL created: {} ({} MB)", 
            h2SqlPath, 
            String.format("%.1f", Files.size(h2SqlPath) / 1024.0 / 1024.0));
        
        return h2SqlPath;
    }

    /**
     * GZIP compresses the SQL file.
     */
    private static Path compressSqlFile(Path sqlPath) throws IOException {
        Path gzippedPath = Paths.get(sqlPath.toString() + ".gz");
        
        DatabaseCompressor.gzipCompress(sqlPath, gzippedPath);
        
        LOG.info("SQL compressed: {} ({} MB)",
            gzippedPath,
            String.format("%.1f", Files.size(gzippedPath) / 1024.0 / 1024.0));
        
        // Delete uncompressed version
        Files.deleteIfExists(sqlPath);
        
        return gzippedPath;
    }

    /**
     * Creates the compressed H2 database from SQL.
     */
    private static void createH2Database(Path sqlPath, Path outputDbPath, ConversionOptions options) 
            throws IOException, SQLException {
        
        CompressedWorldCitiesDb.createCompressedDatabase(sqlPath, outputDbPath, options.compact());
        
        LOG.info("H2 database created: {}.mv.db ({} MB)",
            outputDbPath,
            String.format("%.1f", Files.size(Paths.get(outputDbPath + ".mv.db")) / 1024.0 / 1024.0));
    }

    /**
     * Cleans up intermediate files.
     */
    private static void cleanupIntermediateFiles(Path... paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
                LOG.debug("Cleaned up: {}", path);
            } catch (IOException e) {
                LOG.warn("Failed to cleanup: {}", path, e);
            }
        }
    }

    /**
     * Prints database statistics.
     */
    private static void printStatistics(Path outputDbPath) throws SQLException {
        String stats = CompressedWorldCitiesDb.getStatistics(outputDbPath.toString());
        LOG.info("\n{}", stats);
    }

    /**
     * Command-line entry point.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);

        if (!inputPath.toFile().exists()) {
            LOG.error("Error: Input file not found: {}", inputPath);
            System.exit(1);
        }

        // Parse options
        ConversionOptions.Builder optionsBuilder = ConversionOptions.builder();
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--compact":
                    optionsBuilder.compact(true);
                    break;
                case "--full":
                    optionsBuilder.compact(false);
                    break;
                case "--gzip":
                    optionsBuilder.gzipIntermediate(true);
                    break;
                case "--no-gzip":
                    optionsBuilder.gzipIntermediate(false);
                    break;
                case "--cleanup":
                    optionsBuilder.cleanupIntermediate(true);
                    break;
                case "--no-cleanup":
                    optionsBuilder.cleanupIntermediate(false);
                    break;
                default:
                    LOG.warn("Warning: Unknown option: {}", args[i]);
            }
        }

        try {
            convert(inputPath, outputPath, optionsBuilder.build());
        } catch (Exception e) {
            LOG.error("Conversion failed", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        LOG.info("WorldCitiesConverter - Unified PostgreSQL to H2 conversion pipeline");
        LOG.info("");
        LOG.info("Usage: WorldCitiesConverter <input-postgresql-sql> <output-db-path> [options]");
        LOG.info("");
        LOG.info("Arguments:");
        LOG.info("  input-postgresql-sql  Path to PostgreSQL SQL dump file");
        LOG.info("  output-db-path        Path for H2 database (without .mv.db extension)");
        LOG.info("");
        LOG.info("Options:");
        LOG.info("  --compact       Create compact version with essential columns only");
        LOG.info("  --full          Create full version with all columns (default)");
        LOG.info("  --gzip          GZIP compress intermediate SQL file");
        LOG.info("  --no-gzip       Don't GZIP compress intermediate SQL file (default)");
        LOG.info("  --cleanup       Delete intermediate files after conversion (default)");
        LOG.info("  --no-cleanup    Keep intermediate files for debugging");
        LOG.info("");
        LOG.info("Examples:");
        LOG.info("  WorldCitiesConverter data/world.sql output/world-cities");
        LOG.info("  WorldCitiesConverter data/world.sql output/world-cities --compact --gzip");
        LOG.info("  WorldCitiesConverter data/world.sql output/world-cities --full --no-gzip --no-cleanup");
    }
}
