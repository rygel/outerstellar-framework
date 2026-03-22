package io.github.rygel.outerstellar.i18n

/**
 * Interface for UI components that need to update their texts dynamically when the locale changes. Register with
 * [I18nService.addListener] to receive notifications.
 */
fun interface Translatable {
    /** Called when the active locale changes. Implementations should re-read all translated strings. */
    fun updateTexts()
}
