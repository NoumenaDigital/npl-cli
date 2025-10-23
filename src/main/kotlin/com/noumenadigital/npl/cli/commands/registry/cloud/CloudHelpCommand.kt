package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudCommands
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudHelpCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud help"
    override val description: String = "Display the description for npl-cli cloud commands"
    override val supportsMcp: Boolean = false

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor = CloudHelpCommand
}

data object CloudHelpCommand : HelpCommand() {
    override fun execute(output: ColorWriter): ExitCode {
        printHelp(CloudCommands.entries.map { it.commandDescriptor() }, output)
        return ExitCode.SUCCESS
    }
}
