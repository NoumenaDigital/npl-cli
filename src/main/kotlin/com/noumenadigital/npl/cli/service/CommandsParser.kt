package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.model.Command

sealed interface CommandsParser {
    fun parse(command: List<String>): List<Command>
}

data object NplCommandsParser : CommandsParser {
    override fun parse(command: List<String>): List<Command> {
        if (command.isEmpty()) {
            throw CommandParsingException("No command to execute\n")
        }
        val commands =
            command.map { stringCommand -> Command(NplCliCommandsEnum.commandFromString(stringCommand)) }.toList()
        return commands
    }
}
