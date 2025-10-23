package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.commands.CommandsRegistry

object CommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName = command.firstOrNull() ?: return Commands.HELP.getCommandDescriptor().createCommandExecutorInstance(emptyMap())

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return CommandsRegistry.commandFromString(commandName, arguments, Commands.entries.toTypedArray())
    }
}
