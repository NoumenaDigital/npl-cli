package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.model.CommandContext

data object HelloCommand : NplCommand {
    override val commandDescription = "Command to return hello string"

    override fun execute(commandContext: CommandContext) {
        commandContext.output.write("Hello")
    }
}
