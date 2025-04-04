package com.noumenadigital.npl.cli.exception

class CommandParsingException(
    s: String,
) : RuntimeException(s)

class CommandNotFoundException(
    s: String,
) : RuntimeException(s)
