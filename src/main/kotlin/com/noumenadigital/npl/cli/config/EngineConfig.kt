package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

data class DeployTarget(
    val engineManagementUrl: String = "http://localhost:12400",
    val authUrl: String = "http://localhost:11000",
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
)

class EngineConfig(
    val targets: Map<String, DeployTarget> = emptyMap(),
) {
    companion object {
        private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        /**
         * Load configuration from a JSON file
         * Looks in the following locations in order:
         * 1. Current directory .noumena/config.json
         * 2. User home directory .noumena/config.json
         */
        fun load(): EngineConfig {
            // Check current directory first
            val currentDirConfig = File(".noumena/config.json")
            val userHomeConfig = File(System.getProperty("user.home"), ".noumena/config.json")

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
                    EngineConfig()
                }
            } else {
                EngineConfig()
            }
        }

        /**
         * Checks if the target exists and has required properties set
         */
        fun validateTarget(
            config: EngineConfig,
            targetLabel: String,
        ): List<String> {
            val errors = mutableListOf<String>()

            val target = config.targets[targetLabel]
            if (target == null) {
                errors.add("Target '$targetLabel' not found in configuration")
                return errors
            }

            if (target.engineManagementUrl.isBlank()) errors.add("engineManagementUrl is required")
            if (target.authUrl.isBlank()) errors.add("authUrl is required")
            if (target.username.isBlank()) errors.add("username is required")
            if (target.password.isBlank()) errors.add("password is required")
            if (target.clientId.isBlank()) errors.add("clientId is required")

            return errors
        }
    }
}
