package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.Commands
import java.io.Writer

data object HelpCommand : CommandExecutor {
    override val commandName: String = "help"
    override val description: String = "Display the description for npl-cli commands"

    override fun execute(output: Writer): ExitCode {
        val entries = Commands.entries
        val commandPadding = entries.maxOf { it.commandName.length } + 4

        entries.forEach { command ->
            // Print command name and description
            val name = command.commandName.padEnd(commandPadding)
            output.write("$name${command.description}\n")

            // Display parameter descriptions if any
            val executor = Commands.commandFromString(command.commandName)
            executor.parameters.forEach { param ->
                val paramIndent = " ".repeat(commandPadding + 2)

                // Format parameter details
                val requiredMark = if (param.isRequired) " (required)" else ""
                val defaultValue = param.defaultValue
                val defaultText =
                    when {
                        defaultValue == "." -> "(defaults to current directory)"
                        defaultValue != null -> "(defaults to $defaultValue)"
                        else -> ""
                    }

                output.write("$paramIndent<${param.name}>$requiredMark  ${param.description} $defaultText\n")
            }
        }

        return ExitCode.SUCCESS
    }
}
