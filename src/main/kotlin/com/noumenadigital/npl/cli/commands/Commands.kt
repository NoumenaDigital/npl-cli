package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class Commands(
    val commandName: String,
    val description: String,
    val createExecutor: (List<String>) -> CommandExecutor,
) {
    VERSION(
        "version",
        VersionCommand.COMMAND_DESCRIPTION,
        { VersionCommand },
    ),
    HELP(
        "help",
        HelpCommand.COMMAND_DESCRIPTION,
        { HelpCommand },
    ),
    ;

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
        ): CommandExecutor {
            val normalizedCommand = command.lowercase()
            return Commands.fromString(normalizedCommand)?.createExecutor?.invoke(params)
                ?: throw CommandNotFoundException(normalizedCommand)
        }

        private fun fromString(name: String): Commands? = entries.find { it.commandName.equals(name, ignoreCase = true) }
    }
}
