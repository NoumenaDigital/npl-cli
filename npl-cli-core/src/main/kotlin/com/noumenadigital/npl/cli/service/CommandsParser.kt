package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor

object CommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName = command.firstOrNull() ?: return Commands.HELP.getBaseExecutor().createInstance(emptyList())

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return Commands.commandFromString(commandName, arguments)
    }
}
