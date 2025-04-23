package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter

data object VersionCommand : CommandExecutor {
    override val commandName: String = "version"
    override val description: String = "Display the current version of the NPL CLI"

    // TODO: ST-4556
    override fun execute(output: ColorWriter): ExitCode {
        output.success("I'm v1.0")
        return ExitCode.SUCCESS
    }
}
