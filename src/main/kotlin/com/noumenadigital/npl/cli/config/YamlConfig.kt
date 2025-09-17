package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
        val application: String? = null,
        val portalUrl: String? = null,
        val authUrl: String? = null,
        val deployTarget: String? = null,
    )

    data class Local(
        val managementUrl: String? = null,
        val username: String? = null,
        val password: String? = null,
        val clientId: String? = null,
        val clientSecret: String? = null,
        val withCoverage: Boolean = false,
        val templateUrl: String? = null,
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
}

object YAMLConfigParser {
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

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
}
