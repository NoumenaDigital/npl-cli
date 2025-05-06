package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
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

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "--target",
                description = "Named target from deploy.yml to deploy to. Required unless --dev is used.",
                isRequired = false,
                valuePlaceholder = "<name>",
            ),
            NamedParameter(
                name = "--sourceDir",
                description = "Directory containing NPL sources",
                isRequired = true,
                valuePlaceholder = "<directory>",
            ),
            NamedParameter(
                name = "--clear",
                description = "Clear application contents before deployment",
                isRequired = false,
            ),
            NamedParameter(
                name = "--dev",
                description = "Use default local development settings (ignores deploy.yml unless --target is also specified)",
                isRequired = false,
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = DeployCommand(params)

    override fun execute(output: ColorWriter): ExitCode {
        // Parse arguments using the simpler parameter-based parser
        val parser = CommandArgumentParser()
        val parsedArgs = parser.parse(args, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            output.error("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        val devFlag = parsedArgs.hasFlag("--dev")
        val clearFlag = parsedArgs.hasFlag("--clear")
        val targetValue = parsedArgs.getValue("--target")
        val sourceDirValue = parsedArgs.getValue("--sourceDir")

        if (sourceDirValue == null) {
            output.error("Missing required parameter: --sourceDir <directory>")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        val sourceDirFile = File(sourceDirValue)
        if (!sourceDirFile.exists()) {
            output.error("Source directory does not exist: ${sourceDirFile.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }
        if (!sourceDirFile.isDirectory) {
            output.error("Source path is not a directory: ${sourceDirFile.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }

        val targetConfig: EngineTargetConfig
        when {
            devFlag && targetValue == null -> {
                // --dev used, no --target: Use defaults
                output.info("Using default local development settings.")
                targetConfig = DeployConfig.DEFAULT_DEV_CONFIG
                // Skip validation for default config
            }

            targetValue != null -> {
                // --target specified (with or without --dev): Load and validate from config
                try {
                    targetConfig = loadAndValidateTargetConfig(targetValue)
                } catch (e: DeployConfigException) {
                    output.error("Configuration error: ${e.message}")
                    // Optionally display full usage or config help here
                    return ExitCode.CONFIG_ERROR
                }
            }

            else -> {
                // Neither --dev nor --target provided: --target is required
                output.error("Missing required parameter: --target <name> (or use --dev for local defaults)")
                displayUsage(output)
                return ExitCode.GENERAL_ERROR
            }
        }

        if (clearFlag) {
            output.info("Clearing application contents for ${targetConfig.engineManagementUrl}...")
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

        output.info("Creating NPL deployment archive...")
        output.info("Deploying NPL sources and migrations to ${targetConfig.engineManagementUrl}...")

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

    private fun loadAndValidateTargetConfig(targetLabel: String): EngineTargetConfig {
        val config = DeployConfig.load()
        DeployConfig.validateTarget(config, targetLabel)
        return config.targets[targetLabel] as EngineTargetConfig
    }

    private fun displayUsage(writer: ColorWriter) {
        writer.info(USAGE_STRING)
    }

    companion object {
        val USAGE_STRING =
            """
            Usage: deploy [--target <name> | --dev] --sourceDir <directory> [--clear]

            Deploys NPL sources to a Noumena Engine instance.

            Arguments:
              --sourceDir <directory>   Directory containing NPL sources (required).
                                 IMPORTANT: The directory must contain a valid NPL source structure, including
                                 migrations. E.g.:
                                  main
                                  ├── npl-1.0
                                  │   └── processes
                                  │       └── demo.npl
                                  └── yaml
                                      └── migration.yml

            Target Specification (one required):
              --target <name>     Named target from deploy.yml to deploy to.
              --dev              Use default local development settings (localhost:12400, user 'alice').
                                 If both --dev and --target are given, --target takes precedence.

            Options:
              --clear            Clear application contents before deployment.

            Configuration for --target is read from .npl/deploy.yml (current dir or home dir).
            """.trimIndent()
    }
}
