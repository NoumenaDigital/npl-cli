package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.commands.registry.HelpCommand

data object CloudHelpCommand : HelpCommand() {
    override val commandName: String = "cloud help"
    override val description: String = "Display the description for npl-cli cloud commands"
}
