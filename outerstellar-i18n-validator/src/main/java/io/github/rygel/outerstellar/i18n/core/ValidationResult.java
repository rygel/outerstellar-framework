package io.github.rygel.outerstellar.i18n.core;

/**
 * A single validation finding.
 */
public record ValidationResult(Type type, String key, Status status, String details) {

    public enum Type { TRANSLATION, USAGE, PLACEHOLDER, ERROR }

    public enum Status { OK, MISSING, BLANK, UNUSED, UNDEFINED, PLACEHOLDER_MISMATCH, ERROR }

    public static ValidationResult error(String message) {
        return new ValidationResult(Type.ERROR, "N/A", Status.ERROR, message);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s — %s", type, status, key, details);
    }
}
