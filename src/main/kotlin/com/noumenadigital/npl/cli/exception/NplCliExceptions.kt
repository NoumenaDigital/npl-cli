package com.noumenadigital.npl.cli.exception

open class NplCliException(
    msg: String,
) : RuntimeException(msg)

class CommandParsingException(
    s: String,
) : NplCliException("Invalid command line input. $s")

class CommandNotFoundException(
    s: String,
) : NplCliException(s)
