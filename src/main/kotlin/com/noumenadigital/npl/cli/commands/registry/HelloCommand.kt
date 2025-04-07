package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

data object HelloCommand : NplCommand {
    override val commandDescription = "Command to return hello string"

    override fun execute(output: Writer) {
        output.write("Hello")
    }
}
