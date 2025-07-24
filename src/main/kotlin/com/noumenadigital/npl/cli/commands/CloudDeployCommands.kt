package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployFrontendCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployHelpCommand
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployNplCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class CloudDeployCommands(
    override val commandExecutorFactory: () -> CommandExecutor,
) : CommandsRegistry {
    CLOUD_DEPLOY_HELP({ CloudDeployHelpCommand }),
    CLOUD_DEPLOY_NPL({ CloudDeployNplCommand() }),
    CLOUD_DEPLOY_FRONTEND({ CloudDeployFrontendCommand() }),
    ;

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
        ): CommandExecutor {
            val normalizedCommand = "cloud deploy ${command.lowercase()}"
            val matchedCommand =
                CloudDeployCommands.entries.find { it.commandName.equals(normalizedCommand, ignoreCase = true) }
                    ?: throw CommandNotFoundException(normalizedCommand)

            val baseExecutor = matchedCommand.getBaseExecutor()
            return baseExecutor.createInstance(params)
        }
    }
}
