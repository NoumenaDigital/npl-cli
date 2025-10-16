package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.service.ColorWriter

object HeloCommandDescriptor : CommandDescriptor {
    override val commandName: String = "help"
    override val description: String = "Display the description for npl-cli commands"
    override val supportsMcp: Boolean = false

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor = HelpCommand()
}

open class HelpCommand : CommandExecutor {
    override fun execute(output: ColorWriter): ExitCode {
        printHelp(Commands.entries.map { it.commandDescriptor() }, output)
        return ExitCode.SUCCESS
    }

    protected fun printHelp(
        entries: List<CommandDescriptor>,
        output: ColorWriter,
    ) {
        val commandPadding = entries.maxOf { it.commandName.length } + 4
        entries.forEach { command ->
            val name = command.commandName.padEnd(commandPadding)
            output.info("$name${command.description}")
            command.parameters
                .filter { !it.isHidden }
                .forEach { param ->
                    val paramIndent = " ".repeat(commandPadding + 2)
                    val requiredMark = if (param.isRequired) " (required)" else ""
                    val defaultValue = param.defaultValue
                    val defaultText =
                        when {
                            defaultValue == "." -> " (defaults to current directory)"
                            defaultValue != null -> " (defaults to $defaultValue)"
                            else -> ""
                        }
                    val formattedName =
                        if (param.valuePlaceholder != null) {
                            "${param.cliName} ${param.valuePlaceholder}"
                        } else {
                            param.cliName
                        }
                    output.info("$paramIndent$formattedName$requiredMark  ${param.description}$defaultText")
                }

            if (command.envVariables.isNotEmpty()) {
                val envIndent = " ".repeat(commandPadding + 2)
                command.envVariables.forEach { envVar ->
                    val requiredMark = if (envVar.isRequired) " (required)" else ""
                    output.info("$envIndent${envVar.name}  ${envVar.type}$requiredMark: ${envVar.description}")
                }
            }
        }
    }
}
