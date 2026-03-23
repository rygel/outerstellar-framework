package io.github.rygel.outerstellar.plugin

import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/** Result of initializing a single plugin. */
data class PluginLoadResult<T : Plugin>(
    val plugin: T,
    val success: Boolean,
    val error: Exception? = null,
)

class PluginManager<T : Plugin> private constructor(
    private val pluginClass: Class<T>,
    private val classLoader: ClassLoader,
) {
    private val cache = ConcurrentHashMap<String, T>()
    @Volatile private var initialized = false

    private val logger = LoggerFactory.getLogger(PluginManager::class.java)

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

    /**
     * Discovers and initializes all plugins. Returns a list of [PluginLoadResult] with success/failure details.
     *
     * @param strict if true, throws on the first plugin that fails to initialize.
     */
    @JvmOverloads
    fun discoverAndInitialize(strict: Boolean = false): List<PluginLoadResult<T>> {
        val plugins = discover()
        val results = mutableListOf<PluginLoadResult<T>>()
        plugins.forEach { plugin ->
            try {
                plugin.initialize()
                val previous = cache.put(plugin.name, plugin)
                if (previous != null) {
                    logger.warn("Plugin name collision: '${plugin.name}' replaced a previously registered plugin")
                }
                logger.debug("Initialized plugin: ${plugin.name} v${plugin.version}")
                results.add(PluginLoadResult(plugin, success = true))
            } catch (e: Exception) {
                logger.error("Failed to initialize plugin ${plugin.name}", e)
                if (strict) {
                    throw PluginInitializationException(plugin.name, e)
                }
                results.add(PluginLoadResult(plugin, success = false, error = e))
            }
        }
        initialized = true
        return results
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
                logger.debug("Shut down plugin: ${plugin.name}")
            } catch (e: Exception) {
                logger.warn("Error shutting down plugin ${plugin.name}", e)
            }
        }
        cache.clear()
    }

    fun shutdownPlugin(name: String) {
        cache.remove(name)?.let { plugin ->
            try {
                plugin.shutdown()
                logger.debug("Shut down plugin: ${plugin.name}")
            } catch (e: Exception) {
                logger.warn("Error shutting down plugin $name", e)
            }
        }
    }

    fun isInitialized(): Boolean = initialized

    fun <R> withPlugin(name: String, block: (T) -> R): R? {
        check(initialized) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
        return cache[name]?.let(block)
    }

    fun <R> withEachPlugin(block: (T) -> R): List<R> {
        check(initialized) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
        return cache.values.map(block)
    }
}

/** Thrown in strict mode when a plugin fails to initialize. */
class PluginInitializationException(pluginName: String, cause: Exception) :
    RuntimeException("Plugin '$pluginName' failed to initialize", cause)
