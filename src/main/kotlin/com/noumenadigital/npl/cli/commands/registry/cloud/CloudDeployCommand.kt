package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudDeployCommandsParser
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudDeployCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override val commandName: String
        get() = "cloud deploy"
    override val description: String
        get() = "Subset of commands to deploy to NOUMENA Cloud; for the full list of sub-commands, run `npl cloud deploy help`"
    override val supportsMcp: Boolean = false

    override fun createInstance(params: List<String>): CommandExecutor = CloudDeployCommand(args = params)

    override fun execute(output: ColorWriter): ExitCode = CloudDeployCommandsParser.parse(args).execute(output)
}
