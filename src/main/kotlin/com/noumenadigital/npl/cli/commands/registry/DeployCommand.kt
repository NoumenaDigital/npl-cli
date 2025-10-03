package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.RequiredParameterMissing
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.DeployResult
import com.noumenadigital.npl.cli.service.DeployService
import com.noumenadigital.npl.cli.settings.DefaultSettingsProvider
import com.noumenadigital.npl.cli.settings.SettingsProvider
import com.noumenadigital.npl.cli.util.relativeOrAbsolute

class DeployCommand(
    private val args: List<String> = emptyList(),
    private val deployService: DeployService = DeployService(),
    private val settings: SettingsProvider? = null,
) : CommandExecutor {
    override val commandName: String = "deploy"
    override val description: String = "Deploy NPL sources to a Noumena Engine instance"

    override val parameters: List<NamedParameter> =
        listOf(
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

    override fun createInstance(params: List<String>): CommandExecutor =
        DeployCommand(params, deployService, DefaultSettingsProvider(params, parameters))

    override fun execute(output: ColorWriter): ExitCode {
        val settings = settings ?: DefaultSettingsProvider(args, parameters)
        val localSettings = settings.local
        val structureSettings = settings.structure

        val sourceDir = structureSettings.nplSourceDir
        if (sourceDir == null) {
            output.error("Missing required parameter: --source-dir <directory>")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        if (!sourceDir.exists()) {
            output.error("Source directory does not exist: ${sourceDir.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        if (!sourceDir.isDirectory) {
            output.error("Source path is not a directory: ${sourceDir.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        val username =
            localSettings.username
                ?: throw RequiredParameterMissing(
                    parameterName = "username",
                    yamlExample = "local:\n  username: <username>",
                )

        val password =
            localSettings.password
                ?: throw RequiredParameterMissing(
                    parameterName = "password",
                    yamlExample = "local:\n  password: <password>",
                )

        if (localSettings.clear) {
            when (val clearResult = deployService.clearApplication(localSettings)) {
                is DeployResult.ClearSuccess -> {
                    output.info("Application contents cleared for ${localSettings.managementUrl}")
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

        return when (val deployResult = deployService.deploySourcesAndMigrations(localSettings, sourceDir.absolutePath)) {
            is DeployResult.Success -> {
                output.success("Successfully deployed NPL sources and migrations to ${localSettings.managementUrl}.")
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
            Usage: deploy --source-dir <directory> [--clear]

            Deploys NPL sources to a Noumena Engine instance.

            Arguments:
              --source-dir <directory>   Directory containing NPL sources (required).
                                 IMPORTANT: The directory must contain a valid NPL source structure, including
                                 migrations. E.g.:
                                  main
                                  ├── npl-1.0
                                  └── migration.yml

            Options:
              --clear             Clear application contents before deployment.

            Configuration is read from npl.yml (current dir).
            """.trimIndent()
    }
}
