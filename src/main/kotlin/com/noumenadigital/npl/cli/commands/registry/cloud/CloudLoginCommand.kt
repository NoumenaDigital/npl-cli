package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudLoginCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud login"
    override val description: String = "Handle the NPL CLI login to NOUMENA Сloud  "
    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "client-id",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
                configFilePath = YamlConfig.Cloud.clientId,
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
                configFilePath = YamlConfig.Cloud.clientSecret,
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = YamlConfig.Cloud.url,
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedUrl = parsedArguments["url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"
        return CloudLoginCommand(clientId = parsedClientId, clientSecret = parsedClientSecret, url = parsedUrl)
    }
}

class CloudLoginCommand(
    private val clientId: String,
    private val clientSecret: String,
    private val url: String,
) : CommandExecutor {
    val noumenaCloudAuthConfig =
        NoumenaCloudAuthConfig.get(
            clientId = clientId,
            clientSecret = clientSecret,
            url = url,
        )
    val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    val authManager = CloudAuthManager(noumenaCloudAuthClient)

    override fun execute(output: ColorWriter): ExitCode {
        try {
            authManager.loginBlocking(output)
            output.success("Successfully logged in to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud login")
        }
    }
}
