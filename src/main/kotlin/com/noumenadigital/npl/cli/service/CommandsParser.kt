package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.CommandsRegistry
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CommandParsingException

object CommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName = command.firstOrNull() ?: throw CommandParsingException("No command to execute")
        val arguments = if (command.size > 1) command.drop(1) else emptyList()
        return CommandsRegistry.commandFromString(commandName, arguments)
    }
}
