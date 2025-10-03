package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
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

    fun parse(): YamlConfig? {
        val configFile = File("npl.yml")

        val config: YamlConfig? =
            if (configFile.exists()) {
                try {
                    mapper.readValue(configFile, YamlConfig::class.java)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

        return config
    }
}
