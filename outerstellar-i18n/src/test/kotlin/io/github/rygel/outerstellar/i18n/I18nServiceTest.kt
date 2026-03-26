package io.github.rygel.outerstellar.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class I18nServiceTest {

    @Test
    fun testCreateService() {
        val service = I18nService.create("messages")
        assertNotNull(service)
    }

    @Test
    fun testCreateWithLocale() {
        val service = I18nService.create("messages", Locale.FRENCH)
        assertEquals(Locale.FRENCH, service.getLocale())
    }

    @Test
    fun testCreateWithClassLoader() {
        val service = I18nService.create("messages", Thread.currentThread().contextClassLoader)
        assertNotNull(service)
    }

    @Test
    fun testSetLocale() {
        val service = I18nService.create("messages")
        service.setLocale(Locale.FRENCH)
        assertEquals(Locale.FRENCH, service.getLocale())
    }

    @Test
    fun testGetLocaleDefault() {
        val service = I18nService.create("messages")
        assertEquals(Locale.getDefault(), service.getLocale())
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
    fun testHasKey() {
        val service = I18nService.create("nonexistent")
        assertFalse(service.hasKey("any.key"))
    }

    @Test
    fun `getKeys returns empty set for nonexistent bundle`() {
        val service = I18nService.create("nonexistent")
        assertTrue(service.getKeys().isEmpty())
    }

    @Test
    fun `reload clears bundle cache`() {
        val service = I18nService.create("messages")
        service.translate("any.key")
        service.reload()
        // After reload, service should still work — just forces re-read on next access
        assertEquals("missing.key", service.translate("missing.key"))
    }

    @Test
    fun `addListener notifies on locale change`() {
        val service = I18nService.create("messages")
        var notified = false
        val listener = Translatable { notified = true }

        service.addListener(listener)
        service.setLocale(Locale.GERMAN)

        assertTrue(notified)
    }

    @Test
    fun `addListener does not add duplicate`() {
        val service = I18nService.create("messages")
        var count = 0
        val listener = Translatable { count++ }

        service.addListener(listener)
        service.addListener(listener)
        service.setLocale(Locale.GERMAN)

        assertEquals(1, count)
    }

    @Test
    fun `removeListener stops notifications`() {
        val service = I18nService.create("messages")
        var notified = false
        val listener = Translatable { notified = true }

        service.addListener(listener)
        service.removeListener(listener)
        service.setLocale(Locale.GERMAN)

        assertFalse(notified)
    }

    @Test
    fun `loadFromStream adds dynamic bundle`() {
        val service = I18nService.create("nonexistent")
        val props = "greeting=Hello Dynamic".toByteArray()

        service.loadFromStream(props.inputStream(), "test")

        assertEquals("Hello Dynamic", service.translate("greeting"))
        assertTrue(service.hasKey("greeting"))
    }

    @Test
    fun `loadFromStream with default key`() {
        val service = I18nService.create("nonexistent")
        val props = "key1=value1".toByteArray()

        service.loadFromStream(props.inputStream())

        assertEquals("value1", service.translate("key1"))
    }

    @Test
    fun `dynamic bundles overlay locale bundles`() {
        val service = I18nService.create("nonexistent")
        val props = "override.key=dynamic value".toByteArray()

        service.loadFromStream(props.inputStream(), "overlay")

        assertEquals("dynamic value", service.translate("override.key"))
    }

    @Test
    fun `getKeys includes dynamic bundle keys`() {
        val service = I18nService.create("nonexistent")
        val props = "dynkey=value".toByteArray()

        service.loadFromStream(props.inputStream(), "test")

        assertTrue(service.getKeys().contains("dynkey"))
    }

    @Test
    fun `loadFromClasspath throws for missing resource`() {
        val service = I18nService.create("messages")
        assertThrows(IllegalArgumentException::class.java) {
            service.loadFromClasspath("nonexistent.properties")
        }
    }

    @Test
    fun `translate with parameters`() {
        val service = I18nService.create("nonexistent")
        val props = "greeting=Hello {0}, you are {1}".toByteArray()
        service.loadFromStream(props.inputStream(), "test")

        assertEquals("Hello World, you are great", service.translate("greeting", "World", "great"))
    }

    @Test
    fun `translateOrDefault with parameters`() {
        val service = I18nService.create("nonexistent")
        val props = "msg=Count: {0}".toByteArray()
        service.loadFromStream(props.inputStream(), "test")

        assertEquals("Count: 42", service.translateOrDefault("msg", "fallback", 42))
    }

    @Test
    fun `translateOrDefault returns default when key missing`() {
        val service = I18nService.create("nonexistent")
        assertEquals("fallback", service.translateOrDefault("missing", "fallback", "unused"))
    }
}
