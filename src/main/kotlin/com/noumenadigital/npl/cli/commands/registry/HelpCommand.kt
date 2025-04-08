package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.NplCliCommands
import com.noumenadigital.npl.cli.model.CommandContext

data object HelpCommand : NplCommand {
    override val commandDescription = "Manual for npl-cli commands"

    override fun execute(commandContext: CommandContext) {
        val entries = NplCliCommands.entries
        val padding = entries.maxOf { it.commandName.length } + 4

        entries.forEach { entry ->
            val name = entry.commandName.padEnd(padding)
            val description = entry.nplCommand?.commandDescription ?: "No description available"
            commandContext.output.write("$name$description\n")
        }
    }
}
