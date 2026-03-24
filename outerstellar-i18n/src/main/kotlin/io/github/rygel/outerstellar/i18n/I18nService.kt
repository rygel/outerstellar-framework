package io.github.rygel.outerstellar.i18n

import java.io.InputStream
import java.net.URL
import java.util.Locale
import java.util.MissingResourceException
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class I18nService private constructor(
    private val baseName: String,
    private val classLoader: ClassLoader,
) {
    private val bundleCache = ConcurrentHashMap<String, ResourceBundle>()
    private val dynamicBundles = ConcurrentHashMap<String, ResourceBundle>()
    private val listeners = CopyOnWriteArrayList<Translatable>()

    @Volatile
    private var currentLocale: Locale = Locale.getDefault()

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
        fun create(
            baseName: String,
            locale: Locale,
            classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        ): I18nService {
            val service = I18nService(baseName, classLoader)
            service.setLocale(locale)
            return service
        }
    }

    fun setLocale(locale: Locale) {
        currentLocale = locale
        bundleCache.clear()
        listeners.forEach { it.updateTexts() }
    }

    fun getLocale(): Locale = currentLocale

    /** Registers a [Translatable] listener that will be notified when the locale changes. */
    fun addListener(listener: Translatable) {
        if (listener !in listeners) listeners.add(listener)
    }

    /** Removes a previously registered [Translatable] listener. */
    fun removeListener(listener: Translatable) {
        listeners.remove(listener)
    }

    private fun getBundle(): ResourceBundle {
        val key = "${currentLocale}_$baseName"
        return bundleCache.getOrPut(key) {
            try {
                ResourceBundle.getBundle(baseName, currentLocale, classLoader)
            } catch (_: MissingResourceException) {
                ResourceBundle.getBundle(baseName, Locale.getDefault(), classLoader)
            }
        }
    }

    private fun findTemplate(key: String): String? {
        // Check dynamic bundles first (overlay semantics)
        for (bundle in dynamicBundles.values) {
            try {
                return bundle.getString(key)
            } catch (_: MissingResourceException) {
                // continue
            }
        }
        // Fall back to locale-based bundle
        return try {
            getBundle().getString(key)
        } catch (_: MissingResourceException) {
            null
        }
    }

    fun translate(key: String, vararg params: Any): String {
        val template = findTemplate(key) ?: return key
        return if (params.isEmpty()) template else ParameterInjector.inject(template, *params)
    }

    fun translateOrDefault(key: String, default: String, vararg params: Any): String {
        val template = findTemplate(key) ?: return default
        return if (params.isEmpty()) template else ParameterInjector.inject(template, *params)
    }

    fun hasKey(key: String): Boolean {
        return findTemplate(key) != null
    }

    fun getKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        dynamicBundles.values.forEach { keys.addAll(it.keySet()) }
        try {
            keys.addAll(getBundle().keySet())
        } catch (_: Exception) {
            // ignore
        }
        return keys
    }

    /** Clears the locale-based bundle cache, forcing a reload on next access. Dynamic bundles are preserved. */
    fun reload() {
        bundleCache.clear()
    }

    fun loadFromStream(inputStream: InputStream, key: String = "dynamic") {
        val bundle = PropertyResourceBundle(inputStream)
        dynamicBundles[key] = bundle
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
