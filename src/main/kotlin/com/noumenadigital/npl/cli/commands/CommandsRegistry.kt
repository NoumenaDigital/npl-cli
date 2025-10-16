package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import com.noumenadigital.npl.cli.settings.DefaultSettingsProvider

interface CommandsRegistry {
    val commandDescriptor: () -> CommandDescriptor

    val commandName: String
        get() = commandDescriptor().commandName

    val description: String
        get() = commandDescriptor().description

    /**
     * Get the base executor for this command
     */
    fun getCommandDescriptor(): CommandDescriptor = commandDescriptor()

    companion object {
        const val SUB_COMMANDS_CONST = "sub-commands"

        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
            toTypedArray: Array<CommandsRegistry>,
        ): CommandExecutor {
            val normalizedCommand = command.lowercase()
            val matchedCommand =
                toTypedArray.find { it.commandName.equals(normalizedCommand, ignoreCase = true) }
                    ?: throw CommandNotFoundException(normalizedCommand)

            val commandDescriptor = matchedCommand.getCommandDescriptor()
            if (commandDescriptor.isParentCommand) {
                return commandDescriptor.createCommandExecutorInstance(mapOf(SUB_COMMANDS_CONST to params))
            }

            val settingsProvider = DefaultSettingsProvider(params, commandDescriptor)
            val parsedArguments = settingsProvider.getParsedCommandArgumentsWithBasicValidation()
            return commandDescriptor.createCommandExecutorInstance(parsedArguments)
        }
    }
}
