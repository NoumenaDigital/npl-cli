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

class CloudLoginCommand(
    private val authManager: CloudAuthManager? = null,
) : CommandExecutor {
    override val commandName: String = "login"
    override val description: String = "Login to Noumena Cloud"

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "--clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "--clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "--serverUrl",
                description = "Noumena Cloud Auth URL",
                isRequired = false,
                valuePlaceholder = "<serverUrl>",
            ),
            NamedParameter(
                name = "--realm",
                description = "Noumena Cloud realm",
                isRequired = false,
                valuePlaceholder = "<realm>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val clientId = parsedArgs.getValue("--clientId") ?: "paas"
        val clientSecret = parsedArgs.getValue("--clientSecret") ?: "paas"
        val baseUrl = parsedArgs.getValue("--baseUrl") ?: "https://keycloak.noumena.cloud"
        val realm = parsedArgs.getValue("--realm") ?: "paas"
        val noumenaCloudClient =
            NoumenaCloudClient(
                NoumenaCloudConfig(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    baseUrl = baseUrl,
                    realm = realm,
                ),
            )
        val authManager = CloudAuthManager(noumenaCloudClient)
        return CloudLoginCommand(authManager)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            authManager?.login(output)
            output.success("Successfully logged in to Noumena Cloud. Access token stored.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud login")
        }
    }

    private fun displayUsage(writer: ColorWriter) {
        writer.info(
            """
            Usage: cloud login --clientId <clientId> --clientSecret <clientSecret> --url <url>
            """.trimIndent(),
        )
    }
}
