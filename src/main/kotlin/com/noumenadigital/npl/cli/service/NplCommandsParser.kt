package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommands
import com.noumenadigital.npl.cli.exception.CommandParsingException

data object NplCommandsParser {
    fun parse(commands: List<String>): NplCliCommands {
        if (commands.isEmpty()) {
            throw CommandParsingException("No command to execute\n")
        }
        if (commands.size != 1) {
            throw CommandParsingException("Only 1 command can be processed, but was $commands\n")
        }
        return NplCliCommands.commandFromString(commands.first())
    }
}
