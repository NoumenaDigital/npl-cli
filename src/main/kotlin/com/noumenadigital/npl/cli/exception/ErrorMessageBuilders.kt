package com.noumenadigital.npl.cli.exception

import com.noumenadigital.npl.cli.commands.Commands
import org.apache.commons.text.similarity.LevenshteinDistance

fun CommandParsingException.buildOutputMessage(): String =
    when {
        commands.isEmpty() -> "No command provided"
        else -> "Command [${commands.joinToString(" ")}] cannot be parsed"
    }

fun CommandNotFoundException.buildOutputMessage(): String {
    fun suggestClosestCommand(input: String): String? {
        val distanceCalc = LevenshteinDistance()
        return Commands.entries
            .map { it.commandName to distanceCalc.apply(it.commandName, input) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= 3 }
            ?.first
    }
    return listOfNotNull(
        "Command not supported: '$commandName'.",
        suggestClosestCommand(commandName)?.let { "Did you mean '$it'?" },
    ).joinToString(" ")
}

fun Exception.buildOutputMessage(inputArgs: List<String>): String =
    "Executing command: [${inputArgs.joinToString(" ")}] FAILED. Error: ${stackTraceToString().trim()}"
