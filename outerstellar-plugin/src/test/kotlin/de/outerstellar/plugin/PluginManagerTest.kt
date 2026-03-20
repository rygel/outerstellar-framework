package de.outerstellar.plugin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PluginManagerTest {

    @Test
    fun `create manager`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertNotNull(manager)
    }

    @Test
    fun `factory methods`() {
        assertNotNull(PluginManager.forServer())
        assertNotNull(PluginManager.forDesktop())
        assertNotNull(PluginManager.forShared())
    }

    @Test
    fun `discover with no plugins returns empty`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertTrue(manager.discover().isEmpty())
    }

    @Test
    fun `get plugin not found returns null`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertNull(manager.getPlugin("nonexistent"))
    }

    @Test
    fun `withPlugin not found returns null`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertNull(manager.withPlugin("nonexistent") { "found" })
    }

    @Test
    fun `withEachPlugin on empty returns empty`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertTrue(manager.withEachPlugin { it.name }.isEmpty())
    }

    @Test
    fun `isInitialized starts false`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertFalse(manager.isInitialized())
    }

    @Test
    fun `shutdownAll on empty is safe`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        manager.shutdownAll()
        assertTrue(manager.getAllPlugins().isEmpty())
    }

    @Test
    fun `discoverAndInitialize sets initialized`() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        manager.discoverAndInitialize()
        assertTrue(manager.isInitialized())
    }
}

class DummyPlugin : Plugin {
    override val name = "dummy"
    override val version = "1.0.0"
    override val description = "Dummy plugin for testing"

    var initialized = false
    var shutdown = false

    override fun initialize() { initialized = true }
    override fun shutdown() { shutdown = true }
}
