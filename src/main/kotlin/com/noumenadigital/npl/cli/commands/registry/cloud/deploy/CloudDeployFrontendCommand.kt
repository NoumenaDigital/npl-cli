package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.ExitCode
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
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.settings.DefaultSettingsProvider
import java.io.File

class CloudDeployFrontendCommand(
    private val sourcesManager: SourcesManager = SourcesManager("."),
    private val cloudDeployService: CloudDeployService =
        CloudDeployService(
            CloudAuthManager(),
            NoumenaCloudClient(NoumenaCloudConfig()),
        ),
) : CommandExecutor {
    override val commandName: String = "cloud deploy frontend"
    override val description: String = "Deploy frontend sources to a NOUMENA Cloud Application"

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
                name = "frontend",
                description = "Path to the frontend source directory containing the index.html file",
                isRequired = true,
                valuePlaceholder = "<frontend>",
                takesPath = true,
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
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<authUrl>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val settings = DefaultSettingsProvider(params, parameters)
        val cloud = settings.cloud
        val local = settings.local
        val structure = settings.structure
        val config =
            CloudDeployFrontendConfig(
                app = cloud.app ?: throw RequiredParameterMissing("app"),
                tenant = cloud.tenant ?: throw RequiredParameterMissing("tenant"),
                frontend = structure.frontEnd ?: throw RequiredParameterMissing("frontend"),
                url = cloud.url,
                clientId = local.clientId,
                clientSecret = local.clientSecret,
                authUrl = cloud.authUrl,
            )

        if (!config.frontend.exists() || !config.frontend.isDirectory) {
            throw CloudCommandException(
                message = "Build directory does not exist or is not a directory - ${config.frontend}",
                commandName = "cloud deploy frontend",
            )
        }
        val sourcesManager = SourcesManager(config.frontend.absolutePath)
        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(config.clientId, config.clientSecret, config.authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudDeployService =
            CloudDeployService(
                CloudAuthManager(noumenaCloudAuthClient),
                NoumenaCloudClient(NoumenaCloudConfig.get(config.app, config.tenant, config.url)),
            )
        return CloudDeployFrontendCommand(sourcesManager, cloudDeployService)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val saClientId = cloudDeployService.noumenaCloudClient.config.tenantSlug
            val saClientSecret =
                System.getenv("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
                    ?: System.getProperty("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
            val archive = sourcesManager.getArchivedSources()

            if (!saClientSecret.isNullOrBlank()) {
                output.info("Preparing to deploy frontend to NOUMENA Cloud using service account...")
                val accessToken =
                    cloudDeployService.cloudAuthManager.getServiceAccountAccessToken(saClientId, saClientSecret)
                output.success("Successfully authenticated with service account credentials")
                cloudDeployService.deployFrontendWithToken(archive, accessToken)
            } else {
                cloudDeployService.deployFrontend(archive)
            }

            output.success("Frontend successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud deploy frontend")
        }
    }
}

data class CloudDeployFrontendConfig(
    val app: String,
    val tenant: String,
    val frontend: File,
    val url: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val authUrl: String? = null,
) : CommandConfig
