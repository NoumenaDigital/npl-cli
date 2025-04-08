package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

class VersionCommand : NplCommandExecutor {
    companion object {
        const val COMMAND_DESCRIPTION = "Command to return current npl cli version"
    }

    override val commandName: String = "version"

    override fun execute(output: Writer) {
        output.write("I'm v1.0")
    }
}
