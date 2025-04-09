package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.CommandsRegistry
import java.io.Writer

data object HelpCommand : CommandExecutor {
    const val COMMAND_DESCRIPTION = "Display the description for npl-cli commands"

    override val commandName: String = "help"

    override fun execute(output: Writer) {
        val entries = CommandsRegistry.registeredCommands
        val padding = entries.keys.maxOf { it.length } + 4

        entries.forEach { registeredCommand ->
            val name = registeredCommand.key.padEnd(padding)
            val description = registeredCommand.value
            output.write("$name$description\n")
        }
    }
}
