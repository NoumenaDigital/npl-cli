package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommandsRegistry
import com.noumenadigital.npl.cli.commands.registry.NplCommandExecutor
import com.noumenadigital.npl.cli.exception.CommandParsingException

object NplCommandsParser {
    fun parse(command: List<String>): NplCommandExecutor {
        if (command.isEmpty()) {
            throw CommandParsingException("No command to execute\n")
        }
        val commandName = command.first()
        val arguments = if (command.size > 1) command.drop(1) else emptyList()
        return NplCliCommandsRegistry.commandFromString(commandName, arguments)
    }
}
