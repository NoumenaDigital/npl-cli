package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.ArgumentParser
import com.noumenadigital.npl.cli.commands.CommandConfig
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.RequiredParameterMissing
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.CloudDeployService
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudClearNplCommand(
    val cloudDeployService: CloudDeployService =
        CloudDeployService(
            CloudAuthManager(),
            NoumenaCloudClient(NoumenaCloudConfig()),
        ),
) : CommandExecutor {
    override val commandName: String = "cloud clear"
    override val description: String = "Delete NPL sources and clears protocols from the database from the NOUMENA Cloud Application"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "app",
                description = "NOUMENA Cloud Application slug",
                isRequired = true,
                valuePlaceholder = "<app>",
            ),
            NamedParameter(
                name = "tenant",
                description = "NOUMENA Cloud Tenant slug",
                isRequired = true,
                valuePlaceholder = "<tenant>",
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud deployment URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
            ),
            NamedParameter(
                name = "client-id",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-id>",
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-secret>",
            ),
            NamedParameter(
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<auth-url>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val config =
            ArgumentParser.parse(params, parameters) { settings ->
                CloudClearNplConfig(
                    app = settings.cloud.app ?: throw RequiredParameterMissing("app"),
                    tenant = settings.cloud.tenant ?: throw RequiredParameterMissing("tenant"),
                    url = settings.cloud.url,
                    clientId = settings.local.clientId,
                    clientSecret = settings.local.clientSecret,
                    authUrl = settings.cloud.authUrl,
                )
            }

        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(config.clientId, config.clientSecret, config.authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudDeployService =
            CloudDeployService(
                CloudAuthManager(noumenaCloudAuthClient),
                NoumenaCloudClient(NoumenaCloudConfig.get(config.app, config.tenant, config.url)),
            )
        return CloudClearNplCommand(cloudDeployService = cloudDeployService)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            cloudDeployService.clearApp()
            output.success("NPL sources successfully cleared from NOUMENA Cloud app.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud clear")
        }
    }
}

data class CloudClearNplConfig(
    val app: String,
    val tenant: String,
    val url: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val authUrl: String? = null,
) : CommandConfig
