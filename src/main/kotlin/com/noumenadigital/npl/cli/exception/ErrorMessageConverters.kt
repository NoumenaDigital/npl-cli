package com.noumenadigital.npl.cli.exception

fun CommandParsingException.errorMessage(): String = "Command [${commands.joinToString(" ")}] cannot be parsed"

fun CommandNotFoundException.errorMessage(): String =
    listOfNotNull(
        "Command not supported: '$commandName'.",
        suggestedCommand?.let { "Did you mean '$it'?" },
    ).joinToString(" ")

fun Exception.genericErrorMessage(inputArgs: List<String>): String =
    "Executing command: [${inputArgs.joinToString(" ")}] FAILED. Error: ${stackTraceToString().trim()}"
