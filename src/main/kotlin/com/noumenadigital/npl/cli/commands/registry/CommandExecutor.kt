package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter

sealed interface CommandExecutor {
    val commandName: String
    val description: String

    val parameters: List<CommandParameter>
        get() = emptyList()

    fun createInstance(params: List<String>): CommandExecutor = this

    fun execute(output: ColorWriter): ExitCode
}
