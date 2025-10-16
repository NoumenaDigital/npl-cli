package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.CloudDeployService
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudClearNplCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud clear"
    override val description: String = "Delete NPL sources and clears protocols from the database from the NOUMENA Cloud Application"
    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "app",
                description = "NOUMENA Cloud Application slug",
                isRequired = true,
                valuePlaceholder = "<app>",
                configFilePath = "/cloud/app",
            ),
            NamedParameter(
                name = "tenant",
                description = "NOUMENA Cloud Tenant slug",
                isRequired = true,
                valuePlaceholder = "<tenant>",
                configFilePath = "/cloud/tenant",
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud deployment URL",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = "/cloud/url",
            ),
            NamedParameter(
                name = "client-id",
                description = "OAuth2 Client ID",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<client-id>",
                configFilePath = "/cloud/clientId",
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<client-secret>",
                configFilePath = "/cloud/clientSecret",
            ),
            NamedParameter(
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<auth-url>",
                configFilePath = "/cloud/authUrl",
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedApp = parsedArguments["app"] as String
        val parsedTenant = parsedArguments["tenant"] as String
        val parsedUrl = parsedArguments["url"] as String
        val parsedClientId = parsedArguments["client-id"] as String
        val parsedClientSecret = parsedArguments["client-secret"] as String
        val parsedAuthUrl = parsedArguments["auth-url"] as String
        return CloudClearNplCommand(
            app = parsedApp,
            tenant = parsedTenant,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
    }
}

class CloudClearNplCommand(
    private val app: String,
    private val tenant: String,
    private val url: String,
    private val clientId: String,
    private val clientSecret: String,
    private val authUrl: String,
) : CommandExecutor {
    val noumenaCloudAuthConfig =
        NoumenaCloudAuthConfig.get(
            clientId = clientId,
            clientSecret = clientSecret,
            url = authUrl,
        )
    val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
    val noumenaCloudClient =
        NoumenaCloudClient(
            NoumenaCloudConfig.get(appSlug = app, tenantSlug = tenant, url = url),
        )
    val cloudDeployService =
        CloudDeployService(
            cloudAuthManager,
            noumenaCloudClient,
        )

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
