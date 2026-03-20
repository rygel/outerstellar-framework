package de.outerstellar.world;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts PostgreSQL SQL dump to H2-compatible SQL.
 * Handles data type conversions, syntax differences, and PostgreSQL-specific features.
 */
public class PostgresqlToH2Converter {

    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "(character varying|varchar)\\((\\d+)\\)|"
        + "(character|char)\\((\\d+)\\)|"
        + "(numeric|decimal)\\((\\d+)(?:,\\s*(\\d+))?\\)|"
        + "(timestamp without time zone|timestamp with time zone)|"
        + "(double precision)|"
        + "(bigint)|"
        + "(integer)|"
        + "(smallint)|"
        + "(text)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEFAULT_TIMESTAMP_PATTERN = Pattern.compile(
        "DEFAULT\\s+'([^']+)'::timestamp\\s+without\\s+time\\s+zone",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IDENTITY_PATTERN = Pattern.compile(
        "ALTER\\s+TABLE\\s+(\\w+)\\.?(\\w+)?\\s+ALTER\\s+COLUMN\\s+(\\w+)\\s+ADD\\s+GENERATED\\s+BY\\s+DEFAULT\\s+AS\\s+IDENTITY",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OWNER_PATTERN = Pattern.compile(
        "ALTER\\s+(?:TABLE|SEQUENCE)\\s+.*?OWNER\\s+TO\\s+\\w+;",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "COMMENT\\s+ON\\s+(?:COLUMN|TABLE|CONSTRAINT)\\s+.*?;",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern RESTRICT_PATTERN = Pattern.compile(
        "^\\\\restrict.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNRESTRICT_PATTERN = Pattern.compile(
        "^\\\\unrestrict.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SELECT_CONFIG_PATTERN = Pattern.compile(
        "SELECT\\s+pg_catalog\\.set_config\\([^)]+\\);",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SET_STATEMENT_PATTERN = Pattern.compile(
        "SET\\s+(?:statement_timeout|lock_timeout|idle_in_transaction_session_timeout|"
        + "client_encoding|standard_conforming_strings|check_function_bodies|"
        + "xmloption|client_min_messages|row_security|default_tablespace|"
        + "default_table_access_method)\\s*=\\s*[^;]+;",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Converts a PostgreSQL SQL file to H2-compatible SQL.
     *
     * @param inputPath Path to the PostgreSQL SQL file
     * @param outputPath Path where the H2-compatible SQL will be written
     * @throws IOException if an I/O error occurs
     */
    public void convert(Path inputPath, Path outputPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(inputPath);
             BufferedWriter writer = Files.newBufferedWriter(outputPath)) {

            writer.write("-- Converted from PostgreSQL to H2");
            writer.newLine();
            writer.write("SET MODE PostgreSQL;");
            writer.newLine();
            writer.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String converted = convertLine(line);
                if (converted != null) {
                    writer.write(converted);
                    writer.newLine();
                }
            }

            writer.newLine();
            writer.write("-- Conversion complete");
            writer.newLine();
        }
    }

    /**
     * Converts a single line of SQL from PostgreSQL to H2 syntax.
     *
     * @param line the SQL line to convert
     * @return the converted line, or null if the line should be skipped
     */
    private String convertLine(String line) {
        // Skip PostgreSQL-specific commands
        if (RESTRICT_PATTERN.matcher(line).matches()
            || UNRESTRICT_PATTERN.matcher(line).matches()
            || line.trim().startsWith("-- Dumped from")
            || line.trim().startsWith("-- PostgreSQL database dump")) {
            return null;
        }

        // Skip owner statements
        if (OWNER_PATTERN.matcher(line).matches()) {
            return null;
        }

        // Skip comments
        if (COMMENT_PATTERN.matcher(line).matches()) {
            return null;
        }

        // Skip SET statements
        if (SET_STATEMENT_PATTERN.matcher(line).matches()) {
            return null;
        }

        // Skip pg_catalog.set_config
        if (SELECT_CONFIG_PATTERN.matcher(line).matches()) {
            return null;
        }

        // Convert data types
        String result = convertDataTypes(line);

        // Convert default timestamps
        result = convertDefaultTimestamps(result);

        // Convert public schema references
        result = result.replaceAll("\\bpublic\\.", "");

        // Convert IDENTITY syntax
        result = convertIdentity(result);

        // Remove trailing whitespace
        result = result.trim();

        return result.isEmpty() ? null : result;
    }

    /**
     * Converts PostgreSQL data types to H2 equivalents.
     */
    private String convertDataTypes(String line) {
        Matcher matcher = TYPE_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String replacement;

            if (matcher.group(1) != null) {
                // character varying(n) -> VARCHAR(n)
                replacement = "VARCHAR(" + matcher.group(2) + ")";
            } else if (matcher.group(3) != null) {
                // character(n) -> CHAR(n)
                replacement = "CHAR(" + matcher.group(4) + ")";
            } else if (matcher.group(5) != null) {
                // numeric/decima(p,s) -> DECIMAL(p,s)
                if (matcher.group(7) != null) {
                    replacement = "DECIMAL(" + matcher.group(6) + "," + matcher.group(7) + ")";
                } else {
                    replacement = "DECIMAL(" + matcher.group(6) + ")";
                }
            } else if (matcher.group(8) != null) {
                // timestamp without time zone / with time zone -> TIMESTAMP
                replacement = "TIMESTAMP";
            } else if (matcher.group(9) != null) {
                // double precision -> DOUBLE
                replacement = "DOUBLE";
            } else if (matcher.group(10) != null) {
                // bigint -> BIGINT
                replacement = "BIGINT";
            } else if (matcher.group(11) != null) {
                // integer -> INTEGER
                replacement = "INTEGER";
            } else if (matcher.group(12) != null) {
                // smallint -> SMALLINT
                replacement = "SMALLINT";
            } else if (matcher.group(13) != null) {
                // text -> CLOB
                replacement = "CLOB";
            } else {
                replacement = matcher.group();
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Converts PostgreSQL timestamp default values to H2 format.
     */
    private String convertDefaultTimestamps(String line) {
        Matcher matcher = DEFAULT_TIMESTAMP_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String timestamp = matcher.group(1);
            String replacement = "DEFAULT TIMESTAMP '" + timestamp + "'";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Converts PostgreSQL GENERATED BY DEFAULT AS IDENTITY to H2 IDENTITY.
     */
    private String convertIdentity(String line) {
        Matcher matcher = IDENTITY_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String tableName = matcher.group(2) != null ? matcher.group(2) : matcher.group(1);
            String columnName = matcher.group(3);
            String replacement = "ALTER TABLE " + tableName
                + " ALTER COLUMN " + columnName + " SET DEFAULT NULL;";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
