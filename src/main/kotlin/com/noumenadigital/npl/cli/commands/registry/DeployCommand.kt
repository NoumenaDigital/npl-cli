package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.ArgumentParser
import com.noumenadigital.npl.cli.commands.CommandConfig
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CloudDeployConfig.Companion.AUTH_URL_DEFAULT
import com.noumenadigital.npl.cli.commands.registry.CloudDeployConfig.Companion.MGMT_URL_DEFAULT
import com.noumenadigital.npl.cli.config.EngineTargetConfig
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.DeployResult
import com.noumenadigital.npl.cli.service.DeployService
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File

class DeployCommand(
    private val args: List<String> = emptyList(),
    private val deployService: DeployService = DeployService(),
) : CommandExecutor {
    override val commandName: String = "deploy"
    override val description: String = "Deploy NPL sources to a Noumena Engine instance"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "target",
                description = "Named target from npl.yml to deploy to. Required unless defaultTarget is set in config.",
                isRequired = false,
                valuePlaceholder = "<name>",
            ),
            NamedParameter(
                name = "source-dir",
                description = "Directory containing NPL sources",
                isRequired = true,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
            ),
            NamedParameter(
                name = "clear",
                description = "Clear application contents before deployment",
                isRequired = false,
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = DeployCommand(params)

    override fun execute(output: ColorWriter): ExitCode {
        val config =
            ArgumentParser.parse(args, parameters) { settings ->
                CloudDeployConfig(
                    target = settings.cloud.target,
                    username = settings.local.username,
                    password = settings.local.password,
                    sourceDir = settings.structure.nplSourceDir,
                    clear = settings.cloud.clear,
                    authUrl = settings.cloud.authUrl ?: AUTH_URL_DEFAULT,
                    clientId = settings.local.clientId,
                    clientSecret = settings.local.clientSecret,
                    engineManagementUrl = settings.local.managementUrl ?: MGMT_URL_DEFAULT,
                )
            }

//        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
//            output.error("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
//            displayUsage(output)
//            return ExitCode.GENERAL_ERROR
//        }

        if (config.sourceDir == null) {
            output.error("Missing required parameter: --source-dir <directory>")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        if (!config.sourceDir.exists()) {
            output.error("Source directory does not exist: ${config.sourceDir.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        if (!config.sourceDir.isDirectory) {
            output.error("Source path is not a directory: ${config.sourceDir.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        if (config.target == null) {
            output.error("Missing required parameter: --target <name> or set defaultTarget in npl.yml")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        if (config.username == null) {
            output.error("Configuration error '${config.username}': username is required for engine target")
            return ExitCode.CONFIG_ERROR
        }

        if (config.password == null) {
            output.error("Configuration error '${config.password}': password is required for engine target")
            return ExitCode.CONFIG_ERROR
        }

        val targetConfig =
            EngineTargetConfig(
                engineManagementUrl = config.engineManagementUrl,
                authUrl = config.authUrl,
                username = config.username,
                password = config.password,
                clientId = config.clientId,
                clientSecret = config.clientSecret,
            )

        if (config.clear) {
            when (val clearResult = deployService.clearApplication(targetConfig)) {
                is DeployResult.ClearSuccess -> {
                    output.info("Application contents cleared for ${config.engineManagementUrl}")
                }

                is DeployResult.ClearFailed -> {
                    output.error("Failed to clear application contents: ${clearResult.exception.message ?: "Unknown error"}")
                    return ExitCode.GENERAL_ERROR
                }

                else -> {
                    output.error("Unexpected result during clear operation: $clearResult")
                    return ExitCode.GENERAL_ERROR
                }
            }
        }

        return when (val deployResult = deployService.deploySourcesAndMigrations(targetConfig, config.sourceDir.absolutePath)) {
            is DeployResult.Success -> {
                output.success("Successfully deployed NPL sources and migrations to ${targetConfig.engineManagementUrl}.")
                ExitCode.SUCCESS
            }

            is DeployResult.DeploymentFailed -> {
                output.error("Error deploying NPL sources: ${deployResult.exception.message ?: "Unknown deployment failure"}")
                ExitCode.GENERAL_ERROR
            }

            else -> {
                output.error("Internal error: Unhandled deployment result state: $deployResult")
                ExitCode.INTERNAL_ERROR
            }
        }
    }

    private fun displayUsage(writer: ColorWriter) {
        writer.info(USAGE_STRING)
    }

    companion object {
        val USAGE_STRING =
            """
            Usage: deploy --source-dir <directory> [--target <name>] [--config-file <path>] [--clear]

            Deploys NPL sources to a Noumena Engine instance.

            Arguments:
              --source-dir <directory>   Directory containing NPL sources (required).
                                 IMPORTANT: The directory must contain a valid NPL source structure, including
                                 migrations. E.g.:
                                  main
                                  ├── npl-1.0
                                  └── migration.yml
                                  └── npl.yml

            Target Specification (one required):
              --target <name>     Named target from deploy.yml to deploy to.
                                If --target is omitted, the 'defaultTarget' from deploy.yml is used if set.

            Options:
              --clear             Clear application contents before deployment.

            Configuration for --target is read from .npl/deploy.yml (current dir).
            """.trimIndent()
    }
}

data class CloudDeployConfig(
    val authUrl: String = AUTH_URL_DEFAULT,
    val clear: Boolean = false,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val engineManagementUrl: String = MGMT_URL_DEFAULT,
    val password: String? = null,
    val sourceDir: File? = null,
    val target: String? = null,
    val username: String? = null,
) : CommandConfig {
    companion object {
        const val AUTH_URL_DEFAULT: String = "http://localhost:11000"
        const val MGMT_URL_DEFAULT: String = "http://localhost:12400/realms/noumena"
    }
}
