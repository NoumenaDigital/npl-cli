package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

sealed interface CommandExecutor {
    val arguments: List<String>
        get() = emptyList()
    val commandName: String

    fun execute(output: Writer)
}
