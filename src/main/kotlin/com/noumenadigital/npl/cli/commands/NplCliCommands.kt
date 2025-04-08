package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.HelloCommand
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.NplCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

enum class NplCliCommands(
    val commandName: String,
    val nplCommand: NplCommand? = null,
) {
    VERSION("version", VersionCommand),
    HELLO("hello", HelloCommand),
    HELP("help", HelpCommand),
    ;

    companion object {
        fun commandFromString(command: String): NplCliCommands =
            entries.find { it.commandName.equals(command, ignoreCase = true) }
                ?: throw CommandNotFoundException(buildErrorMessage(command))

        private fun buildErrorMessage(input: String): String {
            val suggestion = suggestClosestCommand(input)
            return buildString {
                append("Command not supported: '$input'.")
                if (suggestion != null) {
                    append(" Did you mean '${suggestion.commandName.lowercase()}'?")
                }
            }
        }

        private fun suggestClosestCommand(input: String): NplCliCommands? {
            val distanceCalc = LevenshteinDistance()
            return entries
                .map { it to distanceCalc.apply(it.commandName.lowercase(), input.lowercase()) }
                .minByOrNull { it.second }
                ?.takeIf { it.second <= 3 }
                ?.first
        }
    }
}
