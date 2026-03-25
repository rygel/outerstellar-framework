package io.github.rygel.outerstellar.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class LanguageTest {

    @Test
    fun `availableLanguages returns all supported languages`() {
        val languages = Language.availableLanguages()
        assertEquals(3, languages.size)
    }

    @Test
    fun `availableLanguages contains English German French`() {
        val locales = Language.availableLanguages().map { it.locale }
        assertTrue(locales.contains(Locale.ENGLISH))
        assertTrue(locales.contains(Locale.GERMAN))
        assertTrue(locales.contains(Locale.FRENCH))
    }

    @Test
    fun `isSupported returns true for supported locales`() {
        assertTrue(Language.isSupported(Locale.ENGLISH))
        assertTrue(Language.isSupported(Locale.GERMAN))
        assertTrue(Language.isSupported(Locale.FRENCH))
    }

    @Test
    fun `isSupported returns false for unsupported locales`() {
        assertFalse(Language.isSupported(Locale.JAPANESE))
        assertFalse(Language.isSupported(Locale.CHINESE))
    }

    @Test
    fun `forLocale returns language for supported locale`() {
        val english = Language.forLocale(Locale.ENGLISH)
        assertNotNull(english)
        assertEquals(Locale.ENGLISH, english!!.locale)
        assertEquals("English", english.displayName)
        assertEquals("English", english.nativeName)
    }

    @Test
    fun `forLocale returns null for unsupported locale`() {
        assertNull(Language.forLocale(Locale.JAPANESE))
    }

    @Test
    fun `forLocale matches by language ignoring country`() {
        val usEnglish = Language.forLocale(Locale.US)
        assertNotNull(usEnglish)
        assertEquals(Locale.ENGLISH, usEnglish!!.locale)
    }

    @Test
    fun `toString returns displayName`() {
        val german = Language.forLocale(Locale.GERMAN)
        assertNotNull(german)
        assertEquals("Deutsch", german.toString())
    }

    @Test
    fun `language data class properties`() {
        val french = Language.forLocale(Locale.FRENCH)!!
        assertEquals("Français", french.displayName)
        assertEquals(Locale.FRENCH, french.locale)
        assertEquals("Français", french.nativeName)
    }
}
