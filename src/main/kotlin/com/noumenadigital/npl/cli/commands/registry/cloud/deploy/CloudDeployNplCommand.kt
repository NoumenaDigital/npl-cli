package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.EnvironmentVariable
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CommandValidationException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.CloudDeployService
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.SourcesManager
import java.io.File

object CloudDeployNplCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud deploy npl"
    override val description: String = "Deploy NPL sources to a NOUMENA Cloud Application"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "app",
                description = "NOUMENA Cloud Application slug",
                isRequired = true,
                valuePlaceholder = "<app>",
                configFilePath = YamlConfig.Cloud.app,
            ),
            NamedParameter(
                name = "tenant",
                description = "NOUMENA Cloud Tenant slug",
                isRequired = true,
                valuePlaceholder = "<tenant>",
                configFilePath = YamlConfig.Cloud.tenant,
            ),
            NamedParameter(
                name = "migration",
                description = "Path to migration.yml",
                isRequired = true,
                valuePlaceholder = "<migration>",
                takesPath = true,
                configFilePath = YamlConfig.Structure.migration,
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
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<auth-url>",
                configFilePath = YamlConfig.Cloud.authUrl,
            ),
        )

    override val envVariables: List<EnvironmentVariable> =
        listOf(
            EnvironmentVariable(
                name = "NPL_SERVICE_ACCOUNT_CLIENT_SECRET",
                description = "Client secret for the service account to use for authentication",
                isRequired = false,
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedApp = parsedArguments["app"] as String
        val parsedTenant = parsedArguments["tenant"] as String
        val parsedUrl = parsedArguments["url"] as? String ?: "https://portal.noumena.cloud"
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedMigration = parsedArguments["migration"] as? String ?: "."
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedAuthUrl = parsedArguments["auth-url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"
        return CloudDeployNplCommand(
            app = parsedApp,
            tenant = parsedTenant,
            migration = parsedMigration,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
    }
}

class CloudDeployNplCommand(
    private val app: String,
    private val tenant: String,
    private val migration: String,
    private val url: String,
    private val clientId: String,
    private val clientSecret: String,
    private val authUrl: String,
) : CommandExecutor {
    init {
        val file = File(migration)
        if (!file.exists()) {
            throw CommandValidationException(
                message = "Command cloud deploy npl failed: Migration file does not exist - $file",
            )
        }
    }

    val sourcesManager = SourcesManager(migration)
    val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(clientId, clientSecret, authUrl)
    val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    val cloudDeployService =
        CloudDeployService(
            CloudAuthManager(noumenaCloudAuthClient),
            NoumenaCloudClient(
                config =
                    NoumenaCloudConfig.get(
                        appSlug = app,
                        tenantSlug =
                        tenant,
                        url = url,
                    ),
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val saClientId = cloudDeployService.noumenaCloudClient.config.tenantSlug
            val saClientSecret =
                System.getenv("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
                    ?: System.getProperty("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
            val archive = sourcesManager.getArchivedSources()

            if (!saClientSecret.isNullOrBlank()) {
                output.info("Preparing to deploy NPL application to NOUMENA Cloud using service account...")
                val accessToken =
                    cloudDeployService.cloudAuthManager.getServiceAccountAccessToken(saClientId, saClientSecret)
                output.success("Successfully authenticated with service account credentials")
                cloudDeployService.deployNplApplicationWithToken(archive, accessToken)
            } else {
                cloudDeployService.deployNplApplication(archive)
            }

            output.success("NPL Application successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud deploy npl")
        }
    }
}
