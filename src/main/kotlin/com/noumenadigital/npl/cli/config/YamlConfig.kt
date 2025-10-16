package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class YamlConfig(
    @param:JsonProperty("\$schema")
    val schemaUrl: String?,
    val runtime: Runtime = Runtime(),
    val cloud: Cloud = Cloud(),
    val local: Local = Local(),
    val structure: Structure = Structure(),
) {
    data class Runtime(
        val version: String? = null,
    )

    data class Cloud(
        val tenant: String? = null,
        val app: String? = null,
        val authUrl: String? = null,
        val clear: Boolean = false,
        val url: String? = null,
        val clientId: String? = null,
        val clientSecret: String? = null,
    )

    data class Local(
        val managementUrl: String? = null,
        val authUrl: String? = null,
        val clientId: String? = null,
        val clientSecret: String? = null,
        val username: String? = null,
        val password: String? = null,
        val clear: Boolean = false,
    )

    data class Structure(
        val coverage: Boolean = false,
        val frontend: String? = null,
        val migration: String? = null,
        val outputDir: String? = null,
        val rules: String? = null,
        val sourceDir: String? = null,
        val testSourceDir: String? = null,
    )
}

object YAMLConfigParser {
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private val configFile = File("npl.yml")
    private var rootNode: JsonNode? = null

    fun reload() {
        rootNode =
            try {
                if (configFile.exists()) mapper.readTree(configFile) else null
            } catch (_: Exception) {
                null
            }
    }

    fun parse(): YamlConfig? {
        if (!configFile.exists()) return null
        return try {
            mapper.readValue(configFile, YamlConfig::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getValue(path: String): Any? {
        var node = rootNode ?: return null
        node = node.at(path) ?: return null
        return when {
            node.isTextual -> node.asText()
            node.isNumber -> node.numberValue()
            node.isBoolean -> node.booleanValue()
            node.isArray -> node.map { it.asText() }
            node.isObject -> mapper.convertValue(node, Map::class.java)
            else -> null
        }
    }
}
