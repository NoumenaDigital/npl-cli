package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

enum class Command(
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
        fun fromString(name: String): Command? = entries.find { it.commandName.equals(name, ignoreCase = true) }
    }
}

object CommandsRegistry {
    fun commandFromString(
        command: String,
        params: List<String> = emptyList(),
    ): CommandExecutor {
        val normalizedCommand = command.lowercase()
        return Command.fromString(normalizedCommand)?.createExecutor?.invoke(params)
            ?: throw CommandNotFoundException(buildErrorMessage(normalizedCommand))
    }

    val registeredCommands: Map<String, String>
        get() =
            Command.entries.associate {
                it.commandName to it.description
            }
}

private fun buildErrorMessage(input: String): String {
    val suggestion = suggestClosestCommand(input)
    return buildString {
        append("Command not supported: '$input'.")
        if (suggestion != null) {
            append(" Did you mean '$suggestion'?")
        }
    }
}

private fun suggestClosestCommand(input: String): String? {
    val distanceCalc = LevenshteinDistance()
    return Command.entries
        .map { it.commandName to distanceCalc.apply(it.commandName, input) }
        .minByOrNull { it.second }
        ?.takeIf { it.second <= 3 }
        ?.first
}
