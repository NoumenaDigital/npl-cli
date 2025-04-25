package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter

class DeployCommandExecutor(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override val commandName: String = "deploy"
    override val description: String = "Deploy NPL sources to a Noumena Engine instance"

    override val parameters: List<CommandParameter> =
        listOf(
            CommandParameter(
                name = "target",
                description = "Named target from config.json to deploy to",
                isRequired = true,
            ),
            CommandParameter(
                name = "directory",
                description = "Directory containing NPL sources",
                defaultValue = ".",
            ),
            CommandParameter(
                name = "--clean",
                description = "Clear application contents before deployment",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = DeployCommandExecutor(params)

    override fun execute(output: ColorWriter): ExitCode {
        if (args.isEmpty()) {
            output.error("Missing required parameter: target")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }

        // Parse arguments
        val cleanFlag = args.contains("--clean")

        // Remove the --clean flag if present to simplify remaining arg processing
        val argsWithoutClean = args.filterNot { it == "--clean" }

        // First arg is always the target
        val target = argsWithoutClean.first()

        // Second arg is directory, if provided
        val directory = if (argsWithoutClean.size > 1) argsWithoutClean[1] else "."

        return DeployCommand(
            targetLabel = target,
            srcDir = directory,
            cleanFirst = cleanFlag,
        ).execute(output)
    }

    private fun displayUsage(output: ColorWriter) {
        output.info("Usage: deploy <target> [directory] [--clean]")
        output.info()
        output.info("Arguments:")
        output.info("  target           Named target from config.json to deploy to")
        output.info("  directory        Directory containing NPL sources (defaults to current directory)")
        output.info()
        output.info("Options:")
        output.info("  --clean          Clear application contents before deployment")
        output.info()
        output.info("Configuration is read from .noumena/config.json in the current directory")
        output.info("or the user's home directory.")
    }
}
