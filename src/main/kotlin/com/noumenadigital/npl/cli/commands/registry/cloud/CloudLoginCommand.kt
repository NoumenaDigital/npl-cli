package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandParameter
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter
import kotlinx.coroutines.runBlocking

class CloudLoginCommand(
    private val authManager: CloudAuthManager? = null,
) : CommandExecutor {
    override val commandName: String = "cloud login"
    override val description: String = "Login to Noumena Cloud"

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "--clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "--clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "--url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val clientId = parsedArgs.getValue("--clientId") ?: "paas"
        val clientSecret = parsedArgs.getValue("--clientSecret") ?: "paas"
        val url = parsedArgs.getValue("--url") ?: "https://keycloak.noumena.cloud/realms/paas"
        val noumenaCloudClient =
            NoumenaCloudClient(
                NoumenaCloudConfig(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    url = url,
                ),
            )
        val authManager = CloudAuthManager(noumenaCloudClient)
        return CloudLoginCommand(authManager)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            runBlocking { authManager?.login(output) }
            output.success("Successfully logged in to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud login")
        }
    }
}
