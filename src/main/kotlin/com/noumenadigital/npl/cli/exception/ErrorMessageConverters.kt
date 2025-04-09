package com.noumenadigital.npl.cli.exception

fun CommandParsingException.buildOutputMessage(): String =
    when {
        commands.isEmpty() -> "No command provided"
        else -> "Command [${commands.joinToString(" ")}] cannot be parsed"
    }

fun CommandNotFoundException.buildOutputMessage(): String =
    listOfNotNull(
        "Command not supported: '$commandName'.",
        suggestedCommand?.let { "Did you mean '$it'?" },
    ).joinToString(" ")

fun Exception.buildOutputMessage(inputArgs: List<String>): String =
    "Executing command: [${inputArgs.joinToString(" ")}] FAILED. Error: ${stackTraceToString().trim()}"
