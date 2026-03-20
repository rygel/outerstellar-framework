package de.outerstellar.plugin

import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

class PluginManager<T : Plugin> private constructor(
    private val pluginClass: Class<T>,
    private val classLoader: ClassLoader,
) {
    private val cache = ConcurrentHashMap<String, T>()
    @Volatile private var initialized = false

    private val logger = Logger.getLogger(PluginManager::class.java.name)

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
        fun forServer(): PluginManager<ServerPlugin> = create(ServerPlugin::class.java)

        @JvmStatic
        @JvmName("forDesktop")
        fun forDesktop(): PluginManager<DesktopPlugin> = create(DesktopPlugin::class.java)

        @JvmStatic
        @JvmName("forShared")
        fun forShared(): PluginManager<SharedPlugin> = create(SharedPlugin::class.java)
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
                val previous = cache.put(plugin.name, plugin)
                if (previous != null) {
                    logger.warning("Plugin name collision: '${plugin.name}' replaced a previously registered plugin")
                }
                logger.fine("Initialized plugin: ${plugin.name} v${plugin.version}")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to initialize plugin ${plugin.name}", e)
            }
        }
        initialized = true
        return plugins
    }

    fun getPlugin(name: String): T? = cache[name]

    fun getAllPlugins(): Collection<T> = cache.values

    fun reload() {
        shutdownAll()
        if (initialized) {
            discoverAndInitialize()
        }
    }

    fun shutdownAll() {
        cache.values.forEach { plugin ->
            try {
                plugin.shutdown()
                logger.fine("Shut down plugin: ${plugin.name}")
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error shutting down plugin ${plugin.name}", e)
            }
        }
        cache.clear()
    }

    fun shutdownPlugin(name: String) {
        cache.remove(name)?.let { plugin ->
            try {
                plugin.shutdown()
                logger.fine("Shut down plugin: ${plugin.name}")
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error shutting down plugin $name", e)
            }
        }
    }

    fun isInitialized(): Boolean = initialized

    fun <R> withPlugin(name: String, block: (T) -> R): R? = cache[name]?.let(block)

    fun <R> withEachPlugin(block: (T) -> R): List<R> = cache.values.map(block)
}
