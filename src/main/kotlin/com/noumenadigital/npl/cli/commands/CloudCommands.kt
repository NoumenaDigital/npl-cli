package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudDeployNplCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudHelpCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLoginCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class CloudCommands(
    override val commandExecutorFactory: () -> CommandExecutor,
) : CommandsRegistry {
    LOGIN({ CloudLoginCommand() }),
    CLOUD_HELP({ CloudHelpCommand }),
    CLOUD_DEPLOY_NPL({ CloudDeployNplCommand() }),
    ;

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
        ): CommandExecutor {
            val normalizedCommand = "cloud ${command.lowercase()}"
            val matchedCommand =
                CloudCommands.entries.find { it.commandName.equals(normalizedCommand, ignoreCase = true) }
                    ?: throw CommandNotFoundException(normalizedCommand)

            val baseExecutor = matchedCommand.getBaseExecutor()
            return baseExecutor.createInstance(params)
        }
    }
}
