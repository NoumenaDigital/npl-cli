package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandsRegistry.Companion.SUB_COMMANDS_CONST
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployCommandsParser
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudDeployCommandDescriptor : CommandDescriptor {
    override val commandName: String
        get() = "cloud deploy"
    override val description: String
        get() = "Subset of commands to deploy to NOUMENA Cloud; for the full list of sub-commands, run `npl cloud deploy help`"
    override val supportsMcp: Boolean = false
    override val isParentCommand: Boolean = true

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val args = parsedArguments[SUB_COMMANDS_CONST] as? List<String> ?: emptyList()
        return CloudDeployCommand(args = args)
    }
}

class CloudDeployCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override fun execute(output: ColorWriter): ExitCode = CloudDeployCommandsParser.parse(args).execute(output)
}
