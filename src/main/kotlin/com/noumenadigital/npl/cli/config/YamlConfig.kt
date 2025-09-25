package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

data class YamlConfig(
    @JsonProperty("\$schema")
    val schemaUrl: String,
    val runtime: Runtime = Runtime(),
    val cloud: Cloud = Cloud(),
    val local: Local = Local(),
    val structure: Structure = Structure(),
) {
    val schemaVersion: String = schemaUrl.substringAfterLast("/").removeSuffix(".json") // TODO Modify to support any line

    data class Runtime(
        val version: String? = null,
    )

    data class Cloud(
        val tenant: String? = null,
        val app: String? = null,
        val url: String? = null,
        val authUrl: String? = null,
        val target: String? = null,
        val clientId: String? = null,
        val clientSecret: String? = null,
    )

    data class Local(
        val managementUrl: String? = null,
        val username: String? = null,
        val password: String? = null,
        val withCoverage: Boolean = false,
    )

    data class Structure(
        val nplSources: String? = null,
        val nplTestSources: String? = null,
        val projectDir: String? = null,
        val output: String? = null,
        val nplMigrationDescriptor: String? = null,
        val rules: String? = null,
        val frontEndUrl: String? = null,
    )

    fun getNamedParameter(name: String): Pair<String, String>? {
        val (section, key) = name.split(".", limit = 2)

        return when (section) {
            "cloud" ->
                when (key) {
                    "clientId" -> cloud.clientId?.let { "$section.$key" to it }
                    "clientSecret" -> cloud.clientSecret?.let { "$section.$key" to it }
                    "app" -> cloud.app?.let { "$section.$key" to it }
                    "tenant" -> cloud.tenant?.let { "$section.$key" to it }
//                    "frontend" -> cloud.frontend?.let { "$section.$key" to it }
                    "url" -> cloud.url?.let { "$section.$key" to it }
                    "authUrl" -> cloud.authUrl?.let { "$section.$key" to it }
                    else -> null
                }
            "local" ->
                when (key) {
//                    "sourceDir" -> local.sourceDir?.let { "$section.$key" to it }
//                    "outputDir" -> local.outputDir?.let { "$section.$key" to it }
                    "coverage" -> local.withCoverage?.let { "$section.$key" to it.toString() }
                    else -> null
                }
            "structure" ->
                when (key) {
                    "" -> null
                    else -> null
                }
            else -> null
        }
    }
}

class YamlConfigValues(
    private val map: Map<String, String>,
) {
    fun getValue(key: String): String? = map[key]

    fun getValueOrElse(
        key: String,
        default: String?,
    ): String? = map[key] ?: default
}

object YAMLConfigParser {
    private val mapper = ObjectMapper(YAMLFactory())

    fun parse(): YamlConfig? {
        val configFile = File("npl.yml")

        return if (configFile.exists()) {
            try {
                mapper.readValue(configFile)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun parseValues(): YamlConfigValues? {
        val configFile = File("npl.yml")

        return if (configFile.exists()) {
            try {
                // Deserialize into a nested map
                val nested: Map<String, Map<String, String>> =
                    mapper.readValue(
                        configFile,
                        mapper.typeFactory.constructMapType(Map::class.java, String::class.java, Map::class.java),
                    )

                // Flatten into Map<String, String> with dot-separated keys
                val flat: Map<String, String> =
                    nested
                        .flatMap { (section, inner) ->
                            inner.map { (k, v) -> "$section.$k" to v }
                        }.toMap()
                YamlConfigValues(flat)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
}

interface CommandConfig

data class AppSettings(
    val cloud: CloudSettings,
    val local: LocalSettings,
    val project: ProjectSettings,
) {
    data class CloudSettings(
        val clientId: String? = null,
        val clientSecret: String? = null,
        val cloudApp: String? = null,
        val cloudTenant: String? = null,
        val authUrl: String? = null,
        val deploymentUrl: String? = null,
        val url: String? = null,
        val frontend: File? = null,
        val managementUrl: String? = null,
        val clear: Boolean = false,
        val username: String? = null,
        val password: String? = null,
        val deployTarget: String? = null,
        val deployConfig: File? = null,
    )

    data class LocalSettings(
        val migrationDescriptorFile: File? = null,
        val projectDir: File? = null,
        val withCoverage: Boolean = false,
        val configFile: File? = null,
    )

    data class ProjectSettings(
        val nplSourceDir: File? = null,
        val testSourceDir: File? = null,
        val outputDir: File? = null,
        val rulesFile: File? = null,
    )
}
