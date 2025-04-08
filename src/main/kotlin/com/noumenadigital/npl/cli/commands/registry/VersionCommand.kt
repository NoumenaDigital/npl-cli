package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.model.CommandContext

data object VersionCommand : NplCommand {
    override val commandDescription = "Command to return current npl cli version"

    override fun execute(commandContext: CommandContext) {
        commandContext.output.write("I'm v1.0")
    }
}
