package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.noumenadigital.npl.cli.config.YamlConfigField.Companion.yamlPath
import java.io.File

object YamlConfig {
    object Cloud {
        val tenant: YamlConfigField = yamlPath("/cloud/tenant")
        val app: YamlConfigField = yamlPath("/cloud/app")
        val authUrl: YamlConfigField = yamlPath("/cloud/authUrl")
        val clear: YamlConfigField = yamlPath("/cloud/clear")
        val url: YamlConfigField = yamlPath("/cloud/url")
        val clientId: YamlConfigField = yamlPath("/cloud/clientId")
        val clientSecret: YamlConfigField = yamlPath("/cloud/clientSecret")
    }

    object Local {
        val managementUrl: YamlConfigField = yamlPath("/local/managementUrl")
        val authUrl: YamlConfigField = yamlPath("/local/authUrl")
        val clientId: YamlConfigField = yamlPath("/local/clientId")
        val clientSecret: YamlConfigField = yamlPath("/local/clientSecret")
        val username: YamlConfigField = yamlPath("/local/username")
        val password: YamlConfigField = yamlPath("/local/password")
        val clear: YamlConfigField = yamlPath("/local/clear")
    }

    object Structure {
        val coverage: YamlConfigField = yamlPath("/structure/coverage")
        val frontend: YamlConfigField = yamlPath("/structure/frontend")
        val migration: YamlConfigField = yamlPath("/structure/migration")
        val outputDir: YamlConfigField = yamlPath("/structure/outputDir")
        val rules: YamlConfigField = yamlPath("/structure/rules")
        val sourceDir: YamlConfigField = yamlPath("/structure/sourceDir")
        val testSourceDir: YamlConfigField = yamlPath("/structure/testSourceDir")
        val initProjectDir: YamlConfigField = yamlPath("/structure/initProjectDir")
        val initBare: YamlConfigField = yamlPath("/structure/initBare")
        val initTemplateUrl: YamlConfigField = yamlPath("/structure/initTemplateUrl")
    }
}

class YamlConfigField private constructor(
    private val fieldPath: String,
) {
    fun getValue(): String = fieldPath

    override fun toString(): String = fieldPath

    companion object {
        fun yamlPath(fieldPath: String): YamlConfigField = YamlConfigField(fieldPath)
    }
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
