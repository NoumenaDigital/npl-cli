package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.service.ColorWriter

open class HelpCommand : CommandExecutor {
    override val commandName: String = "help"
    override val description: String = "Display the description for npl-cli commands"
    override val supportsMcp: Boolean = false

    override fun execute(output: ColorWriter): ExitCode {
        printHelp(Commands.entries.map { it.commandExecutorFactory() }, output)
        return ExitCode.SUCCESS
    }

    protected fun printHelp(
        entries: List<CommandExecutor>,
        output: ColorWriter,
    ) {
        val commandPadding = entries.maxOf { it.commandName.length } + 4
        entries.forEach { command ->
            val name = command.commandName.padEnd(commandPadding)
            output.info("$name${command.description}")
            val executor = command
            executor.parameters
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
                        when (param) {
                            is NamedParameter -> {
                                if (param.valuePlaceholder != null) {
                                    "${param.cliName} ${param.valuePlaceholder}"
                                } else {
                                    param.cliName
                                }
                            }
                        }
                    output.info("$paramIndent$formattedName$requiredMark  ${param.description}$defaultText")
                }
        }
    }
}
