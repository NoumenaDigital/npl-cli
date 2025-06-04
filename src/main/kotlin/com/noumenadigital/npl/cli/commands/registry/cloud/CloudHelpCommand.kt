package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudCommands
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.service.ColorWriter

data object CloudHelpCommand : HelpCommand() {
    override val commandName: String = "help"
    override val description: String = "Display the description for npl-cli cloud commands"

    override fun execute(output: ColorWriter): ExitCode {
        printHelp(CloudCommands.entries.map { it.commandExecutorFactory() }, output)
        return ExitCode.SUCCESS
    }
}
