package de.outerstellar.theme

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

data class ColorScheme @JsonCreator constructor(
    @JsonProperty("base") val base: String,
    @JsonProperty("hover") val hover: String,
    @JsonProperty("pressed") val pressed: String
)

data class ThemeColors @JsonCreator constructor(
    @JsonProperty("colors") val colors: Map<String, ColorScheme>
)

class ThemeService private constructor(
    private val objectMapper: ObjectMapper,
    private val themeColors: ThemeColors
) {
    companion object {
        @JvmStatic
        @JvmName("create")
        fun create(): ThemeService {
            return ThemeService(ObjectMapper(), ThemeColors(emptyMap()))
        }

        @JvmStatic
        @JvmName("createFromJson")
        fun createFromJson(json: String): ThemeService {
            val mapper = ObjectMapper()
            val themeColors = mapper.readValue(json, ThemeColors::class.java)
            return ThemeService(mapper, themeColors)
        }
    }

    @JvmOverloads
    fun loadFromFile(file: File): ThemeService {
        val loaded = objectMapper.readValue(file, ThemeColors::class.java)
        return ThemeService(objectMapper, loaded)
    }

    @JvmOverloads
    fun loadFromInputStream(inputStream: InputStream): ThemeService {
        val loaded = objectMapper.readValue(inputStream, ThemeColors::class.java)
        return ThemeService(objectMapper, loaded)
    }

    @JvmOverloads
    fun loadFromUrl(url: URL): ThemeService {
        val loaded = objectMapper.readValue(url, ThemeColors::class.java)
        return ThemeService(objectMapper, loaded)
    }

    @JvmOverloads
    fun loadFromClasspath(path: String): ThemeService {
        val inputStream = this::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return loadFromInputStream(inputStream)
    }

    @JvmOverloads
    fun loadFromJson(json: String): ThemeService {
        val loaded = objectMapper.readValue(json, ThemeColors::class.java)
        return ThemeService(objectMapper, loaded)
    }

    @JvmOverloads
    fun loadFromDirectory(directory: File): ThemeService {
        if (!directory.isDirectory) {
            throw IllegalArgumentException("Not a directory: ${directory.path}")
        }
        var accumulated = themeColors.colors
        Files.list(directory.toPath())
            .filter { it.toString().endsWith(".json") }
            .forEach { path ->
                try {
                    val json = Files.readString(path)
                    val loaded = objectMapper.readValue(json, ThemeColors::class.java)
                    accumulated = accumulated + loaded.colors
                } catch (e: Exception) {
                    System.err.println("Failed to load theme from ${path}: ${e.message}")
                }
            }
        return ThemeService(objectMapper, ThemeColors(accumulated))
    }

    @JvmOverloads
    fun loadFromDirectory(path: String): ThemeService {
        return loadFromDirectory(File(path))
    }

    fun loadFromDirectory(directoryPath: Path): ThemeService {
        return loadFromDirectory(directoryPath.toFile())
    }

    fun getColors(): Map<String, ColorScheme> = themeColors.colors

    fun getBaseColor(name: String): String? = themeColors.colors[name]?.base

    fun getHoverColor(name: String): String? = themeColors.colors[name]?.hover

    fun getPressedColor(name: String): String? = themeColors.colors[name]?.pressed

    fun computeShading(baseColor: String): ColorScheme {
        return ColorScheme(
            base = baseColor,
            hover = SmartShader.hover(baseColor),
            pressed = SmartShader.pressed(baseColor)
        )
    }

    fun getOrCompute(name: String): ColorScheme {
        return themeColors.colors[name] ?: computeShading("#000000")
    }

    fun getHexMap(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        themeColors.colors.forEach { (name, scheme) ->
            result["$name-base"] = scheme.base
            result["$name-hover"] = scheme.hover
            result["$name-pressed"] = scheme.pressed
        }
        return result
    }

    fun getHexMapWithComputed(baseColors: Map<String, String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        baseColors.forEach { (name, baseColor) ->
            val scheme = computeShading(baseColor)
            result["$name-base"] = scheme.base
            result["$name-hover"] = scheme.hover
            result["$name-pressed"] = scheme.pressed
        }
        return result
    }

    fun toCssVariables(): String {
        val sb = StringBuilder()
        sb.appendLine(":root {")
        themeColors.colors.forEach { (name, scheme) ->
            sb.appendLine("  --color-$name: ${scheme.base};")
            sb.appendLine("  --color-$name-hover: ${scheme.hover};")
            sb.appendLine("  --color-$name-pressed: ${scheme.pressed};")
        }
        sb.appendLine("}")
        return sb.toString()
    }

    fun toCssForSelector(selector: String): String {
        val sb = StringBuilder()
        sb.appendLine("$selector {")
        themeColors.colors.forEach { (name, scheme) ->
            sb.appendLine("  --color-$name: ${scheme.base};")
            sb.appendLine("  --color-$name-hover: ${scheme.hover};")
            sb.appendLine("  --color-$name-pressed: ${scheme.pressed};")
        }
        sb.appendLine("}")
        return sb.toString()
    }

    fun toCssVariablesWithComputed(baseColors: Map<String, String>): String {
        val sb = StringBuilder()
        sb.appendLine(":root {")
        baseColors.forEach { (name, baseColor) ->
            val scheme = computeShading(baseColor)
            sb.appendLine("  --color-$name: ${scheme.base};")
            sb.appendLine("  --color-$name-hover: ${scheme.hover};")
            sb.appendLine("  --color-$name-pressed: ${scheme.pressed};")
        }
        sb.appendLine("}")
        return sb.toString()
    }

    fun addColor(name: String, colorScheme: ColorScheme): ThemeService {
        val newColors = themeColors.colors + (name to colorScheme)
        return ThemeService(objectMapper, ThemeColors(newColors))
    }

    fun addColor(name: String, baseColor: String): ThemeService {
        return addColor(name, computeShading(baseColor))
    }
}
