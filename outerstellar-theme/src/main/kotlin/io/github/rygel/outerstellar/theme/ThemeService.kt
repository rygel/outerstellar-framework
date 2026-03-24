package io.github.rygel.outerstellar.theme

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class ColorScheme @JsonCreator constructor(
    @JsonProperty("base") val base: String,
    @JsonProperty("hover") val hover: String,
    @JsonProperty("pressed") val pressed: String
)

data class ThemeColors @JsonCreator constructor(
    @JsonProperty("colors") val colors: Map<String, ColorScheme>
)

class ThemeService private constructor(
    private val themeColors: ThemeColors
) {
    companion object {
        internal val objectMapper = ObjectMapper()

        @JvmStatic
        @JvmName("create")
        fun create(): ThemeService {
            return ThemeService(ThemeColors(emptyMap()))
        }

        @JvmStatic
        @JvmName("createFromJson")
        fun createFromJson(json: String): ThemeService {
            val themeColors = objectMapper.readValue(json, ThemeColors::class.java)
            return ThemeService(themeColors)
        }

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * Fluent builder for composing themes from multiple sources.
     *
     * ```kotlin
     * val theme = ThemeService.builder()
     *     .fromClasspath("themes/base.json")
     *     .fromClasspath("themes/overrides.json")
     *     .addColor("accent", "#ff6600")
     *     .build()
     * ```
     */
    class Builder {
        private var colors = mutableMapOf<String, ColorScheme>()

        fun fromJson(json: String): Builder {
            val loaded = objectMapper.readValue(json, ThemeColors::class.java)
            colors.putAll(loaded.colors)
            return this
        }

        fun fromFile(file: File): Builder {
            val loaded = objectMapper.readValue(file, ThemeColors::class.java)
            colors.putAll(loaded.colors)
            return this
        }

        fun fromClasspath(path: String): Builder {
            val inputStream = Builder::class.java.classLoader.getResourceAsStream(path)
                ?: throw IllegalArgumentException("Resource not found: $path")
            val loaded = objectMapper.readValue(inputStream, ThemeColors::class.java)
            colors.putAll(loaded.colors)
            return this
        }

        fun fromDirectory(directory: File): Builder {
            require(directory.isDirectory) { "Not a directory: ${directory.path}" }
            Files.list(directory.toPath())
                .filter { it.toString().endsWith(".json") }
                .forEach { path ->
                    try {
                        val json = Files.readString(path)
                        val loaded = objectMapper.readValue(json, ThemeColors::class.java)
                        colors.putAll(loaded.colors)
                    } catch (_: IOException) {
                        // skip unreadable files
                    }
                }
            return this
        }

        fun addColor(name: String, scheme: ColorScheme): Builder {
            colors[name] = scheme
            return this
        }

        fun addColor(name: String, baseColor: String): Builder {
            colors[name] = ColorScheme(
                base = baseColor,
                hover = SmartShader.hover(baseColor),
                pressed = SmartShader.pressed(baseColor)
            )
            return this
        }

        fun build(): ThemeService = ThemeService(ThemeColors(colors.toMap()))
    }

    fun loadFromFile(file: File): ThemeService {
        val loaded = objectMapper.readValue(file, ThemeColors::class.java)
        return ThemeService(loaded)
    }

    fun loadFromInputStream(inputStream: InputStream): ThemeService {
        val loaded = objectMapper.readValue(inputStream, ThemeColors::class.java)
        return ThemeService(loaded)
    }

    fun loadFromUrl(url: URL): ThemeService {
        return url.openStream().use { loadFromInputStream(it) }
    }

    fun loadFromClasspath(path: String): ThemeService {
        val inputStream = this::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return loadFromInputStream(inputStream)
    }

    fun loadFromJson(json: String): ThemeService {
        val loaded = objectMapper.readValue(json, ThemeColors::class.java)
        return ThemeService(loaded)
    }

    fun loadFromDirectory(directory: File): ThemeService {
        require(directory.isDirectory) { "Not a directory: ${directory.path}" }
        var accumulated = themeColors.colors
        Files.list(directory.toPath())
            .filter { it.toString().endsWith(".json") }
            .forEach { path ->
                try {
                    val json = Files.readString(path)
                    val loaded = objectMapper.readValue(json, ThemeColors::class.java)
                    accumulated = accumulated + loaded.colors
                } catch (e: IOException) {
                    System.err.println("Failed to load theme from $path: ${e.message}")
                }
            }
        return ThemeService(ThemeColors(accumulated))
    }

    fun getColors(): Map<String, ColorScheme> = themeColors.colors

    fun getBaseColor(name: String): String? = themeColors.colors[name]?.base

    fun getHoverColor(name: String): String? = themeColors.colors[name]?.hover

    fun getPressedColor(name: String): String? = themeColors.colors[name]?.pressed

    private val shadeCache = ConcurrentHashMap<String, ColorScheme>()

    fun computeShading(baseColor: String): ColorScheme {
        return shadeCache.computeIfAbsent(baseColor) {
            ColorScheme(
                base = it,
                hover = SmartShader.hover(it),
                pressed = SmartShader.pressed(it)
            )
        }
    }

    fun addColor(name: String, colorScheme: ColorScheme): ThemeService {
        val newColors = themeColors.colors + (name to colorScheme)
        return ThemeService(ThemeColors(newColors))
    }

    fun addColor(name: String, baseColor: String): ThemeService {
        return addColor(name, computeShading(baseColor))
    }
}

fun ThemeService.loadFromDirectory(path: String): ThemeService {
    return loadFromDirectory(File(path))
}

fun ThemeService.loadFromDirectory(directoryPath: Path): ThemeService {
    return loadFromDirectory(directoryPath.toFile())
}

fun ThemeService.getOrCompute(name: String): ColorScheme {
    return getColors()[name] ?: computeShading("#000000")
}

fun ThemeService.getHexMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    getColors().forEach { (name, scheme) ->
        result["$name-base"] = scheme.base
        result["$name-hover"] = scheme.hover
        result["$name-pressed"] = scheme.pressed
    }
    return Collections.unmodifiableMap(result)
}

fun ThemeService.getHexMapWithComputed(baseColors: Map<String, String>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    baseColors.forEach { (name, baseColor) ->
        val scheme = computeShading(baseColor)
        result["$name-base"] = scheme.base
        result["$name-hover"] = scheme.hover
        result["$name-pressed"] = scheme.pressed
    }
    return Collections.unmodifiableMap(result)
}

fun ThemeService.toCssVariables(): String {
    val sb = StringBuilder()
    sb.appendLine(":root {")
    getColors().forEach { (name, scheme) ->
        sb.appendLine("  --color-$name: ${scheme.base};")
        sb.appendLine("  --color-$name-hover: ${scheme.hover};")
        sb.appendLine("  --color-$name-pressed: ${scheme.pressed};")
    }
    sb.appendLine("}")
    return sb.toString()
}

fun ThemeService.toCssForSelector(selector: String): String {
    val sb = StringBuilder()
    sb.appendLine("$selector {")
    getColors().forEach { (name, scheme) ->
        sb.appendLine("  --color-$name: ${scheme.base};")
        sb.appendLine("  --color-$name-hover: ${scheme.hover};")
        sb.appendLine("  --color-$name-pressed: ${scheme.pressed};")
    }
    sb.appendLine("}")
    return sb.toString()
}

fun ThemeService.toCssVariablesWithComputed(baseColors: Map<String, String>): String {
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
