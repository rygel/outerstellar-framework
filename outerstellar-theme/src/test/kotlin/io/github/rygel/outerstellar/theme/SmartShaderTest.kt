package io.github.rygel.outerstellar.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun `brightness overflow clamps to 255`() {
        val result = SmartShader.adjustBrightness("#ffffff", 2.0)
        assertEquals("#ffffff", result)
    }

    @Test
    fun `brightness underflow clamps to 0`() {
        val result = SmartShader.adjustBrightness("#808080", 0.0)
        assertEquals("#000000", result)
    }

    @Test
    fun `lighten white stays white`() {
        val result = SmartShader.lighten("#ffffff")
        assertEquals("#ffffff", result)
    }

    @Test
    fun `darken black stays black`() {
        val result = SmartShader.darken("#000000")
        assertEquals("#000000", result)
    }

    @Test
    fun `hexToRgb handles 0x prefix`() {
        val (r, g, b) = SmartShader.hexToRgb("0xff0000")
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
    }

    @Test
    fun `hexToRgb is case insensitive`() {
        val lower = SmartShader.hexToRgb("#ff00ff")
        val upper = SmartShader.hexToRgb("#FF00FF")
        assertEquals(lower, upper)
    }

    @Test
    fun `invalid hex length throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            SmartShader.hexToRgb("#ff00")
        }
    }

    @Test
    fun `rgbToHex round-trips correctly`() {
        val hex = "#ab12cd"
        val (r, g, b) = SmartShader.hexToRgb(hex)
        assertEquals(hex, SmartShader.rgbToHex(r, g, b))
    }
}
