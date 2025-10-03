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

fun DeployConfigException.buildOutputMessage(): String {
    val errorLines =
        this.message
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") { "  $it" }

    return "Configuration errors:\n$errorLines"
}

fun ClientSetupException.buildOutputMessage(): String = "Client setup error: ${this.message}"

fun ArgumentParsingException.buildOutputMessage(): String = this.message

fun AuthorizationFailedException.buildOutputMessage(): String = this.message

fun CloudCommandException.buildOutputMessage(): String = "Command ${this.commandName} failed: ${this.message}"

fun RequiredParameterMissing.buildOutputMessage(): String = "Command parsing failed: required parameter ${this.parameterName} is missing"
