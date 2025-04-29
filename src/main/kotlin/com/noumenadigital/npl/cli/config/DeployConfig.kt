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

class DeployConfig(
    val targets: Map<String, DeploymentTargetConfig> = emptyMap(),
) {
    companion object {
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

        /**
         * Load configuration from a YAML file
         * Looks in the following locations in order:
         * 1. Current directory .npl/deploy.yml
         * 2. User home directory .npl/deploy.yml
         */
        fun load(): DeployConfig {
            val currentDirConfig = File(".npl/deploy.yml")
            val userHomeConfig = File(System.getProperty("user.home"), ".npl/deploy.yml")

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
