package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

data object VersionCommand : NplCommand {
    override val commandDescription = "Command to return current npl cli version"

    override fun execute(output: Writer) {
        output.write("I'm v1.0")
    }
}
