package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.commands.CommandEnum
import com.noumenadigital.npl.cli.commands.model.Command
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.service.ICommandsParser

class NplCommandsParser : ICommandsParser {

    override fun parse(command: List<String>): List<Command> {
        if (command.isEmpty()) {
            throw CommandParsingException("No command to execute")
        }
        val commands = command.map { stringCommand -> Command(CommandEnum.commandFromString(stringCommand)) }.toList()
        return commands
    }


}
