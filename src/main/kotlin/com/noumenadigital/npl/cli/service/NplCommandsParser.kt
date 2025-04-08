package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommandsRegistry
import com.noumenadigital.npl.cli.commands.registry.NplCommand
import com.noumenadigital.npl.cli.exception.CommandParsingException

data object NplCommandsParser {
    fun parse(command: List<String>): NplCommand {
        if (command.isEmpty()) {
            throw CommandParsingException("No command to execute\n")
        }
        if (command.size != 1) {
            throw CommandParsingException("Only 1 command can be processed\n")
        }
        return NplCliCommandsRegistry.commandFromString(command.first(), null)
    }
}
