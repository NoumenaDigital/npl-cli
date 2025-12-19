package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudApplicationService
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudDeleteCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud delete"
    override val description: String = "Delete an application from NOUMENA Cloud"
    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "tenant",
                description = "Tenant slug where the application is located",
                isRequired = true,
                valuePlaceholder = "<tenant-slug>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "app",
                description = "Application slug to delete",
                isRequired = true,
                valuePlaceholder = "<app-slug>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud deployment URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = YamlConfig.Cloud.url,
            ),
            NamedParameter(
                name = "client-id",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-id>",
                configFilePath = YamlConfig.Cloud.clientId,
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-secret>",
                configFilePath = YamlConfig.Cloud.clientSecret,
            ),
            NamedParameter(
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<auth-url>",
                configFilePath = YamlConfig.Cloud.authUrl,
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val tenant =
            parsedArguments["tenant"] as? String
                ?: throw IllegalArgumentException("--tenant is required")
        val app =
            parsedArguments["app"] as? String
                ?: throw IllegalArgumentException("--app is required")
        val parsedUrl = parsedArguments["url"] as? String ?: "https://portal.noumena.cloud"
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedAuthUrl = parsedArguments["auth-url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"

        return CloudDeleteCommand(
            tenant = tenant,
            app = app,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
    }
}

class CloudDeleteCommand(
    private val tenant: String,
    private val app: String,
    private val url: String,
    private val clientId: String,
    private val clientSecret: String,
    authUrl: String,
) : CommandExecutor {
    private val noumenaCloudAuthConfig =
        NoumenaCloudAuthConfig.get(
            clientId = clientId,
            clientSecret = clientSecret,
            url = authUrl,
        )
    private val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    private val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
    private val noumenaCloudClient = NoumenaCloudClient(NoumenaCloudConfig(url = url))
    private val cloudApplicationService = CloudApplicationService(cloudAuthManager, noumenaCloudClient)

    override fun execute(output: ColorWriter): ExitCode {
        try {
            cloudApplicationService.deleteApplication(
                output = output,
                actionName = "delete application",
                tenant = tenant,
                appSlug = app,
            )

            output.success("Application '$app' deleted successfully from tenant '$tenant'!")

            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud delete")
        }
    }
}
