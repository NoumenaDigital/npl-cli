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

object CloudCreateCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud create"
    override val description: String = "Create a new application in NOUMENA Cloud"
    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "tenant",
                description = "Tenant slug where the application will be created",
                isRequired = true,
                valuePlaceholder = "<tenant-slug>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "app-name",
                description = "Name for the new application",
                isRequired = true,
                valuePlaceholder = "<app-name>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "provider",
                description = "Cloud provider (default: MicrosoftAzure)",
                isRequired = false,
                valuePlaceholder = "<provider>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "engine-version",
                description = "Engine version (default: latest available)",
                isRequired = false,
                valuePlaceholder = "<version>",
                configFilePath = null,
            ),
            NamedParameter(
                name = "trusted-issuers",
                description = "Comma-separated list of trusted issuer URLs",
                isRequired = false,
                valuePlaceholder = "<issuer-urls>",
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
        val appName =
            parsedArguments["app-name"] as? String
                ?: throw IllegalArgumentException("--app-name is required")
        val provider = parsedArguments["provider"] as? String ?: "MicrosoftAzure"
        val engineVersion = parsedArguments["engine-version"] as? String
        val trustedIssuers = parsedArguments["trusted-issuers"] as? String
        val parsedUrl = parsedArguments["url"] as? String ?: "https://portal.noumena.cloud"
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedAuthUrl = parsedArguments["auth-url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"

        return CloudCreateCommand(
            tenant = tenant,
            appName = appName,
            provider = provider,
            engineVersion = engineVersion,
            trustedIssuers = trustedIssuers,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
    }
}

class CloudCreateCommand(
    private val tenant: String,
    private val appName: String,
    private val provider: String,
    private val engineVersion: String?,
    private val trustedIssuers: String?,
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
            val result =
                cloudApplicationService.createApplication(
                    output = output,
                    actionName = "create application",
                    tenant = tenant,
                    appName = appName,
                    provider = provider,
                    engineVersion = engineVersion,
                    trustedIssuers = trustedIssuers,
                )

            output.success("Application created successfully!")
            output.info("  Name: ${result.application.name}")
            output.info("  Slug: ${result.application.slug}")
            output.info("  ID: ${result.application.id}")
            output.info("  Engine Version: ${result.engineVersion}")
            output.info("  Provider: ${result.provider}")
            output.info("  Trusted Issuers: ${result.trustedIssuers.joinToString(", ")}")

            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud create")
        }
    }
}
