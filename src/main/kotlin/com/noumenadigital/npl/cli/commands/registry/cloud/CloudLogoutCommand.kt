package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudLogoutCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud logout"
    override val description: String = "Handle the NPL CLI logout to NOUMENA Сloud  "

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor = CloudLogoutCommand()
}

class CloudLogoutCommand(
    val cloudAuthManager: CloudAuthManager = CloudAuthManager(),
) : CommandExecutor {
    override fun execute(output: ColorWriter): ExitCode {
        try {
            cloudAuthManager.logout()
            output.success("Successfully logged out of NOUMENA Cloud Cli Tool.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud logout")
        }
    }
}
