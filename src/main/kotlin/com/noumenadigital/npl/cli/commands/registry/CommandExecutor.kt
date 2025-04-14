package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import java.io.Writer

sealed interface CommandExecutor {
    @Suppress("unused")
    val arguments: List<String>
        get() = emptyList()
    val commandName: String

    fun execute(output: Writer): ExitCode
}
