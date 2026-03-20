package io.github.rygel.outerstellar.plugin

interface Plugin {
    val name: String
    val version: String
    val description: String

    fun initialize()
    fun shutdown()
}

interface ServerPlugin : Plugin
interface DesktopPlugin : Plugin
interface SharedPlugin : Plugin
