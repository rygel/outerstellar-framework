package io.github.rygel.outerstellar.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone converter for world cities PostgreSQL dump to H2 format.
 */
public final class ConvertWorldCities {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertWorldCities.class);

    private ConvertWorldCities() {
        // Utility class
    }

    /**
     * Main entry point for the converter.
     *
     * @param args command line arguments: <input-sql-file> <output-sql-file>
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.info("Usage: ConvertWorldCities <input-postgresql-sql> <output-h2-sql>");
            LOG.info("Example: ConvertWorldCities data/world.sql/world.sql data/world-cities-h2.sql");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);

        if (!inputPath.toFile().exists()) {
            LOG.error("Input file not found: {}", inputPath);
            System.exit(1);
        }

        try {
            LOG.info("Converting PostgreSQL SQL to H2 format...");
            LOG.info("Input:  {}", inputPath.toAbsolutePath());
            LOG.info("Output: {}", outputPath.toAbsolutePath());

            PostgresqlToH2Converter converter = new PostgresqlToH2Converter();
            converter.convert(inputPath, outputPath);

            LOG.info("Conversion complete!");
            LOG.info("Output file: {}", outputPath.toAbsolutePath());
            LOG.info("File size: {} bytes", outputPath.toFile().length());

        } catch (Exception e) {
            LOG.error("Conversion failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
