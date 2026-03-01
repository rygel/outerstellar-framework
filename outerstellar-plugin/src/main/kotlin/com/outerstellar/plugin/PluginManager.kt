package com.outerstellar.plugin

import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

class PluginManager<T : Plugin> private constructor(
    private val pluginClass: Class<T>,
    private val classLoader: ClassLoader
) {
    private val cache = ConcurrentHashMap<String, T>()
    private var initialized = false

    companion object {
        @JvmStatic
        @JvmName("create")
        fun <T : Plugin> create(pluginClass: Class<T>): PluginManager<T> {
            return PluginManager(pluginClass, Thread.currentThread().contextClassLoader)
        }

        @JvmStatic
        @JvmName("createWithClassLoader")
        fun <T : Plugin> create(pluginClass: Class<T>, classLoader: ClassLoader): PluginManager<T> {
            return PluginManager(pluginClass, classLoader)
        }

        @JvmStatic
        @JvmName("forServer")
        fun forServer(): PluginManager<ServerPlugin> {
            return create(ServerPlugin::class.java)
        }

        @JvmStatic
        @JvmName("forDesktop")
        fun forDesktop(): PluginManager<DesktopPlugin> {
            return create(DesktopPlugin::class.java)
        }

        @JvmStatic
        @JvmName("forShared")
        fun forShared(): PluginManager<SharedPlugin> {
            return create(SharedPlugin::class.java)
        }
    }

    fun discover(): List<T> {
        val loader = ServiceLoader.load(pluginClass, classLoader)
        return loader.toList()
    }

    fun discoverAndInitialize(): List<T> {
        val plugins = discover()
        plugins.forEach { plugin ->
            try {
                plugin.initialize()
                cache[plugin.name] = plugin
            } catch (e: Exception) {
                System.err.println("Failed to initialize plugin ${plugin.name}: ${e.message}")
            }
        }
        initialized = true
        return plugins
    }

    fun getPlugin(name: String): T? {
        return cache[name]
    }

    fun getAllPlugins(): Collection<T> {
        return cache.values
    }

    fun reload() {
        shutdownAll()
        cache.clear()
        if (initialized) {
            discoverAndInitialize()
        }
    }

    fun shutdownAll() {
        cache.values.forEach { plugin ->
            try {
                plugin.shutdown()
            } catch (e: Exception) {
                System.err.println("Error shutting down plugin ${plugin.name}: ${e.message}")
            }
        }
        cache.clear()
    }

    fun shutdownPlugin(name: String) {
        cache[name]?.let { plugin ->
            try {
                plugin.shutdown()
                cache.remove(name)
            } catch (e: Exception) {
                System.err.println("Error shutting down plugin $name: ${e.message}")
            }
        }
    }

    fun isInitialized(): Boolean = initialized

    fun <R> withPlugin(name: String, block: (T) -> R): R? {
        return cache[name]?.let(block)
    }

    fun <R> withEachPlugin(block: (T) -> R): List<R> {
        return cache.values.map(block)
    }
}
