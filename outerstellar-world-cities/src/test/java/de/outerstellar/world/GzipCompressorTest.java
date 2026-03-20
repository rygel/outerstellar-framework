package de.outerstellar.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GzipCompressor.
 */
@Tag("unit")
class GzipCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompressFile() throws IOException {
        Path input = tempDir.resolve("test.sql");
        String content = "CREATE TABLE test (id BIGINT); INSERT INTO test VALUES (1);";
        Files.writeString(input, content);
        Path output = tempDir.resolve("test.sql.gz");

        GzipCompressor.compress(input, output);

        assertThat(output).exists();
        assertThat(Files.size(output)).isGreaterThan(0);
    }

    @Test
    void shouldProduceValidGzipFile() throws IOException {
        Path input = tempDir.resolve("test.sql");
        String originalContent = "SELECT * FROM cities WHERE name = 'Berlin';";
        Files.writeString(input, originalContent);
        Path output = tempDir.resolve("test.sql.gz");

        GzipCompressor.compress(input, output);

        // Verify we can decompress it
        try (GZIPInputStream gzipIn = new GZIPInputStream(Files.newInputStream(output))) {
            String decompressed = new String(gzipIn.readAllBytes());
            assertThat(decompressed).isEqualTo(originalContent);
        }
    }

    @Test
    void shouldCompressLargeFile() throws IOException {
        Path input = tempDir.resolve("large.sql");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("INSERT INTO cities (id, name) VALUES (").append(i).append(", 'City").append(i).append("');\n");
        }
        Files.writeString(input, largeContent.toString());
        Path output = tempDir.resolve("large.sql.gz");

        GzipCompressor.compress(input, output);

        assertThat(output).exists();
        assertThat(Files.size(output)).isLessThan(Files.size(input));
    }

    @Test
    void shouldHandleEmptyFile() throws IOException {
        Path input = tempDir.resolve("empty.sql");
        Files.createFile(input);
        Path output = tempDir.resolve("empty.sql.gz");

        GzipCompressor.compress(input, output);

        assertThat(output).exists();
        // Empty file compressed should still be small
        assertThat(Files.size(output)).isLessThan(100);
    }
}
