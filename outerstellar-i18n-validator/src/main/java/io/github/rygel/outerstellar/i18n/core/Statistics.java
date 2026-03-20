package io.github.rygel.outerstellar.i18n.core;

/**
 * Summary statistics from a validation run.
 */
public record Statistics(
        int ok,
        int missing,
        int blank,
        int unused,
        int undefined,
        int placeholderMismatch
) {
    public boolean isValid() {
        return missing == 0 && undefined == 0 && placeholderMismatch == 0;
    }

    public int totalIssues() {
        return missing + blank + unused + undefined + placeholderMismatch;
    }

    @Override
    public String toString() {
        return String.format("OK: %d | Missing: %d | Blank: %d | Unused: %d | Undefined: %d | Placeholder: %d",
                ok, missing, blank, unused, undefined, placeholderMismatch);
    }
}
