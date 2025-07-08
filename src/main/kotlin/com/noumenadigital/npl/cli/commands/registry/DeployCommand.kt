package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.DeployConfig
import com.noumenadigital.npl.cli.config.EngineTargetConfig
import com.noumenadigital.npl.cli.exception.DeployConfigException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.DeployResult
import com.noumenadigital.npl.cli.service.DeployService
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
                description = "Named target from deploy.yml to deploy to. Required unless defaultTarget is set in config.",
                isRequired = false,
                valuePlaceholder = "<name>",
            ),
            NamedParameter(
                name = "sourceDir",
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
        // Parse arguments using the simpler parameter-based parser
        val parser = CommandArgumentParser
        val parsedArgs = parser.parse(args, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            output.error("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        val clearFlag = parsedArgs.hasFlag("clear")
        val targetValue = parsedArgs.getValue("target")
        val sourceDirValue = parsedArgs.getValue("sourceDir")

        if (sourceDirValue == null) {
            output.error("Missing required parameter: --sourceDir <directory>")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        val sourceDirFile = File(sourceDirValue)
        if (!sourceDirFile.exists()) {
            output.error("Source directory does not exist: $sourceDirValue")
            return ExitCode.GENERAL_ERROR
        }
        if (!sourceDirFile.isDirectory) {
            output.error("Source path is not a directory: $sourceDirValue")
            return ExitCode.GENERAL_ERROR
        }

        val targetConfig: EngineTargetConfig
        val config = DeployConfig.load()

        when {
            targetValue != null -> {
                // --target specified: Load and validate from config
                try {
                    targetConfig = loadAndValidateTargetConfig(config, targetValue) // Pass loaded config
                } catch (e: DeployConfigException) {
                    output.error("Configuration error: ${e.message}")
                    // Optionally display full usage or config help here
                    return ExitCode.CONFIG_ERROR
                }
            }

            config.defaultTarget != null -> {
                // No --target, but defaultTarget exists in config
                output.info("Using default target '${config.defaultTarget}' from configuration.")
                try {
                    targetConfig = loadAndValidateTargetConfig(config, config.defaultTarget)
                } catch (e: DeployConfigException) {
                    output.error("Configuration error for default target '${config.defaultTarget}': ${e.message}")
                    return ExitCode.CONFIG_ERROR
                }
            }

            else -> {
                // Neither --target provided, nor defaultTarget in config
                output.error("Missing required parameter: --target <name> or set defaultTarget in deploy.yml")
                displayUsage(output)
                return ExitCode.GENERAL_ERROR
            }
        }

        if (clearFlag) {
            when (val clearResult = deployService.clearApplication(targetConfig)) {
                is DeployResult.ClearSuccess -> {
                    output.info("Application contents cleared for ${targetConfig.engineManagementUrl}")
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

        when (val deployResult = deployService.deploySourcesAndMigrations(targetConfig, sourceDirValue)) {
            is DeployResult.Success -> {
                output.success("Successfully deployed NPL sources and migrations to ${targetConfig.engineManagementUrl}.")
                return ExitCode.SUCCESS
            }

            is DeployResult.DeploymentFailed -> {
                output.error("Error deploying NPL sources: ${deployResult.exception.message ?: "Unknown deployment failure"}")
                return ExitCode.GENERAL_ERROR
            }

            else -> {
                output.error("Internal error: Unhandled deployment result state: $deployResult")
                return ExitCode.INTERNAL_ERROR
            }
        }
    }

    private fun loadAndValidateTargetConfig(
        config: DeployConfig,
        targetLabel: String,
    ): EngineTargetConfig { // Accept config as parameter
        DeployConfig.validateTarget(config, targetLabel)
        return config.targets[targetLabel] as EngineTargetConfig
    }

    private fun displayUsage(writer: ColorWriter) {
        writer.info(USAGE_STRING)
    }

    companion object {
        val USAGE_STRING =
            """
            Usage: deploy --sourceDir <directory> [--target <name>] [--clear]

            Deploys NPL sources to a Noumena Engine instance.

            Arguments:
              --sourceDir <directory>   Directory containing NPL sources (required).
                                 IMPORTANT: The directory must contain a valid NPL source structure, including
                                 migrations. E.g.:
                                  main
                                  ├── npl-1.0
                                  │   └── processes
                                  │       └── demo.npl
                                  └── migration.yml

            Target Specification (one required):
              --target <name>     Named target from deploy.yml to deploy to.
                                If --target is omitted, the 'defaultTarget' from deploy.yml is used if set.

            Options:
              --clear            Clear application contents before deployment.

            Configuration for --target is read from .npl/deploy.yml (current dir or home dir).
            """.trimIndent()
    }
}
