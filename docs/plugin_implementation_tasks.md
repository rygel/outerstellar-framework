# Outerstellar Framework - Plugin Implementation Tasks

Since you are writing the plugin system yourself, here is your step-by-step developer specification for building the SPI architecture and the Exposed bridge!

## Module 1: `outerstellar-plugin`
**Location:** `C:\Develop\Claude\outerstellar\outerstellar-plugin`
**Purpose:** Provide the raw `java.util.ServiceLoader` wrappers. Zero framework dependencies.

### Step 1: The Core Interface (`Plugin.kt`)
Create the foundational interface that all future plugin developers will implement.
- **Constraints:** Must use `@JvmOverloads` or `@JvmStatic` where appropriate.
```kotlin
package com.outerstellar.plugin

interface Plugin {
    val id: String
    val name: String
    val version: String
    
    fun onInitialize() {}
    fun onShutdown() {}
}
```

### Step 2: The Core Manager (`PluginManager.kt`)
Create the utility that actually discovers the `.jar` files on the classpath using Java's built-in SPI mechanism.
- **Goal:** Write an `inline reified` Kotlin function that wraps `ServiceLoader.load()`.
```kotlin
package com.outerstellar.plugin

import java.util.ServiceLoader

object PluginManager {
    /**
     * Finds and instantiates all classes on the classpath that implement the requested interface [T].
     * Example: `val webPlugins = PluginManager.load<ServerPlugin>()`
     */
    inline fun <reified T : Plugin> load(): List<T> {
        val loader = ServiceLoader.load(T::class.java)
        return loader.toList()
    }
}
```

---

## Module 2: `outerstellar-exposed-plugin`
**Location:** `C:\Develop\Claude\outerstellar\outerstellar-exposed-plugin`
**Purpose:** Bridge the generic `Plugin` interface with **JetBrains Exposed** so plugins can ask the host app for SQL storage safely.

### Step 1: Add Dependencies to `pom.xml`
Make sure `outerstellar-exposed-plugin/pom.xml` depends on `outerstellar-plugin` and `exposed-core`.

### Step 2: Define the Data Interface (`ExposedPlugin.kt`)
Create an extension of the base `Plugin` interface that allows plugins to return their database table definitions.
```kotlin
package com.outerstellar.plugin.exposed

import com.outerstellar.plugin.Plugin
import org.jetbrains.exposed.sql.Table

/**
 * Any plugin that implements this interface proves it requires database storage.
 * It provides JetBrains Exposed [Table] objects to the Host Application.
 */
interface ExposedPlugin : Plugin {
    fun provideDataTables(): List<Table>
}
```

### Step 3: Write the Auto-Generator (`PluginSchemaGenerator.kt`)
Write a utility for the **Host Application** (like WorkProof) to call. When the host app boots up, it calls this utility to safely and automatically generate the Postgres/H2 tables for all the active plugins.
```kotlin
package com.outerstellar.plugin.exposed

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object PluginSchemaGenerator {
    
    /**
     * Loops through every registered ExposedPlugin and safely generates
     * its required SQL tables within the Host Application's active database transaction.
     */
    @JvmStatic
    fun generateAllSchemas(plugins: List<ExposedPlugin>) {
        transaction {
            val allTables = plugins.flatMap { it.provideDataTables() }.toTypedArray()
            
            if (allTables.isNotEmpty()) {
                // JetBrains Exposed safely executes the 'CREATE TABLE IF NOT EXISTS' SQL
                SchemaUtils.createMissingTablesAndColumns(*allTables)
            }
        }
    }
}
```

## How to Test it!
Once you finish writing these 4 files:
1. Run `mvn clean install` inside `C:\Develop\Claude\outerstellar`.
2. Make sure it successfully compiles and publishes the `.jar` files to your local Maven repository.
3. Then ping me back, and I will help you integrate it into the main WorkProof web server!
