package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.commands.CloudDeployCommands
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor

object CloudDeployCommandsParser {
    fun parse(command: List<String>): CommandExecutor {
        val commandName =
            command.firstOrNull() ?: return CloudDeployCommands.CLOUD_DEPLOY_HELP.getBaseExecutor().createInstance(emptyList())

        val arguments = if (command.size > 1) command.drop(1) else emptyList()

        return CloudDeployCommands.Companion.commandFromString(commandName, arguments)
    }
}
