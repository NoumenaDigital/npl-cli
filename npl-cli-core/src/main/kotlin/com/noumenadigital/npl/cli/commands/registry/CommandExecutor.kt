package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import java.io.Writer

data class CommandParameter(
    val name: String,
    val description: String,
    val defaultValue: String? = null,
    val isRequired: Boolean = false,
)

sealed interface CommandExecutor {
    val commandName: String
    val description: String

    val parameters: List<CommandParameter>
        get() = emptyList()

    fun createInstance(params: List<String>): CommandExecutor = this

    fun execute(output: Writer): ExitCode
}
