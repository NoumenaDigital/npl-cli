package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudHelpCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLoginCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class CloudCommands(
    private val commandExecutorFactory: () -> CommandExecutor,
) {
    LOGIN({ CloudLoginCommand() }),
    CLOUD_HELP({ CloudHelpCommand }),
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
