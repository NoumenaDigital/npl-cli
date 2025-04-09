package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.GenericCommand
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.NplCommandExecutor
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

object NplCliCommandsRegistry {
    private val commands: Map<String, GenericCommand> =
        mapOf(
            "version" to GenericCommand(VersionCommand.COMMAND_DESCRIPTION) { VersionCommand() },
            "help" to GenericCommand(HelpCommand.COMMAND_DESCRIPTION) { param -> HelpCommand(param) },
        )

    fun commandFromString(
        command: String,
        params: List<String>? = emptyList(),
    ): NplCommandExecutor {
        val normalizedCommand = command.lowercase()
        return commands[normalizedCommand]?.executor?.invoke(params)
            ?: throw CommandNotFoundException(buildErrorMessage(normalizedCommand))
    }

    val registeredCommands: Map<String, GenericCommand>
        get() = commands

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
        return commands.keys
            .map { it to distanceCalc.apply(it, input) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= 3 }
            ?.first
    }
}
