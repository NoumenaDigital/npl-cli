package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandsRegistry.Companion.SUB_COMMANDS_CONST
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudCommandsParser
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud"
    override val description: String = "Subset of commands for NOUMENA Cloud; for the full list of sub-commands, run `npl cloud help`"
    override val supportsMcp: Boolean = false
    override val isParentCommand: Boolean = true

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val args = parsedArguments[SUB_COMMANDS_CONST] as? List<String> ?: emptyList()
        return CloudCommand(args = args)
    }
}

class CloudCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override fun execute(output: ColorWriter): ExitCode = CloudCommandsParser.parse(args).execute(output)
}
