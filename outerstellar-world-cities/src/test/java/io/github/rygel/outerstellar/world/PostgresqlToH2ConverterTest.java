package io.github.rygel.outerstellar.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PostgresqlToH2Converter.
 */
@Tag("unit")
class PostgresqlToH2ConverterTest {

    private PostgresqlToH2Converter converter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        converter = new PostgresqlToH2Converter();
    }

    @Test
    void shouldConvertVarcharType() throws IOException {
        Path input = createTempFile("CREATE TABLE test (name character varying(255));");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("VARCHAR(255)");
        assertThat(content).doesNotContain("character varying");
    }

    @Test
    void shouldConvertTimestampType() throws IOException {
        Path input = createTempFile(
            "CREATE TABLE test (created_at timestamp without time zone);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("TIMESTAMP");
        assertThat(content).doesNotContain("timestamp without time zone");
    }

    @Test
    void shouldConvertDoublePrecisionType() throws IOException {
        Path input = createTempFile("CREATE TABLE test (value double precision);");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("DOUBLE");
        assertThat(content).doesNotContain("double precision");
    }

    @Test
    void shouldConvertTextType() throws IOException {
        Path input = createTempFile("CREATE TABLE test (description text);");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("CLOB");
        assertThat(content).doesNotContain(" text");
    }

    @Test
    void shouldConvertNumericType() throws IOException {
        Path input = createTempFile("CREATE TABLE test (amount numeric(10,2));");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("DECIMAL(10,2)");
        assertThat(content).doesNotContain("numeric(10,2)");
    }

    @Test
    void shouldRemovePublicSchemaPrefix() throws IOException {
        Path input = createTempFile(
            "INSERT INTO public.cities (id, name) VALUES (1, 'Berlin');"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("INSERT INTO cities");
        assertThat(content).doesNotContain("public.cities");
    }

    @Test
    void shouldRemoveOwnerStatements() throws IOException {
        Path input = createTempFile(
            "ALTER TABLE cities OWNER TO postgres;",
            "CREATE TABLE cities (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).doesNotContain("OWNER TO");
        assertThat(content).contains("CREATE TABLE cities");
    }

    @Test
    void shouldRemoveSetStatements() throws IOException {
        Path input = createTempFile(
            "SET statement_timeout = 0;",
            "SET client_encoding = 'UTF8';",
            "CREATE TABLE test (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).doesNotContain("SET statement_timeout");
        assertThat(content).doesNotContain("SET client_encoding");
        assertThat(content).contains("CREATE TABLE test");
    }

    @Test
    void shouldRemoveCommentStatements() throws IOException {
        Path input = createTempFile(
            "COMMENT ON COLUMN cities.name IS 'City name';",
            "CREATE TABLE cities (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).doesNotContain("COMMENT ON");
        assertThat(content).contains("CREATE TABLE cities");
    }

    @Test
    void shouldSkipDumpComments() throws IOException {
        Path input = createTempFile(
            "-- Dumped from database version 14.2",
            "-- PostgreSQL database dump",
            "CREATE TABLE test (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).doesNotContain("Dumped from database");
        assertThat(content).doesNotContain("PostgreSQL database dump");
        assertThat(content).contains("CREATE TABLE test");
    }

    @Test
    void shouldConvertTimestampDefault() throws IOException {
        Path input = createTempFile(
            "CREATE TABLE test (created_at TIMESTAMP DEFAULT '2024-01-01 00:00:00'::timestamp without time zone);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        // The converter converts the type and removes the PostgreSQL cast syntax
        assertThat(content).contains("TIMESTAMP");
        assertThat(content).doesNotContain("::timestamp without time zone");
    }

    @Test
    void shouldAddHeaderAndFooter() throws IOException {
        Path input = createTempFile("CREATE TABLE test (id BIGINT);");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).startsWith("-- Converted from PostgreSQL to H2");
        assertThat(content).contains("SET MODE PostgreSQL;");
        assertThat(content).contains("-- Conversion complete");
    }

    @Test
    void shouldRemovePgCatalogStatements() throws IOException {
        Path input = createTempFile(
            "SELECT pg_catalog.set_config('search_path', '', false);",
            "CREATE TABLE test (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).doesNotContain("pg_catalog.set_config");
        assertThat(content).contains("CREATE TABLE test");
    }

    @Test
    void shouldHandleEmptyLines() throws IOException {
        Path input = createTempFile(
            "",
            "CREATE TABLE test (id BIGINT);",
            "",
            "INSERT INTO test VALUES (1);",
            ""
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("CREATE TABLE test");
        assertThat(content).contains("INSERT INTO test");
    }

    @Test
    void shouldPreserveRegularComments() throws IOException {
        Path input = createTempFile(
            "-- This is a regular comment",
            "CREATE TABLE test (id BIGINT);"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("-- This is a regular comment");
    }

    @Test
    void shouldConvertCharType() throws IOException {
        Path input = createTempFile("CREATE TABLE test (code char(2));");
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("CHAR(2)");
    }

    @Test
    void shouldConvertIntegerTypes() throws IOException {
        Path input = createTempFile(
            "CREATE TABLE test (",
            "    big_val bigint,",
            "    int_val integer,",
            "    small_val smallint",
            ");"
        );
        Path output = tempDir.resolve("output.sql");

        converter.convert(input, output);

        String content = Files.readString(output);
        assertThat(content).contains("BIGINT");
        assertThat(content).contains("INTEGER");
        assertThat(content).contains("SMALLINT");
    }

    private Path createTempFile(String... lines) throws IOException {
        Path file = tempDir.resolve("input.sql");
        Files.write(file, java.util.Arrays.asList(lines));
        return file;
    }
}
