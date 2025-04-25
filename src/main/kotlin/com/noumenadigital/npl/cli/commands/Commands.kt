package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CheckCommand
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.OpenapiCommand
import com.noumenadigital.npl.cli.commands.registry.TestCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class Commands(
    private val commandExecutorFactory: () -> CommandExecutor,
) {
    VERSION({ VersionCommand }),
    HELP({ HelpCommand }),
    CHECK({ CheckCommand() }),
    OPENAPI({ OpenapiCommand() }),
    TEST({ TestCommand() }),
    ;

    val commandName: String
        get() = commandExecutorFactory().commandName

    val description: String
        get() = commandExecutorFactory().description

    /**
     * Get the base executor for this command
     */
    fun getBaseExecutor(): CommandExecutor = commandExecutorFactory()

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
        ): CommandExecutor {
            val normalizedCommand = command.lowercase()
            val matchedCommand =
                entries.find { it.commandName.equals(normalizedCommand, ignoreCase = true) }
                    ?: throw CommandNotFoundException(normalizedCommand)

            val baseExecutor = matchedCommand.getBaseExecutor()
            return baseExecutor.createInstance(params)
        }
    }
}
