package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

sealed interface NplCommand {
    val commandName: String

    fun execute(output: Writer)
}

class GenericCommand(
    val commandDescription: String? = null,
    val command: (List<String>?) -> NplCommand,
)
