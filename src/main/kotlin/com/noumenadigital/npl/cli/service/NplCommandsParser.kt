package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommands
import com.noumenadigital.npl.cli.exception.CommandParsingException

data object NplCommandsParser {
    fun parse(commands: List<String>): NplCliCommands {
        if (commands.isEmpty()) {
            throw CommandParsingException("No command to execute\n")
        }
        return NplCliCommands.commandFromString(commands.first())
    }
}
