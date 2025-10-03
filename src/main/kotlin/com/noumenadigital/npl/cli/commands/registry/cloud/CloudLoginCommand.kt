package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandConfig
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.settings.DefaultSettingsProvider

class CloudLoginCommand(
    private val authManager: CloudAuthManager = CloudAuthManager(),
) : CommandExecutor {
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
            ),
            NamedParameter(
                name = "client-secret",
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
        val settings = DefaultSettingsProvider(params, parameters)
        val local = settings.local
        val cloud = settings.cloud
        val config =
            CloudLoginConfig(
                clientId = local.clientId,
                clientSecret = local.clientSecret,
                url = cloud.url,
            )

        val noumenaCloudAuthClient =
            NoumenaCloudAuthClient(
                NoumenaCloudAuthConfig.get(
                    clientId = config.clientId,
                    clientSecret = config.clientSecret,
                    url = config.url,
                ),
            )
        val authManager = CloudAuthManager(noumenaCloudAuthClient)
        return CloudLoginCommand(authManager)
    }

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

data class CloudLoginConfig(
    val clientId: String? = null,
    val clientSecret: String? = null,
    val url: String? = null,
) : CommandConfig
