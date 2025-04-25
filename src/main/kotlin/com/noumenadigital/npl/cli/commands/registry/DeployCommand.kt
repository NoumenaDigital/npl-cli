package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.config.EngineConfig
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.platform.client.auth.AuthConfiguration
import com.noumenadigital.platform.client.auth.TokenAuthorizationProvider
import com.noumenadigital.platform.client.auth.UserConfiguration
import com.noumenadigital.platform.client.engine.ManagementHttpClient
import org.apache.http.impl.client.HttpClients
import java.io.File

class DeployCommand(
    private val targetLabel: String,
    private val srcDir: String = ".",
    private val cleanFirst: Boolean = false,
) {
    fun execute(writer: ColorWriter): ExitCode {
        // Verify the source directory exists
        val sourceDir = File(srcDir)
        if (!sourceDir.exists()) {
            writer.error("Target directory does not exist: ${sourceDir.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }

        if (!sourceDir.isDirectory) {
            writer.error("Target path is not a directory: ${sourceDir.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }

        // Load configuration
        val config = EngineConfig.load()
        val configErrors = EngineConfig.validateTarget(config, targetLabel)
        if (configErrors.isNotEmpty()) {
            writer.error("Configuration errors:")
            configErrors.forEach { writer.error("  $it") }
            writer.info("")
            writer.info("Please create a configuration file at .noumena/config.json")
            writer.info("with the following format:")
            writer.info(
                """
                {
                  "targets": {
                    "$targetLabel": {
                      "engineManagementUrl": "<URL of the Noumena Engine API>",
                      "authUrl": "<URL of the authentication endpoint>",
                      "username": "<username for authentication>",
                      "password": "<password for authentication>",
                      "clientId": "<client ID for authentication>",
                      "clientSecret": "<client secret for authentication>"
                    }
                  }
                }
                """.trimIndent(),
            )
            return ExitCode.CONFIG_ERROR
        }

        try {
            // Create an HTTP client
            val httpClient = HttpClients.createSystem()

            // Get the target configuration
            val target = config.targets[targetLabel]!!

            // Create the authorization provider
            val userConfig = UserConfiguration(target.username, target.password)
            val authConfig =
                AuthConfiguration(
                    clientId = target.clientId,
                    clientSecret = target.clientSecret,
                    authUrl = target.authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)

            // Use the ManagementHttpClient to deploy sources
            ManagementHttpClient(target.engineManagementUrl, httpClient).use { client ->
                // Clear application contents if requested
                if (cleanFirst) {
                    writer.info("Clearing application contents...")
                    client.clearApplicationContents(authProvider)
                    writer.info("Application contents cleared")
                }

                // Deploy the sources based on configuration
                writer.info("Creating NPL deployment archive...")
                writer.info("Deploying NPL sources and migrations to ${target.engineManagementUrl}...")

                client.deploySourcesWithMigrations(
                    sourceDirectory = srcDir,
                    authorizationProvider = authProvider,
                )

                writer.info("Successfully deployed NPL sources and migrations to target '$targetLabel'.")
                return ExitCode.SUCCESS
            }
        } catch (e: Exception) {
            writer.error("Error deploying NPL sources: ${e.message}")
            return ExitCode.GENERAL_ERROR
        }
    }
}
