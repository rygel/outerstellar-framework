package io.github.rygel.outerstellar.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

/**
 * Simple GZIP compressor for SQL files.
 */
public final class GzipCompressor {

    private static final Logger LOG = LoggerFactory.getLogger(GzipCompressor.class);

    private GzipCompressor() {
        // Utility class
    }

    /**
     * GZIP compresses a file.
     *
     * @param inputPath path to the input file
     * @param outputPath path for the compressed output
     * @throws IOException if file operation fails
     */
    public static void compress(Path inputPath, Path outputPath) throws IOException {
        LOG.info("GZIP compressing:");
        LOG.info("  Input:  {}", inputPath.toAbsolutePath());
        LOG.info("  Output: {}", outputPath.toAbsolutePath());
        LOG.info("  Processing...");

        long startTime = System.currentTimeMillis();
        long bytesRead = 0;

        try (InputStream in = Files.newInputStream(inputPath);
             OutputStream out = new GZIPOutputStream(Files.newOutputStream(outputPath))) {

            byte[] buffer = new byte[8192];
            int bytes;
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
                bytesRead += bytes;

                // Progress indicator every 10MB
                if (bytesRead % (10 * 1024 * 1024) < 8192) {
                    LOG.info("  Progress: {} MB read", bytesRead / 1024 / 1024);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long originalSize = Files.size(inputPath);
        long compressedSize = Files.size(outputPath);
        double ratio = (1.0 - (double) compressedSize / originalSize) * 100;

        LOG.info("");
        LOG.info("Compression complete:");
        LOG.info("  Original size:  {} bytes ({} MB)", String.format("%,d", originalSize),
            String.format("%,.1f", originalSize / 1024.0 / 1024.0));
        LOG.info("  Compressed:     {} bytes ({} MB)", String.format("%,d", compressedSize),
            String.format("%,.1f", compressedSize / 1024.0 / 1024.0));
        LOG.info("  Reduction:      {}%", String.format("%.1f", ratio));
        LOG.info("  Time:           {} seconds", String.format("%,.1f", (endTime - startTime) / 1000.0));
    }

    /**
     * Main entry point.
     *
     * @param args input-file [output-file]
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            LOG.info("Usage: GzipCompressor <input-file> [output-file]");
            LOG.info("Example: GzipCompressor data/world-cities-h2.sql");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = args.length > 1
            ? Paths.get(args[1])
            : Paths.get(inputPath.toString() + ".gz");

        if (!inputPath.toFile().exists()) {
            LOG.error("Error: Input file not found: {}", inputPath);
            System.exit(1);
        }

        try {
            compress(inputPath, outputPath);
        } catch (Exception e) {
            LOG.error("Error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
