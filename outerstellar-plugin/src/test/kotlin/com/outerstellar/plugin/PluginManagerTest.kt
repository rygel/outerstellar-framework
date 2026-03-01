package com.outerstellar.plugin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PluginManagerTest {

    @Test
    fun testCreateManager() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertNotNull(manager)
    }

    @Test
    fun testForServer() {
        val manager = PluginManager.forServer()
        assertNotNull(manager)
    }

    @Test
    fun testForDesktop() {
        val manager = PluginManager.forDesktop()
        assertNotNull(manager)
    }

    @Test
    fun testForShared() {
        val manager = PluginManager.forShared()
        assertNotNull(manager)
    }

    @Test
    fun testDiscoverWithNoPlugins() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        val plugins = manager.discover()
        assertTrue(plugins.isEmpty())
    }

    @Test
    fun testGetPluginNotFound() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        val plugin = manager.getPlugin("nonexistent")
        assertNull(plugin)
    }

    @Test
    fun testWithPluginNotFound() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        val result = manager.withPlugin("nonexistent") { "found" }
        assertNull(result)
    }

    @Test
    fun testWithEachPluginEmpty() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        val results = manager.withEachPlugin { it.name }
        assertTrue(results.isEmpty())
    }

    @Test
    fun testIsInitialized() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        assertFalse(manager.isInitialized())
    }

    @Test
    fun testShutdownAllEmpty() {
        val manager = PluginManager.create(DummyPlugin::class.java)
        manager.shutdownAll()
        assertTrue(true)
    }
}

class DummyPlugin : Plugin {
    override val name: String = "dummy"
    override val version: String = "1.0.0"
    override val description: String = "Dummy plugin for testing"
    
    var initialized = false
    var shutdown = false
    
    override fun initialize() {
        initialized = true
    }
    
    override fun shutdown() {
        shutdown = true
    }
}
