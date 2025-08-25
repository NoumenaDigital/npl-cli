package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudClearNplCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudDeployCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudHelpCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLoginCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLogoutCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudStatusCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class CloudCommands(
    override val commandExecutorFactory: () -> CommandExecutor,
) : CommandsRegistry {
    CLOUD_LOGIN({ CloudLoginCommand() }),
    CLOUD_LOGOUT({ CloudLogoutCommand() }),
    CLOUD_HELP({ CloudHelpCommand }),
    CLOUD_CLEAR_NPL({ CloudClearNplCommand() }),
    CLOUD_DEPLOY({ CloudDeployCommand() }),
    CLOUD_STATUS({ CloudStatusCommand() }),
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
