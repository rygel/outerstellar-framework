package io.github.rygel.outerstellar.i18n

import java.io.InputStream
import java.net.URL
import java.util.Locale
import java.util.MissingResourceException
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

class I18nService private constructor(
    private val baseName: String,
    private val classLoader: ClassLoader,
) {
    private val bundleCache = ConcurrentHashMap<String, ResourceBundle>()
    @Volatile private var currentLocale: Locale = Locale.getDefault()

    companion object {
        @JvmStatic
        @JvmName("create")
        fun create(baseName: String): I18nService {
            return I18nService(baseName, Thread.currentThread().contextClassLoader)
        }

        @JvmStatic
        @JvmName("createWithClassLoader")
        fun create(baseName: String, classLoader: ClassLoader): I18nService {
            return I18nService(baseName, classLoader)
        }

        @JvmStatic
        @JvmName("createWithLocale")
        @JvmOverloads
        fun create(baseName: String, locale: Locale, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): I18nService {
            val service = I18nService(baseName, classLoader)
            service.setLocale(locale)
            return service
        }
    }

    @JvmOverloads
    fun setLocale(locale: Locale) {
        currentLocale = locale
        bundleCache.clear()
    }

    fun getLocale(): Locale = currentLocale

    private fun getBundle(): ResourceBundle {
        val key = "${currentLocale.toString()}_$baseName"
        return bundleCache.getOrPut(key) {
            try {
                ResourceBundle.getBundle(baseName, currentLocale, classLoader)
            } catch (e: MissingResourceException) {
                ResourceBundle.getBundle(baseName, Locale.getDefault(), classLoader)
            }
        }
    }

    @JvmOverloads
    fun translate(key: String, vararg params: Any): String {
        return try {
            val template = getBundle().getString(key)
            if (params.isEmpty()) {
                template
            } else {
                ParameterInjector.inject(template, *params)
            }
        } catch (e: MissingResourceException) {
            key
        }
    }

    @JvmOverloads
    fun translateOrDefault(key: String, default: String, vararg params: Any): String {
        return try {
            val template = getBundle().getString(key)
            if (params.isEmpty()) {
                template
            } else {
                ParameterInjector.inject(template, *params)
            }
        } catch (e: MissingResourceException) {
            default
        }
    }

    fun hasKey(key: String): Boolean {
        return try {
            getBundle().containsKey(key)
        } catch (e: Exception) {
            false
        }
    }

    fun getKeys(): Set<String> {
        return getBundle().keySet().toSet()
    }

    fun reload() {
        bundleCache.clear()
    }

    fun loadFromStream(inputStream: InputStream, key: String = "dynamic") {
        val bundle = PropertyResourceBundle(inputStream)
        val localeKey = "${currentLocale.toString()}_${baseName}_$key"
        bundleCache[localeKey] = bundle
    }

    fun loadFromUrl(url: URL, key: String = "dynamic") {
        url.openStream().use { inputStream ->
            loadFromStream(inputStream, key)
        }
    }

    fun loadFromClasspath(path: String, key: String = "dynamic") {
        val inputStream = classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        inputStream.use { stream ->
            loadFromStream(stream, key)
        }
    }
}
