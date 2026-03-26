package io.github.rygel.outerstellar.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
        assertTrue(service.getColors().isEmpty())
    }

    @Test
    fun testCreateFromJson() {
        val service = ThemeService.createFromJson(testJson)
        assertEquals("#007bff", service.getBaseColor("primary"))
        assertEquals("#0056b3", service.getHoverColor("primary"))
        assertEquals("#004085", service.getPressedColor("primary"))
        assertEquals(2, service.getColors().size)
    }

    @Test
    fun testLoadFromJson() {
        val service = ThemeService.create().loadFromJson(testJson)
        assertEquals("#007bff", service.getBaseColor("primary"))
    }

    @Test
    fun testComputeShading() {
        val service = ThemeService.create()
        val scheme = service.computeShading("#007bff")

        assertEquals("#007bff", scheme.base)
        assertEquals(SmartShader.hover("#007bff"), scheme.hover)
        assertEquals(SmartShader.pressed("#007bff"), scheme.pressed)
    }

    @Test
    fun testGetHexMap() {
        val service = ThemeService.create().loadFromJson(testJson)
        val hexMap = service.getHexMap()

        assertEquals("#007bff", hexMap["primary-base"])
        assertEquals("#0056b3", hexMap["primary-hover"])
        assertEquals("#004085", hexMap["primary-pressed"])
        assertEquals(6, hexMap.size)
    }

    @Test
    fun testToCssVariables() {
        val service = ThemeService.create().loadFromJson(testJson)
        val css = service.toCssVariables()

        assertTrue(css.contains(":root {"))
        assertTrue(css.contains("--color-primary: #007bff;"))
        assertTrue(css.contains("--color-primary-hover: #0056b3;"))
        assertTrue(css.contains("--color-primary-pressed: #004085;"))
    }

    @Test
    fun testToCssForSelector() {
        val service = ThemeService.create().loadFromJson(testJson)
        val css = service.toCssForSelector(".dark-theme")

        assertTrue(css.contains(".dark-theme {"))
        assertTrue(css.contains("--color-primary: #007bff;"))
        assertTrue(css.contains("}"))
    }

    @Test
    fun testToCssVariablesWithComputed() {
        val service = ThemeService.create()
        val css = service.toCssVariablesWithComputed(mapOf("accent" to "#ff6600"))

        assertTrue(css.contains(":root {"))
        assertTrue(css.contains("--color-accent: #ff6600;"))
        assertTrue(css.contains("--color-accent-hover:"))
        assertTrue(css.contains("--color-accent-pressed:"))
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
    fun testGetBaseColor() {
        val service = ThemeService.create().loadFromJson(testJson)
        assertEquals("#007bff", service.getBaseColor("primary"))
        assertNull(service.getBaseColor("nonexistent"))
    }

    @Test
    fun testGetHoverColor() {
        val service = ThemeService.create().loadFromJson(testJson)
        assertEquals("#0056b3", service.getHoverColor("primary"))
        assertNull(service.getHoverColor("nonexistent"))
    }

    @Test
    fun testGetPressedColor() {
        val service = ThemeService.create().loadFromJson(testJson)
        assertEquals("#004085", service.getPressedColor("primary"))
        assertNull(service.getPressedColor("nonexistent"))
    }

    @Test
    fun testAddColorWithBaseString() {
        val service = ThemeService.create().addColor("test", "#ff0000")

        assertEquals("#ff0000", service.getBaseColor("test"))
        assertEquals(SmartShader.hover("#ff0000"), service.getHoverColor("test"))
        assertEquals(SmartShader.pressed("#ff0000"), service.getPressedColor("test"))
    }

    @Test
    fun testAddColorWithColorScheme() {
        val scheme = ColorScheme(base = "#aaa", hover = "#bbb", pressed = "#ccc")
        val service = ThemeService.create().addColor("custom", scheme)

        assertEquals("#aaa", service.getBaseColor("custom"))
        assertEquals("#bbb", service.getHoverColor("custom"))
        assertEquals("#ccc", service.getPressedColor("custom"))
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
        assertEquals(SmartShader.hover("#990000"), hexMap["custom-hover"])
        assertEquals(SmartShader.pressed("#990000"), hexMap["custom-pressed"])
    }

    @Test
    fun testLoadFromFile() {
        val tempDir = createTempDirectory().toFile()
        val file = File(tempDir, "theme.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.create().loadFromFile(file)
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLoadFromInputStream() {
        val inputStream = testJson.byteInputStream()
        val service = ThemeService.create().loadFromInputStream(inputStream)
        assertEquals("#007bff", service.getBaseColor("primary"))
    }

    @Test
    fun testLoadFromClasspath() {
        val service = ThemeService.create().loadFromClasspath("themes/default.json")
        assertNotNull(service.getColors())
        assertTrue(service.getColors().isNotEmpty())
    }

    @Test
    fun testLoadFromClasspathNotFound() {
        assertThrows(IllegalArgumentException::class.java) {
            ThemeService.create().loadFromClasspath("nonexistent.json")
        }
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
    fun testLoadFromDirectoryStringExtension() {
        val tempDir = createTempDirectory().toFile()
        val file = File(tempDir, "theme1.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.create().loadFromDirectory(tempDir.absolutePath)
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLoadFromDirectoryPathExtension() {
        val tempDir = createTempDirectory()
        val file = File(tempDir.toFile(), "theme1.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.create().loadFromDirectory(tempDir)
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testLoadFromDirectoryNotADirectory() {
        val tempFile = File.createTempFile("not-a-dir", ".json")
        try {
            assertThrows(IllegalArgumentException::class.java) {
                ThemeService.create().loadFromDirectory(tempFile)
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testBuilderFromJson() {
        val service = ThemeService.builder()
            .fromJson(testJson)
            .build()
        assertEquals("#007bff", service.getBaseColor("primary"))
    }

    @Test
    fun testBuilderFromFile() {
        val tempDir = createTempDirectory().toFile()
        val file = File(tempDir, "theme.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.builder()
                .fromFile(file)
                .build()
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testBuilderFromClasspath() {
        val service = ThemeService.builder()
            .fromClasspath("themes/default.json")
            .build()
        assertTrue(service.getColors().isNotEmpty())
    }

    @Test
    fun testBuilderFromDirectory() {
        val tempDir = createTempDirectory().toFile()
        val file = File(tempDir, "theme.json")
        file.writeText(testJson)

        try {
            val service = ThemeService.builder()
                .fromDirectory(tempDir)
                .build()
            assertEquals("#007bff", service.getBaseColor("primary"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testBuilderAddColorWithBase() {
        val service = ThemeService.builder()
            .addColor("accent", "#ff6600")
            .build()
        assertEquals("#ff6600", service.getBaseColor("accent"))
    }

    @Test
    fun testBuilderAddColorWithScheme() {
        val scheme = ColorScheme(base = "#aaa", hover = "#bbb", pressed = "#ccc")
        val service = ThemeService.builder()
            .addColor("custom", scheme)
            .build()
        assertEquals("#aaa", service.getBaseColor("custom"))
    }

    @Test
    fun testBuilderChaining() {
        val service = ThemeService.builder()
            .fromJson(testJson)
            .addColor("accent", "#ff6600")
            .build()
        assertEquals("#007bff", service.getBaseColor("primary"))
        assertEquals("#ff6600", service.getBaseColor("accent"))
        assertEquals(3, service.getColors().size)
    }
}
