package io.github.rygel.outerstellar.i18n.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core i18n validation engine. Compares keys across locale property files,
 * scans source code for key usage, and cross-references to find missing,
 * unused, and undefined keys.
 *
 * <p>Configurable via {@link Config}. Can be used from CLI, GUI, or Maven plugin.</p>
 */
public class I18nValidator {

    private final Config config;
    private final List<ValidationResult> results = new ArrayList<>();
    private final Map<String, Properties> allProperties = new LinkedHashMap<>();
    private final Set<String> sourceCodeKeys = new HashSet<>();

    public I18nValidator(Config config) {
        this.config = config;
    }

    public List<ValidationResult> validate(ProgressListener listener) {
        results.clear();
        allProperties.clear();
        sourceCodeKeys.clear();

        log(listener, "Loading translation files...");
        loadTranslationFiles(listener);

        log(listener, "Validating translations across locales...");
        validateTranslations(listener);

        if (config.projectPath() != null) {
            log(listener, "Scanning source code for key usage...");
            scanSourceCode(listener);

            log(listener, "Cross-referencing keys...");
            crossReferenceKeys(listener);
        }

        if (config.checkPlaceholders()) {
            log(listener, "Checking placeholder consistency...");
            validatePlaceholders(listener);
        }

        return List.copyOf(results);
    }

    // -- Step 1: Load property files ------------------------------------------

    private void loadTranslationFiles(ProgressListener listener) {
        File dir = new File(config.resourcesPath());
        if (!dir.exists() || !dir.isDirectory()) {
            results.add(ValidationResult.error("Resources directory does not exist: " + config.resourcesPath()));
            return;
        }

        String basePattern = config.baseFileName().replace(".", "\\.").replace("*", ".*");
        Pattern filePattern = Pattern.compile(
                basePattern.replace(".properties", "") + "_\\w+\\.properties"
        );

        // Load base file
        File baseFile = new File(dir, config.baseFileName());
        if (baseFile.exists()) {
            loadPropertiesFile(baseFile, "base", listener);
        }

        // Load locale files
        File[] files = dir.listFiles((d, name) -> filePattern.matcher(name).matches());
        if (files != null) {
            for (File file : files) {
                String lang = extractLanguage(file.getName());
                loadPropertiesFile(file, lang, listener);
            }
        }

        log(listener, "Loaded " + allProperties.size() + " property files");
    }

