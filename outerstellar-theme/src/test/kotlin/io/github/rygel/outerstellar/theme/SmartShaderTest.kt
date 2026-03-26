package io.github.rygel.outerstellar.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
        // 0x80=128, lighten adds 255*0.2=51 → 179=0xb3, g/b = 51=0x33
        assertEquals("#b33333", lightened)
    }

    @Test
    fun testDarken() {
        val darkened = SmartShader.darken("#800000")
        // 0x80=128, darken subtracts 255*0.2=51 → 77=0x4d, g/b clamp to 0
        assertEquals("#4d0000", darkened)
    }

    @Test
    fun testAdjustBrightness() {
        val brightened = SmartShader.adjustBrightness("#808080", 1.5)
        // 0x80=128, 128*1.5=192=0xc0
        assertEquals("#c0c0c0", brightened)
    }

    @Test
    fun testAdjustBrightnessBelowOne() {
        val darkened = SmartShader.adjustBrightness("#808080", 0.5)
        // 0x80=128, 128*0.5=64=0x40
        assertEquals("#404040", darkened)
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
