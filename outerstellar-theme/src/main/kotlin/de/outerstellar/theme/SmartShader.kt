package de.outerstellar.theme

import kotlin.math.max
import kotlin.math.min

object SmartShader {
    private const val LIGHTEN_FACTOR = 0.2
    private const val DARKEN_FACTOR = 0.2

    @JvmStatic
    @JvmName("lighten")
    fun lighten(hexColor: String): String {
        val rgb = hexToRgb(hexColor)
        val r = min(255, (rgb.first + 255 * LIGHTEN_FACTOR).toInt())
        val g = min(255, (rgb.second + 255 * LIGHTEN_FACTOR).toInt())
        val b = min(255, (rgb.third + 255 * LIGHTEN_FACTOR).toInt())
        return rgbToHex(r, g, b)
    }

    @JvmStatic
    @JvmName("darken")
    fun darken(hexColor: String): String {
        val rgb = hexToRgb(hexColor)
        val r = max(0, (rgb.first - 255 * DARKEN_FACTOR).toInt())
        val g = max(0, (rgb.second - 255 * DARKEN_FACTOR).toInt())
        val b = max(0, (rgb.third - 255 * DARKEN_FACTOR).toInt())
        return rgbToHex(r, g, b)
    }

    @JvmStatic
    @JvmName("hover")
    fun hover(hexColor: String): String = lighten(hexColor)

    @JvmStatic
    @JvmName("pressed")
    fun pressed(hexColor: String): String = darken(hexColor)

    @JvmStatic
    @JvmName("hexToRgb")
    fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        val cleanHex = hex.removePrefix("#").removePrefix("0x")
        return when (cleanHex.length) {
            3 -> {
                val r = cleanHex[0].digitToInt(16) * 17
                val g = cleanHex[1].digitToInt(16) * 17
                val b = cleanHex[2].digitToInt(16) * 17
                Triple(r, g, b)
            }
            6 -> {
                val r = cleanHex.substring(0, 2).toInt(16)
                val g = cleanHex.substring(2, 4).toInt(16)
                val b = cleanHex.substring(4, 6).toInt(16)
                Triple(r, g, b)
            }
            else -> throw IllegalArgumentException("Invalid hex color: $hex")
        }
    }

    @JvmStatic
    @JvmName("rgbToHex")
    fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02x%02x%02x", r, g, b)
    }

    @JvmStatic
    @JvmName("adjustBrightness")
    fun adjustBrightness(hexColor: String, factor: Double): String {
        val rgb = hexToRgb(hexColor)
        val r = max(0, min(255, (rgb.first * factor).toInt()))
        val g = max(0, min(255, (rgb.second * factor).toInt()))
        val b = max(0, min(255, (rgb.third * factor).toInt()))
        return rgbToHex(r, g, b)
    }
}
