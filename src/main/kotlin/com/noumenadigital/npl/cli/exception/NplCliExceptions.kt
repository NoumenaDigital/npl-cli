package com.noumenadigital.npl.cli.exception

open class NplCliException(
    msg: String,
) : RuntimeException(msg)

class CommandParsingException(
    s: String,
) : NplCliException(s)

class CommandNotFoundException(
    s: String,
) : NplCliException(s)
