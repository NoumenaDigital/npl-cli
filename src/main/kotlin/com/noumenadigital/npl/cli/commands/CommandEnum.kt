package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.impl.HelloCommand
import com.noumenadigital.npl.cli.commands.impl.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import org.apache.commons.text.similarity.LevenshteinDistance

enum class CommandEnum(val commandName: String, nplCommand: INplCommand? = null) {

    VERSION("VERSION", VersionCommand()),
    HELLO("HELLO", HelloCommand());

    companion object {
        fun commandFromString(command: String): CommandEnum {
            return entries.find { it.commandName.equals(command, ignoreCase = true) }
                ?: throw CommandNotFoundException(buildErrorMessage(command))
        }

        private fun buildErrorMessage(input: String): String {
            val suggestion = suggestClosestCommand(input)
            return buildString {
                append("Unknown command: '$input'.")
                if (suggestion != null) {
                    append(" Did you mean '${suggestion.commandName}'?")
                }
            }
        }

        private fun suggestClosestCommand(input: String): CommandEnum? {
            val distanceCalc = LevenshteinDistance()
            return entries
                .map { it to distanceCalc.apply(it.commandName.lowercase(), input.lowercase()) }
                .minByOrNull { it.second }
                ?.takeIf { it.second <= 3 }
                ?.first
        }
    }
}
