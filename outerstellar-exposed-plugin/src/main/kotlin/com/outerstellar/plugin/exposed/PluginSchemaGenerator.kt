package com.outerstellar.plugin.exposed

import com.outerstellar.plugin.Plugin
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.IColumnType

interface SchemaProvider {
    fun getTables(): List<Table>
    fun getDataClasses(): List<String>
}

class ExposedSchemaProvider(private val tables: List<Table> = emptyList()) : SchemaProvider {
    override fun getTables(): List<Table> = tables

    override fun getDataClasses(): List<String> = emptyList()
}

class PluginSchemaGenerator {
    private val schemaProviders = mutableMapOf<String, SchemaProvider>()

    companion object {
        @JvmStatic
        @JvmName("create")
        fun create(): PluginSchemaGenerator {
            return PluginSchemaGenerator()
        }
    }

    fun registerProvider(pluginName: String, provider: SchemaProvider) {
        schemaProviders[pluginName] = provider
    }

    fun registerPlugin(plugin: Plugin, provider: SchemaProvider) {
        schemaProviders[plugin.name] = provider
    }

    fun getAllTables(): List<Table> {
        return schemaProviders.values.flatMap { it.getTables() }
    }

    fun getTableNames(): List<String> {
        return getAllTables().map { it.tableName }
    }

    fun getTable(tableName: String): Table? {
        return getAllTables().find { it.tableName == tableName }
    }

    fun generateCreateStatements(): List<String> {
        return getAllTables().map { table ->
            buildString {
                append("CREATE TABLE IF NOT EXISTS ${table.tableName} (")
                val columns = table.columns.joinToString(", ") { column ->
                    val typeName = getColumnTypeName(column.columnType)
                    val nullability = if (column.columnType.nullable) "" else " NOT NULL"
                    "${column.name} $typeName$nullability"
                }
                append(columns)
                append(");")
            }
        }
    }

    private fun getColumnTypeName(columnType: IColumnType): String {
        return when (columnType) {
            is org.jetbrains.exposed.sql.IntegerColumnType -> "INTEGER"
            is org.jetbrains.exposed.sql.VarCharColumnType -> "VARCHAR(255)"
            is org.jetbrains.exposed.sql.TextColumnType -> "TEXT"
            is org.jetbrains.exposed.sql.BooleanColumnType -> "BOOLEAN"
            is org.jetbrains.exposed.sql.DecimalColumnType -> "DECIMAL"
            is org.jetbrains.exposed.sql.DoubleColumnType -> "DOUBLE"
            is org.jetbrains.exposed.sql.LongColumnType -> "BIGINT"
            else -> "TEXT"
        }
    }

    fun getSchemaMap(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        schemaProviders.forEach { (pluginName, provider) ->
            result[pluginName] = provider.getTables().map { it.tableName }
        }
        return result
    }

    fun hasSchema(pluginName: String): Boolean {
        return schemaProviders.containsKey(pluginName)
    }

    fun removeSchema(pluginName: String) {
        schemaProviders.remove(pluginName)
    }

    fun clear() {
        schemaProviders.clear()
    }
}
