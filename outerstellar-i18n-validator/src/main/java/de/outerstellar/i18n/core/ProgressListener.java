package de.outerstellar.i18n.core;

/**
 * Callback for progress updates during validation.
 */
@FunctionalInterface
public interface ProgressListener {
    void onProgress(String message);
}
