package io.github.rygel.outerstellar.i18n

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Locale

class I18nServiceTest {

    @Test
    fun testCreateService() {
        val service = I18nService.create("messages")
        assertNotNull(service)
    }

    @Test
    fun testSetLocale() {
        val service = I18nService.create("messages")
        service.setLocale(Locale.FRENCH)
        assertEquals(Locale.FRENCH, service.getLocale())
    }

    @Test
    fun testTranslateReturnsKeyWhenMissing() {
        val service = I18nService.create("nonexistent")
        val result = service.translate("missing.key")
        assertEquals("missing.key", result)
    }

    @Test
    fun testTranslateOrDefault() {
        val service = I18nService.create("messages")
        val result = service.translateOrDefault("missing", "default value")
        assertEquals("default value", result)
    }

    @Test
    fun testGetLocaleDefault() {
        val service = I18nService.create("messages")
        assertEquals(Locale.getDefault(), service.getLocale())
    }

    @Test
    fun testHasKey() {
        val service = I18nService.create("nonexistent")
        assertFalse(service.hasKey("any.key"))
    }
}
