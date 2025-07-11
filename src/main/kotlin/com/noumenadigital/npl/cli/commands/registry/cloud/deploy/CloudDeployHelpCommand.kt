package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudDeployCommands
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.service.ColorWriter

data object CloudDeployHelpCommand : HelpCommand() {
    override val commandName: String = "cloud deploy help"
    override val description: String = "Display the description for npl-cli cloud deploy commands"
    override val supportsMcp: Boolean = false

    override fun execute(output: ColorWriter): ExitCode {
        printHelp(CloudDeployCommands.entries.map { it.commandExecutorFactory() }, output)
        return ExitCode.SUCCESS
    }
}
