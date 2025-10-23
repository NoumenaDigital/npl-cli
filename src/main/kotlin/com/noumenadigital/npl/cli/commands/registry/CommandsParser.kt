package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.Commands

object CommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName = command.firstOrNull() ?: return Commands.HELP.getBaseExecutor().createInstance(emptyList())

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return Commands.commandFromString(commandName, arguments)
    }
}