    private void loadPropertiesFile(File file, String lang, ProgressListener listener) {
        try {
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            }
            allProperties.put(lang, props);
            log(listener, "  " + file.getName() + ": " + props.size() + " keys");
        } catch (IOException e) {
            results.add(ValidationResult.error("Failed to load " + file.getName() + ": " + e.getMessage()));
        }
    }

    // -- Step 2: Validate translations across locales -------------------------

    private void validateTranslations(ProgressListener listener) {
        if (allProperties.isEmpty()) return;

        Set<String> allKeys = new TreeSet<>();
        for (Properties props : allProperties.values()) {
            allKeys.addAll(props.stringPropertyNames());
        }

        log(listener, "Total unique keys: " + allKeys.size());

        for (String key : allKeys) {
            if (isIgnored(key)) continue;

            List<String> missingIn = new ArrayList<>();
            List<String> blankIn = new ArrayList<>();

            for (var entry : allProperties.entrySet()) {
                if (!entry.getValue().containsKey(key)) {
                    missingIn.add(entry.getKey());
                } else if (entry.getValue().getProperty(key).isBlank()) {
                    blankIn.add(entry.getKey());
                }
            }

            if (!missingIn.isEmpty()) {
                results.add(new ValidationResult(
                        ValidationResult.Type.TRANSLATION, key,
                        ValidationResult.Status.MISSING,
                        "Missing in: " + String.join(", ", missingIn)
                ));
            }

            if (!blankIn.isEmpty()) {
                results.add(new ValidationResult(
                        ValidationResult.Type.TRANSLATION, key,
                        ValidationResult.Status.BLANK,
                        "Blank value in: " + String.join(", ", blankIn)
                ));
            }
        }
    }

    // -- Step 3: Scan source code ---------------------------------------------

    private void scanSourceCode(ProgressListener listener) {
        File projectDir = new File(config.projectPath());
        if (!projectDir.exists()) return;

        List<Pattern> patterns = new ArrayList<>();
        for (String p : config.sourcePatterns()) {
            // Build regex: methodName("captured_key")
            patterns.add(Pattern.compile(Pattern.quote(p) + "\\s*\\(\\s*[\"']([^\"']+)[\"']"));
        }

        List<File> sourceFiles = findSourceFiles(projectDir);
        log(listener, "Scanning " + sourceFiles.size() + " source files...");

        for (File file : sourceFiles) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        sourceCodeKeys.add(matcher.group(1));
                    }
                }
            } catch (IOException e) {
                // Skip unreadable files
            }
        }

        log(listener, "Found " + sourceCodeKeys.size() + " unique keys in source code");
    }

    // -- Step 4: Cross-reference ----------------------------------------------

    private void crossReferenceKeys(ProgressListener listener) {
        if (allProperties.isEmpty() || sourceCodeKeys.isEmpty()) return;

        // Use base properties as reference, fall back to first available
        Properties refProps = allProperties.getOrDefault("base",
                allProperties.getOrDefault("en",
                        allProperties.values().iterator().next()));

        // Unused: defined in properties but not in source code
        for (String key : refProps.stringPropertyNames()) {
            if (isIgnored(key)) continue;
            if (!sourceCodeKeys.contains(key)) {
                results.add(new ValidationResult(
                        ValidationResult.Type.USAGE, key,
                        ValidationResult.Status.UNUSED,
                        "Defined in properties but not found in source code"
                ));
            }
        }

        // Undefined: used in source code but not defined
        for (String key : sourceCodeKeys) {
            if (isIgnored(key)) continue;
            if (!refProps.containsKey(key)) {
                results.add(new ValidationResult(
                        ValidationResult.Type.USAGE, key,
                        ValidationResult.Status.UNDEFINED,
                        "Used in source code but not defined in properties"
                ));
            }
        }
    }

    // -- Step 5: Placeholder consistency --------------------------------------

    private void validatePlaceholders(ProgressListener listener) {
        Properties refProps = allProperties.getOrDefault("base",
                allProperties.getOrDefault("en", null));
        if (refProps == null) return;

        Pattern placeholderPattern = Pattern.compile("\\{(\\d+)}");

        for (String key : refProps.stringPropertyNames()) {
            if (isIgnored(key)) continue;

            String refValue = refProps.getProperty(key);
            Set<String> refPlaceholders = extractPlaceholders(refValue, placeholderPattern);
            if (refPlaceholders.isEmpty()) continue;

            for (var entry : allProperties.entrySet()) {
                if (entry.getKey().equals("base") || entry.getKey().equals("en")) continue;
                String localeValue = entry.getValue().getProperty(key);
                if (localeValue == null) continue;

                Set<String> localePlaceholders = extractPlaceholders(localeValue, placeholderPattern);
                if (!refPlaceholders.equals(localePlaceholders)) {
                    results.add(new ValidationResult(
                            ValidationResult.Type.PLACEHOLDER, key,
                            ValidationResult.Status.PLACEHOLDER_MISMATCH,
                            entry.getKey() + " has " + localePlaceholders + " but base has " + refPlaceholders
                    ));
                }
            }
        }
    }

    private Set<String> extractPlaceholders(String value, Pattern pattern) {
        Set<String> placeholders = new TreeSet<>();
        Matcher m = pattern.matcher(value);
        while (m.find()) {
            placeholders.add(m.group(0));
        }
        return placeholders;
    }

    // -- Helpers --------------------------------------------------------------

    private boolean isIgnored(String key) {
        for (String pattern : config.ignorePatterns()) {
            if (key.matches(pattern.replace("*", ".*"))) return true;
        }
        return false;
    }

    private List<File> findSourceFiles(File directory) {
        List<File> files = new ArrayList<>();
        if (!directory.isDirectory()) return files;

        File[] children = directory.listFiles();
        if (children == null) return files;

        for (File child : children) {
            String name = child.getName();
            if (child.isDirectory()) {
                if (!name.equals("target") && !name.equals(".git") && !name.equals("node_modules")
                        && !name.equals("build") && !name.equals(".gradle")) {
                    files.addAll(findSourceFiles(child));
                }
            } else {
                for (String ext : config.scanExtensions()) {
                    if (name.endsWith("." + ext)) {
                        files.add(child);
                        break;
                    }
                }
            }
        }

        return files;
    }

    private String extractLanguage(String filename) {
        String base = config.baseFileName().replace(".properties", "");
        if (filename.startsWith(base + "_") && filename.endsWith(".properties")) {
            return filename.substring(base.length() + 1, filename.length() - 11);
        }
        return filename;
    }

    private void log(ProgressListener listener, String message) {
        if (listener != null) listener.onProgress(message);
    }

    // -- Statistics ------------------------------------------------------------

    public Statistics getStatistics() {
        int ok = 0, missing = 0, blank = 0, unused = 0, undefined = 0, placeholderMismatch = 0;
        for (ValidationResult r : results) {
            switch (r.status()) {
                case OK -> ok++;
                case MISSING -> missing++;
                case BLANK -> blank++;
                case UNUSED -> unused++;
                case UNDEFINED -> undefined++;
                case PLACEHOLDER_MISMATCH -> placeholderMismatch++;
                default -> {}
            }
        }
        return new Statistics(ok, missing, blank, unused, undefined, placeholderMismatch);
    }

    public Map<String, List<String>> getMissingKeysByLocale() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (ValidationResult r : results) {
            if (r.status() == ValidationResult.Status.MISSING && r.details().startsWith("Missing in: ")) {
                for (String lang : r.details().substring(12).split(", ")) {
                    map.computeIfAbsent(lang, k -> new ArrayList<>()).add(r.key());
                }
            }
        }
        return map;
    }
}
