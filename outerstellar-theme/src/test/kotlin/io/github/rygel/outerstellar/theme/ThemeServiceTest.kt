package io.github.rygel.outerstellar.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ThemeServiceTest {

    private val testJson = """
    {
        "colors": {
            "primary": { "base": "#007bff", "hover": "#0056b3", "pressed": "#004085" },
            "success": { "base": "#28a745", "hover": "#1e7e34", "pressed": "#1a6b2c" }
        }
    }
    """.trimIndent()

    @Test
    fun testCreateEmptyService() {
        val service = ThemeService.create()
        assertNotNull(service)
    }

    @Test
    fun testLoadFromJson() {
        val service = ThemeService.create().loadFromJson(testJson)
        assertEquals("#007bff", service.getBaseColor("primary"))
        assertEquals("#0056b3", service.getHoverColor("primary"))
    }

    @Test
    fun testComputeShading() {
        val service = ThemeService.create()
        val scheme = service.computeShading("#007bff")

        assertEquals("#007bff", scheme.base)
        assertNotNull(scheme.hover)
        assertNotNull(scheme.pressed)
    }

    @Test
    fun testGetHexMap() {
        val service = ThemeService.create().loadFromJson(testJson)
        val hexMap = service.getHexMap()

        assertEquals("#007bff", hexMap["primary-base"])
        assertEquals("#0056b3", hexMap["primary-hover"])
        assertEquals("#004085", hexMap["primary-pressed"])
    }

    @Test
    fun testToCssVariables() {
        val service = ThemeService.create().loadFromJson(testJson)
        val css = service.toCssVariables()

        assertTrue(css.contains(":root {"))
        assertTrue(css.contains("--color-primary: #007bff;"))
        assertTrue(css.contains("--color-primary-hover: #0056b3;"))
    }

    @Test
    fun testGetColors() {
        val service = ThemeService.create().loadFromJson(testJson)
        val colors = service.getColors()

        assertEquals(2, colors.size)
        assertTrue(colors.containsKey("primary"))
        assertTrue(colors.containsKey("success"))
    }

    @Test
    fun testAddColor() {
        val service = ThemeService.create().addColor("test", "#ff0000")

        assertEquals("#ff0000", service.getBaseColor("test"))
        assertNotNull(service.getHoverColor("test"))
    }

    @Test
    fun testGetOrComputeWithExisting() {
        val service = ThemeService.create().loadFromJson(testJson)
        val scheme = service.getOrCompute("primary")

        assertEquals("#007bff", scheme.base)
    }

    @Test
    fun testGetOrComputeWithMissing() {
        val service = ThemeService.create()
        val scheme = service.getOrCompute("nonexistent")

        assertEquals("#000000", scheme.base)
    }

    @Test
    fun testGetHexMapWithComputed() {
        val service = ThemeService.create()
        val baseColors = mapOf("custom" to "#990000")
        val hexMap = service.getHexMapWithComputed(baseColors)

        assertEquals("#990000", hexMap["custom-base"])
        assertNotNull(hexMap["custom-hover"])
        assertNotNull(hexMap["custom-pressed"])
    }

    @Test
    fun testLoadFromDirectory() {
        val tempDir = createTempDirectory().toFile()
        val file = File(tempDir, "theme1.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.create().loadFromDirectory(tempDir)
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCreateFromJson() {
        val service = ThemeService.createFromJson(testJson)
        assertEquals("#007bff", service.getBaseColor("primary"))
    }
}
