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
                description = "Named target from deploy.yml to deploy to",
                isRequired = true,
            ),
            CommandParameter(
                name = "directory",
                description = "Directory containing NPL sources",
                isRequired = true,
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
        if (argsWithoutClean.size < 2) {
            output.error("Missing required parameter: directory")
            displayUsage(output)
            return ExitCode.GENERAL_ERROR
        }
        val directory = argsWithoutClean[1]

        return DeployCommand(
            targetLabel = target,
            srcDir = directory,
            cleanFirst = cleanFlag,
        ).execute(output)
    }

    private fun displayUsage(output: ColorWriter) {
        output.info("Usage: deploy <target> <directory> [--clean]")
        output.info()
        output.info("Arguments:")
        output.info("  target           Named target from deploy.yml to deploy to")
        output.info("  directory        Directory containing NPL sources.")
        output.info("                   IMPORTANT: The directory must contain a valid NPL source structure, including")
        output.info("                   migrations. E.g.:")
        output.info("                    main")
        output.info("                    ├── npl-1.0")
        output.info("                    │   └── processes")
        output.info("                    │       └── demo.npl")
        output.info("                    └── yaml")
        output.info("                        └── migration.yml")
        output.info()
        output.info("Options:")
        output.info("  --clean          Clear application contents before deployment")
        output.info()
        output.info("Configuration is read from .npl/deploy.yml in the current directory")
        output.info("or the user's home directory (~/.npl/deploy.yml).")
    }
}
