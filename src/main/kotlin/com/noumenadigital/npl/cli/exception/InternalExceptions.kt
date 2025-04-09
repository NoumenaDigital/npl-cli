package com.noumenadigital.npl.cli.exception

sealed class InternalException : RuntimeException()

class CommandParsingException(
    val commands: List<String> = emptyList(),
) : InternalException()

class CommandNotFoundException(
    val commandName: String,
    val suggestedCommand: String? = null,
) : InternalException()
