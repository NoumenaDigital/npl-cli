package com.noumenadigital.npl.cli.exception

open class InternalException(
    message: String,
) : RuntimeException(message)

class CommandParsingException(
    message: String,
) : InternalException(message)

class CommandNotFoundException(
    message: String,
) : InternalException(message)
