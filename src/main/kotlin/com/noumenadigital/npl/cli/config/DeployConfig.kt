package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.noumenadigital.npl.cli.exception.DeployConfigException
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
    val engineManagementUrl: String = "http://localhost:12400/realms/noumena",
    val authUrl: String = "http://localhost:11000",
    val username: String,
    val password: String,
    val clientId: String? = null,
    val clientSecret: String? = null,
) : DeploymentTargetConfig

data class DeployConfig(
    val schemaVersion: String = "v1",
    val targets: Map<String, DeploymentTargetConfig> = emptyMap(),
    val defaultTarget: String? = null,
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

        fun load(configFile: File): DeployConfig =
            try {
                mapper.readValue(configFile)
            } catch (e: Exception) {
                throw DeployConfigException("Failed to load configuration from ${configFile.path}: ${e.message}")
            }

        fun validateTarget(
            config: DeployConfig,
            targetLabel: String,
        ) {
            if (config.schemaVersion != "v1") {
                throw DeployConfigException(
                    message = "Unsupported configuration schema version '${config.schemaVersion}'. Supported version is 'v1'.",
                )
            }

            val target =
                config.targets[targetLabel]
                    ?: throw DeployConfigException("Target '$targetLabel' not found in configuration")

            when (target) {
                is EngineTargetConfig -> {
                    val fieldErrors = mutableListOf<String>()
                    if (target.username.isBlank()) fieldErrors.add("username is required for engine target")
                    if (target.password.isBlank()) fieldErrors.add("password is required for engine target")

                    if (fieldErrors.isNotEmpty()) {
                        throw DeployConfigException(
                            "Configuration errors for target '$targetLabel':\n" +
                                fieldErrors.joinToString("\n") { "  - $it" },
                        )
                    }
                }
            }
        }
    }
}
