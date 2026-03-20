package de.outerstellar.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DatabaseCompressor.
 */
@Tag("unit")
class DatabaseCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGzipCompressFile() throws IOException {
        Path input = tempDir.resolve("test.sql");
        Files.writeString(input, "CREATE TABLE test (id BIGINT); INSERT INTO test VALUES (1);");
        Path output = tempDir.resolve("test.sql.gz");

        DatabaseCompressor.gzipCompress(input, output);

        assertThat(output).exists();
        assertThat(Files.size(output)).isGreaterThan(0);
    }

    @Test
    void shouldRemoveOptionalColumnsInCompactMode() throws IOException {
        Path input = tempDir.resolve("input.sql");
        Files.writeString(input, String.join("\n",
            "CREATE TABLE countries (",
            "    id BIGINT,",
            "    name VARCHAR(255),",
            "    native VARCHAR(255),",
            "    emoji VARCHAR(10)",
            ");"
        ));
        Path output = tempDir.resolve("output.sql");

        DatabaseCompressor.compressSqlFile(input, output, true);

        String content = Files.readString(output);
        assertThat(content).contains("id BIGINT");
        assertThat(content).contains("name VARCHAR(255)");
        assertThat(content).doesNotContain("native VARCHAR(255)");
        assertThat(content).doesNotContain("emoji VARCHAR(10)");
    }

    @Test
    void shouldKeepAllColumnsInNonCompactMode() throws IOException {
        Path input = tempDir.resolve("input.sql");
        Files.writeString(input, String.join("\n",
            "CREATE TABLE countries (",
            "    id BIGINT,",
            "    name VARCHAR(255),",
            "    native VARCHAR(255)",
            ");"
        ));
        Path output = tempDir.resolve("output.sql");

        DatabaseCompressor.compressSqlFile(input, output, false);

        String content = Files.readString(output);
        assertThat(content).contains("id BIGINT");
        assertThat(content).contains("name VARCHAR(255)");
        assertThat(content).contains("native VARCHAR(255)");
    }

    @Test
    void shouldCalculateCompressionRatio() throws IOException {
        Path input = tempDir.resolve("test.sql");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("CREATE TABLE test").append(i).append(" (id BIGINT);\n");
        }
        Files.writeString(input, largeContent.toString());
        Path output = tempDir.resolve("test.sql.gz");

        DatabaseCompressor.gzipCompress(input, output);

        long originalSize = Files.size(input);
        long compressedSize = Files.size(output);
        double ratio = (1.0 - (double) compressedSize / originalSize) * 100;

        assertThat(ratio).isGreaterThan(0);
        assertThat(compressedSize).isLessThan(originalSize);
    }

    @Test
    void shouldSkipEmptyLines() throws IOException {
        Path input = tempDir.resolve("input.sql");
        Files.writeString(input, String.join("\n",
            "",
            "CREATE TABLE test (id BIGINT);",
            "",
            ""
        ));
        Path output = tempDir.resolve("output.sql");

        DatabaseCompressor.compressSqlFile(input, output, false);

        String content = Files.readString(output);
        assertThat(content).contains("CREATE TABLE test");
        // Empty lines should be skipped
        assertThat(content.lines().filter(String::isEmpty).count()).isZero();
    }

    @Test
    void shouldPreserveHeaderComments() throws IOException {
        Path input = tempDir.resolve("input.sql");
        Files.writeString(input, String.join("\n",
            "-- Converted from PostgreSQL to H2",
            "CREATE TABLE test (id BIGINT);"
        ));
        Path output = tempDir.resolve("output.sql");

        DatabaseCompressor.createCompactVersion(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("Converted from PostgreSQL");
    }

    @Test
    void shouldRemoveNonEssentialColumnsInMinimalMode() throws IOException {
        Path input = tempDir.resolve("input.sql");
        Files.writeString(input, String.join("\n",
            "CREATE TABLE cities (",
            "    id BIGINT,",
            "    name VARCHAR(255),",
            "    country_id BIGINT,",
            "    country_code VARCHAR(2),",
            "    latitude DOUBLE,",
            "    longitude DOUBLE,",
            "    population BIGINT,",
            "    timezone VARCHAR(50),",
            "    wikiDataId VARCHAR(50)",
            ");"
        ));
        Path output = tempDir.resolve("output.sql");

        DatabaseCompressor.createCompactVersion(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("id BIGINT");
        assertThat(content).contains("name VARCHAR(255)");
        assertThat(content).contains("country_id BIGINT");
        assertThat(content).contains("country_code VARCHAR(2)");
        assertThat(content).contains("latitude DOUBLE");
        assertThat(content).contains("longitude DOUBLE");
        assertThat(content).contains("population BIGINT");
        assertThat(content).contains("timezone VARCHAR(50)");
        assertThat(content).doesNotContain("wikiDataId");
    }
}
