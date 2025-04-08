package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

sealed interface NplCommandExecutor {
    val commandName: String

    fun execute(output: Writer)
}

class GenericCommand(
    val description: String? = null,
    val executor: (List<String>?) -> NplCommandExecutor,
)
