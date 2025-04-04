package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.commands.CommandEnum
import com.noumenadigital.npl.cli.commands.model.Command
import com.noumenadigital.npl.cli.service.ICommandsParser

class NplCommandsParser : ICommandsParser {

    override fun parse(command: List<String>): List<Command> {
        val commands = command.map { x -> Command(CommandEnum.valueOf(x)) }.toList()
        return commands
    }


}
