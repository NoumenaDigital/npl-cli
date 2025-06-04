package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor

interface CommandsRegistry {
    val commandExecutorFactory: () -> CommandExecutor

    val commandName: String
        get() = commandExecutorFactory().commandName

    val description: String
        get() = commandExecutorFactory().description

    /**
     * Get the base executor for this command
     */
    fun getBaseExecutor(): CommandExecutor = commandExecutorFactory()
}
