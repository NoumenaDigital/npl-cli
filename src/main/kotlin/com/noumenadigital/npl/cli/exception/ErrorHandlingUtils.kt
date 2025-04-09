package com.noumenadigital.npl.cli.exception

fun buildCommandNotFoundErrorMessage(
    commandName: String,
    suggestedCommandName: String?,
): String =
    listOfNotNull(
        "Command not supported: '$commandName'.",
        suggestedCommandName?.let { "Did you mean '$it'?" },
    ).joinToString(" ")

fun buildCommandParsingErrorMessage(commands: List<String>): String = "Command [${commands.joinToString(" ")}] cannot be parsed"

fun buildGenericErrorMessage(
    commands: List<String>,
    stackTrace: String,
): String = "Executing command: [${commands.joinToString(" ")}] FAILED. Error: ${stackTrace.trim()}"
