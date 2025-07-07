package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandParameter
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter
import kotlinx.coroutines.runBlocking

class CloudLoginCommand(
    private val authManager: CloudAuthManager = CloudAuthManager(),
) : CommandExecutor {
    override val commandName: String = "cloud login"
    override val description: String = "Handle the NPL CLI login to NOUMENA Сloud  "

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val clientId = parsedArgs.getValue("clientId")
        val clientSecret = parsedArgs.getValue("clientSecret")
        val url = parsedArgs.getValue("url")
        val noumenaCloudAuthClient =
            NoumenaCloudAuthClient(
                NoumenaCloudAuthConfig.get(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    url = url,
                ),
            )
        val authManager = CloudAuthManager(noumenaCloudAuthClient)
        return CloudLoginCommand(authManager)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            runBlocking { authManager.login(output) }
            output.success("Successfully logged in to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud login")
        }
    }
}
