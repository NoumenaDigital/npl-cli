package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor

object CloudCommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName =
            command.firstOrNull() ?: return CloudCommands.CLOUD_HELP.getBaseExecutor().createInstance(emptyList())

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return CloudCommands.commandFromString(commandName, arguments)
    }
}
