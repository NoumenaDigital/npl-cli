package com.noumenadigital.npl.cli.commands.impl

import com.noumenadigital.npl.cli.commands.CommandEnumRegistry
import com.noumenadigital.npl.cli.commands.INplCommand
import java.io.Writer

class HelpCommand : INplCommand {
    override val commandDescription = "Manual for npl-cli commands"

    override fun execute(output: Writer) {
        val entries = CommandEnumRegistry.entries
        val padding = entries.maxOf { it.commandName.length } + 4

        entries.forEach { entry ->
            val name = entry.commandName.padEnd(padding)
            val description = entry.nplCommand?.commandDescription ?: "No description available"
            output.write("$name$description\n")
        }
    }
}