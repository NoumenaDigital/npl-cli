package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import java.io.Writer

data object HelpCommand : NplCommand {
    override val commandDescription = "Manual for npl-cli commands"

    override fun execute(output: Writer) {
        val entries = NplCliCommandsEnum.entries
        val padding = entries.maxOf { it.commandName.length } + 4

        entries.forEach { entry ->
            val name = entry.commandName.padEnd(padding)
            val description = entry.nplCommand?.commandDescription ?: "No description available"
            output.write("$name$description\n")
        }
    }
}
