package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

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
                ?: throw CommandNotFoundException(normalizedCommand, suggestClosestCommand(normalizedCommand))
        }

        private fun fromString(name: String): Commands? = entries.find { it.commandName.equals(name, ignoreCase = true) }

        private fun suggestClosestCommand(input: String): String? {
            val distanceCalc = LevenshteinDistance()
            return Commands.entries
                .map { it.commandName to distanceCalc.apply(it.commandName, input) }
                .minByOrNull { it.second }
                ?.takeIf { it.second <= 3 }
                ?.first
        }
    }
}
