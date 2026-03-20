package de.outerstellar.i18n.core;

import java.util.List;

/**
 * Validation configuration. Immutable record — construct via {@link Builder}.
 */
public record Config(
        String resourcesPath,
        String projectPath,
        String baseFileName,
        List<String> sourcePatterns,
        List<String> scanExtensions,
        List<String> ignorePatterns,
        boolean checkPlaceholders,
        boolean failOnMissing,
        boolean failOnUndefined
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourcesPath;
        private String projectPath;
        private String baseFileName = "messages.properties";
        private List<String> sourcePatterns = List.of("I18n.get", "I18n.format", "i18n.translate");
        private List<String> scanExtensions = List.of("java", "kt", "kte", "jte");
        private List<String> ignorePatterns = List.of();
        private boolean checkPlaceholders = true;
        private boolean failOnMissing = true;
        private boolean failOnUndefined = true;

        public Builder resourcesPath(String path) { this.resourcesPath = path; return this; }
        public Builder projectPath(String path) { this.projectPath = path; return this; }
        public Builder baseFileName(String name) { this.baseFileName = name; return this; }
        public Builder sourcePatterns(List<String> patterns) { this.sourcePatterns = patterns; return this; }
        public Builder scanExtensions(List<String> extensions) { this.scanExtensions = extensions; return this; }
        public Builder ignorePatterns(List<String> patterns) { this.ignorePatterns = patterns; return this; }
        public Builder checkPlaceholders(boolean check) { this.checkPlaceholders = check; return this; }
        public Builder failOnMissing(boolean fail) { this.failOnMissing = fail; return this; }
        public Builder failOnUndefined(boolean fail) { this.failOnUndefined = fail; return this; }

        public Config build() {
            if (resourcesPath == null) throw new IllegalArgumentException("resourcesPath is required");
            return new Config(resourcesPath, projectPath, baseFileName, sourcePatterns,
                    scanExtensions, ignorePatterns, checkPlaceholders, failOnMissing, failOnUndefined);
        }
    }
}
