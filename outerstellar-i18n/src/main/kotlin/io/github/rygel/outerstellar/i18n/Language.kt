package io.github.rygel.outerstellar.i18n

import java.util.Locale

/**
 * Represents a supported language with display metadata. Use [availableLanguages] to get the list of languages the
 * application supports.
 */
data class Language(
    val displayName: String,
    val locale: Locale,
    val nativeName: String,
) {
    override fun toString(): String = displayName

    companion object {
        private val languages =
            listOf(
                Language("English", Locale.ENGLISH, "English"),
                Language("Deutsch", Locale.GERMAN, "Deutsch"),
                Language("Français", Locale.FRENCH, "Français"),
            )

        /** Returns the list of supported languages. */
        @JvmStatic
        fun availableLanguages(): List<Language> = languages

        /** Returns whether the given locale is supported. */
        @JvmStatic
        fun isSupported(locale: Locale): Boolean = languages.any { it.locale.language == locale.language }

        /** Finds the [Language] for the given locale, or null if unsupported. */
        @JvmStatic
        fun forLocale(locale: Locale): Language? = languages.find { it.locale.language == locale.language }
    }
}
