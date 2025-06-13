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

    val helpText =
        """

        Please create or check the configuration file at .npl/deploy.yml
        (in the current directory or your home directory ~/.npl/deploy.yml)
        with the following format:

        schemaVersion: v1
        targets:
          <your-target-name>:
            type: engine
            engineManagementUrl: <URL of the Noumena Engine API>
            authUrl: <URL of the authentication endpoint>
            username: <username for authentication>
            password: <password for authentication>
            clientId: <client ID for authentication>
            clientSecret: <client secret for authentication>
        """.trimIndent()

    return "Configuration errors:\n$errorLines\n$helpText"
}

fun ClientSetupException.buildOutputMessage(): String = "Client setup error: ${this.message}"

fun AuthorizationFailedException.buildOutputMessage(): String = this.message

fun CloudCommandException.buildOutputMessage(): String = "Command ${this.commandName} failed: ${this.message}"

fun RequiredParameterMissing.buildOutputMessage(): String = "Command parsing failed: required parameter ${this.parameterName} is missing"
