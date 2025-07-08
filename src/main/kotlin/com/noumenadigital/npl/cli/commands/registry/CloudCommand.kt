package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudCommandsParser
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override val commandName: String
        get() = "cloud"
    override val description: String
        get() = "Subset of commands for NOUMENA Cloud; for the full list of sub-commands, run `npl cloud help`"
    override val supportsMcp: Boolean = false

    override fun createInstance(params: List<String>): CommandExecutor = CloudCommand(args = params)

    override fun execute(output: ColorWriter): ExitCode = CloudCommandsParser.parse(args).execute(output)
}
