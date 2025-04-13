package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import java.nio.file.Path

object CommandsParser {
    fun parse(
        command: List<String>,
        baseDir: Path? = null,
    ): CommandExecutor {
        val commandName = command.firstOrNull() ?: return Commands.HELP.createExecutor.invoke(emptyList(), baseDir)

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return Commands.commandFromString(commandName, arguments, baseDir)
    }
}
