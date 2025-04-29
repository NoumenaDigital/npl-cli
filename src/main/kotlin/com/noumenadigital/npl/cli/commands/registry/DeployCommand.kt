package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.config.DeployConfig
import com.noumenadigital.npl.cli.config.EngineTargetConfig
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
        val sourceDir = File(srcDir)
        if (!sourceDir.exists()) {
            writer.error("Target directory does not exist: ${sourceDir.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }

        if (!sourceDir.isDirectory) {
            writer.error("Target path is not a directory: ${sourceDir.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }

        val config = DeployConfig.load()
        val configErrors = DeployConfig.validateTarget(config, targetLabel)
        if (configErrors.isNotEmpty()) {
            writer.error("Configuration errors:")
            configErrors.forEach { writer.error("  $it") }
            writer.info("")
            writer.info("Please create a configuration file at .noumena/config.yml")
            writer.info("with the following format:")
            writer.info(
                """
                targets:
                  $targetLabel:
                    type: engine
                    engineManagementUrl: <URL of the Noumena Engine API>
                    authUrl: <URL of the authentication endpoint>
                    username: <username for authentication>
                    password: <password for authentication>
                    clientId: <client ID for authentication>
                    clientSecret: <client secret for authentication>
                """.trimIndent(),
            )
            return ExitCode.CONFIG_ERROR
        }

        val httpClient = HttpClients.createSystem()
        try {
            val targetConfig = config.targets[targetLabel]

            if (targetConfig == null || targetConfig !is EngineTargetConfig) {
                writer.error("Deployment target '$targetLabel' is not configured correctly.")
                writer.error("Ensure the target exists in your config and has 'type: engine'.")
                writer.error("Currently, deployment is only supported for 'engine' target types.")
                return ExitCode.CONFIG_ERROR
            }

            val userConfig = UserConfiguration(targetConfig.username, targetConfig.password)
            val authConfig =
                AuthConfiguration(
                    clientId = targetConfig.clientId,
                    clientSecret = targetConfig.clientSecret,
                    authUrl = targetConfig.authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)

            ManagementHttpClient(targetConfig.engineManagementUrl, httpClient).use { client ->
                if (cleanFirst) {
                    writer.info("Clearing application contents...")
                    client.clearApplicationContents(authProvider)
                    writer.info("Application contents cleared")
                }

                writer.info("Creating NPL deployment archive...")
                writer.info("Deploying NPL sources and migrations to ${targetConfig.engineManagementUrl}...")

                client.deploySourcesWithMigrations(
                    sourceDirectory = srcDir,
                    authorizationProvider = authProvider,
                )

                writer.success("Successfully deployed NPL sources and migrations to target '$targetLabel'.")
                return ExitCode.SUCCESS
            }
        } catch (e: Exception) {
            writer.error("Error deploying NPL sources: ${e.message ?: "Failed to deploy NPL sources"}")
            return ExitCode.GENERAL_ERROR
        } finally {
            httpClient.close()
        }
    }
}
