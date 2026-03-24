package io.github.rygel.outerstellar.theme

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object SmartShader {
    private const val LIGHTEN_FACTOR = 0.2
    private const val DARKEN_FACTOR = 0.2
    private const val MAX_RGB = 255
    private const val SHORT_HEX_LENGTH = 3
    private const val FULL_HEX_LENGTH = 6
    private const val HEX_RADIX = 16
    private const val SHORT_HEX_MULTIPLIER = 17
    private const val HEX_BLUE_OFFSET = 4

    @JvmStatic
    @JvmName("lighten")
    fun lighten(hexColor: String): String {
        val rgb = hexToRgb(hexColor)
        val r = min(MAX_RGB, (rgb.first + MAX_RGB * LIGHTEN_FACTOR).toInt())
        val g = min(MAX_RGB, (rgb.second + MAX_RGB * LIGHTEN_FACTOR).toInt())
        val b = min(MAX_RGB, (rgb.third + MAX_RGB * LIGHTEN_FACTOR).toInt())
        return rgbToHex(r, g, b)
    }

    @JvmStatic
    @JvmName("darken")
    fun darken(hexColor: String): String {
        val rgb = hexToRgb(hexColor)
        val r = max(0, (rgb.first - MAX_RGB * DARKEN_FACTOR).toInt())
        val g = max(0, (rgb.second - MAX_RGB * DARKEN_FACTOR).toInt())
        val b = max(0, (rgb.third - MAX_RGB * DARKEN_FACTOR).toInt())
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
            SHORT_HEX_LENGTH -> {
                val r = cleanHex[0].digitToInt(HEX_RADIX) * SHORT_HEX_MULTIPLIER
                val g = cleanHex[1].digitToInt(HEX_RADIX) * SHORT_HEX_MULTIPLIER
                val b = cleanHex[2].digitToInt(HEX_RADIX) * SHORT_HEX_MULTIPLIER
                Triple(r, g, b)
            }
            FULL_HEX_LENGTH -> {
                val r = cleanHex.substring(0, 2).toInt(HEX_RADIX)
                val g = cleanHex.substring(2, HEX_BLUE_OFFSET).toInt(HEX_RADIX)
                val b = cleanHex.substring(HEX_BLUE_OFFSET, FULL_HEX_LENGTH).toInt(HEX_RADIX)
                Triple(r, g, b)
            }
            else -> throw IllegalArgumentException("Invalid hex color: $hex")
        }
    }

    @JvmStatic
    @JvmName("rgbToHex")
    fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b)
    }

    @JvmStatic
    @JvmName("adjustBrightness")
    fun adjustBrightness(hexColor: String, factor: Double): String {
        val rgb = hexToRgb(hexColor)
        val r = max(0, min(MAX_RGB, (rgb.first * factor).toInt()))
        val g = max(0, min(MAX_RGB, (rgb.second * factor).toInt()))
        val b = max(0, min(MAX_RGB, (rgb.third * factor).toInt()))
        return rgbToHex(r, g, b)
    }
}
