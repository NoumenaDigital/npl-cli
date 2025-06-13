package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudLogoutCommand(
    val cloudAuthManager: CloudAuthManager = CloudAuthManager(),
) : CommandExecutor {
    override val commandName: String = "cloud logout"
    override val description: String = "Logout from Noumena Cloud"

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
