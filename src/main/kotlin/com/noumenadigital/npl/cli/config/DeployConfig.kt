package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EngineTargetConfig::class, name = "engine"),
)
sealed interface DeploymentTargetConfig

data class EngineTargetConfig(
    val engineManagementUrl: String = "http://localhost:12400",
    val authUrl: String = "http://localhost:11000",
    val username: String = "",
    val password: String = "",
    val clientId: String = "foo",
    val clientSecret: String = "bar",
) : DeploymentTargetConfig

data class DeployConfig(
    val schemaVersion: String = "v1",
    val targets: Map<String, DeploymentTargetConfig> = emptyMap(),
) {
    companion object {
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

        /**
         * Load configuration from a YAML file
         * Looks in the following locations in order:
         * 1. Provided current directory .npl/deploy.yml
         * 2. Provided user home directory .npl/deploy.yml
         * @param currentWorkDir The current working directory File object.
         * @param userHomeDir The user's home directory File object.
         */
        fun load(
            currentWorkDir: File = File("."),
            userHomeDir: File = File(System.getProperty("user.home")),
        ): DeployConfig {
            val currentDirConfig = File(currentWorkDir, ".npl/deploy.yml")
            val userHomeConfig = File(userHomeDir, ".npl/deploy.yml")

            val configFile =
                when {
                    currentDirConfig.exists() -> currentDirConfig
                    userHomeConfig.exists() -> userHomeConfig
                    else -> null
                }

            return if (configFile != null) {
                try {
                    mapper.readValue(configFile)
                } catch (_: Exception) {
                    DeployConfig()
                }
            } else {
                DeployConfig()
            }
        }

        /**
         * Checks if the target exists and has required properties set
         */
        fun validateTarget(
            config: DeployConfig,
            targetLabel: String,
        ): List<String> {
            val errors = mutableListOf<String>()

            if (config.schemaVersion != "v1") {
                errors.add("Unsupported configuration schema version '${config.schemaVersion}'. Supported version is 'v1'.")
                // Don't proceed with other checks if the schema is wrong
                return errors
            }

            val target = config.targets[targetLabel]
            if (target == null) {
                errors.add("Target '$targetLabel' not found in configuration")
                return errors
            }

            when (target) {
                is EngineTargetConfig -> {
                    if (target.username.isBlank()) errors.add("username is required for engine target")
                    if (target.password.isBlank()) errors.add("password is required for engine target")
                }
            }

            return errors
        }
    }
}
