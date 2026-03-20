package de.outerstellar.theme

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SmartShaderTest {

    @Test
    fun testHexToRgb6Digits() {
        val (r, g, b) = SmartShader.hexToRgb("#ff0000")
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
    }

    @Test
    fun testHexToRgb3Digits() {
        val (r, g, b) = SmartShader.hexToRgb("#f00")
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
    }

    @Test
    fun testHexToRgbWithoutHash() {
        val (r, g, b) = SmartShader.hexToRgb("00ff00")
        assertEquals(0, r)
        assertEquals(255, g)
        assertEquals(0, b)
    }

    @Test
    fun testRgbToHex() {
        val hex = SmartShader.rgbToHex(255, 128, 0)
        assertEquals("#ff8000", hex)
    }

    @Test
    fun testLighten() {
        val lightened = SmartShader.lighten("#800000")
        assertTrue(lightened.startsWith("#"))
        assertEquals(7, lightened.length)
    }

    @Test
    fun testDarken() {
        val darkened = SmartShader.darken("#800000")
        assertTrue(darkened.startsWith("#"))
        assertEquals(7, darkened.length)
    }

    @Test
    fun testHoverAlias() {
        val hover = SmartShader.hover("#800000")
        val lightened = SmartShader.lighten("#800000")
        assertEquals(hover, lightened)
    }

    @Test
    fun testPressedAlias() {
        val pressed = SmartShader.pressed("#800000")
        val darkened = SmartShader.darken("#800000")
        assertEquals(pressed, darkened)
    }

    @Test
    fun testAdjustBrightness() {
        val brightened = SmartShader.adjustBrightness("#808080", 1.5)
        assertTrue(brightened.startsWith("#"))
    }

    @Test
    fun testAdjustBrightnessBelowOne() {
        val darkened = SmartShader.adjustBrightness("#808080", 0.5)
        assertTrue(darkened.startsWith("#"))
    }

    @Test
    fun testInvalidHexThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            SmartShader.hexToRgb("#gggggg")
        }
    }
}
