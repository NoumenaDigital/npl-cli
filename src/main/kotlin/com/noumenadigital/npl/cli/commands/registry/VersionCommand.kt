package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

data object VersionCommand : CommandExecutor {
    const val COMMAND_DESCRIPTION = "Display the current version of the NPL CLI"

    override val commandName: String = "version"

    // TODO: ST-4556
    override fun execute(output: Writer) {
        output.write("I'm v1.0")
    }
}
