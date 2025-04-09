package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.Commands
import java.io.Writer

data object HelpCommand : CommandExecutor {
    const val COMMAND_DESCRIPTION = "Display the description for npl-cli commands"

    override val commandName: String = "help"

    override fun execute(output: Writer) {
        val entries = Commands.entries
        val padding = entries.maxOf { it.commandName.length } + 4

        entries.forEach { command ->
            val name = command.commandName.padEnd(padding)
            val description = command.description
            output.write("$name$description\n")
        }
    }
}
