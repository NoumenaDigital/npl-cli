package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.NplCliCommandsRegistry
import java.io.Writer

data class HelpCommand(
    val params: List<String>?,
) : NplCommandExecutor {
    companion object {
        const val COMMAND_DESCRIPTION = "Manual for npl-cli commands"
    }

    override val commandName: String = "help"

    override fun execute(output: Writer) {
        val entries = NplCliCommandsRegistry.registeredCommands
        val padding = entries.keys.maxOf { it.length } + 4

        entries.forEach { entry ->
            val name = entry.key.padEnd(padding)
            val description = entry.value.description ?: "No description available"
            output.write("$name$description\n")
        }
    }
}
