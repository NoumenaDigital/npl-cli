package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.impl.HelloCommand
import com.noumenadigital.npl.cli.commands.impl.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

enum class CommandEnumRegistry(val commandName: String, val nplCommand: INplCommand? = null) {

    VERSION("VERSION", VersionCommand()),
    HELLO("HELLO", HelloCommand());

    companion object {
        fun commandFromString(command: String): CommandEnumRegistry {
            return entries.find { it.commandName.equals(command, ignoreCase = true) }
                ?: throw CommandNotFoundException(buildErrorMessage(command))
        }

        private fun buildErrorMessage(input: String): String {
            val suggestion = suggestClosestCommand(input)
            return buildString {
                append("Command not supported: '$input'.")
                if (suggestion != null) {
                    append(" Did you mean '${suggestion.commandName.lowercase()}'?")
                }
            }
        }

        private fun suggestClosestCommand(input: String): CommandEnumRegistry? {
            val distanceCalc = LevenshteinDistance()
            return entries
                .map { it to distanceCalc.apply(it.commandName.lowercase(), input.lowercase()) }
                .minByOrNull { it.second }
                ?.takeIf { it.second <= 3 }
                ?.first
        }
    }
}
