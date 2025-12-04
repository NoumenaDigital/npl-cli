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

fun CommandValidationException.buildOutputMessage(): String = this.message

fun DeployConfigException.buildOutputMessage(): String {
    val errorLines =
        this.message
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") { "  $it" }

    return "Configuration errors:\n$errorLines"
}

fun ClientSetupException.buildOutputMessage(): String =
    if (this.isConnectionError) {
        this.message
    } else {
        "Client setup error: ${this.message}"
    }

fun ArgumentParsingException.buildOutputMessage(): String = this.message

fun AuthorizationFailedException.buildOutputMessage(): String = this.message

fun CloudCommandException.buildOutputMessage(): String = "Command ${this.commandName} failed: ${this.message}"

fun RequiredParameterMissing.buildOutputMessage(): String {
    val params = parameterNames.joinToString(", ")

    val yamlExamplesFormatted =
        yamlExamples
            .filterNotNull()
            .joinToString("\n") { "  $it" }

    val usagePart = usageInstruction?.let { "\nUsage:\n  $it\n" } ?: ""

    return """
        |Missing required parameter(s): $params
        |
        |You can provide them in one of the following ways:
        |
        |  • As command-line arguments:
        |${parameterNames.joinToString("\n") { "      --$it <value>" }}
        |
        |  • In your npl.yml configuration file:
        |
        |$yamlExamplesFormatted
        |$usagePart
        """.trimMargin()
}
