package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

data object CheckCommand : CommandExecutor {
    const val COMMAND_DESCRIPTION = "Validate the correctness of the NPL sources (excluding test sources) in the current directory."

    override val commandName: String = "check"

    override fun execute(output: Writer) {
    }
}
