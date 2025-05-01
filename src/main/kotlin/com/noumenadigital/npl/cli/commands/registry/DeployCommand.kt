package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.config.DeployConfig
import com.noumenadigital.npl.cli.config.EngineTargetConfig
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
            CommandParameter(
                name = "target",
                description = "Named target from deploy.yml to deploy to",
                isRequired = true,
            ),
            CommandParameter(
                name = "directory",
                description = "Directory containing NPL sources",
                isRequired = true,
            ),
            CommandParameter(
                name = "--clear",
                description = "Clear application contents before deployment",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = DeployCommand(params)

    override fun execute(output: ColorWriter): ExitCode {
        val (options, positionalArgs) = args.partition { it.startsWith("--") }

        val clearFlag = options.contains("--clear")

        if (positionalArgs.isEmpty()) {
            output.error("Missing required parameter: target")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        val targetLabel = positionalArgs[0]

        if (positionalArgs.size < 2) {
            output.error("Missing required parameter: directory")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }
        val srcDir = positionalArgs[1]

        val sourceDirFile = File(srcDir)
        if (!sourceDirFile.exists()) {
            output.error("Target directory does not exist: ${sourceDirFile.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }
        if (!sourceDirFile.isDirectory) {
            output.error("Target path is not a directory: ${sourceDirFile.absolutePath}")
            return ExitCode.GENERAL_ERROR
        }
        val targetConfig = validateTargetConfig(targetLabel)

        if (clearFlag) {
            output.info("Clearing application contents...")
            when (val clearResult = deployService.clearApplication(targetLabel)) {
                is DeployResult.ClearSuccess -> {
                    output.info("Application contents cleared")
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

        val engineManagementUrl = targetConfig.engineManagementUrl

        output.info("Creating NPL deployment archive...")
        output.info("Deploying NPL sources and migrations to $engineManagementUrl...")

        when (val deployResult = deployService.deploySourcesAndMigrations(targetLabel, srcDir)) {
            is DeployResult.Success -> {
                output.success("Successfully deployed NPL sources and migrations to target '${deployResult.targetLabel}'.")
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

    private fun validateTargetConfig(targetLabel: String): EngineTargetConfig {
        val config = DeployConfig.load()
        DeployConfig.validateTarget(config, targetLabel)

        return config.targets[targetLabel] as EngineTargetConfig
    }

    private fun displayUsage(writer: ColorWriter) {
        writer.info(
            """
            Usage: deploy <target> <directory> [--clear]

            Arguments:
              target           Named target from deploy.yml to deploy to
              directory        Directory containing NPL sources.
                               IMPORTANT: The directory must contain a valid NPL source structure, including
                               migrations. E.g.:
                                main
                                ├── npl-1.0
                                │   └── processes
                                │       └── demo.npl
                                └── yaml
                                    └── migration.yml

            Options:
              --clear          Clear application contents before deployment

            Configuration is read from .npl/deploy.yml in the current directory
            or the user's home directory (~/.npl/deploy.yml).
            """.trimIndent(),
        )
    }
}
