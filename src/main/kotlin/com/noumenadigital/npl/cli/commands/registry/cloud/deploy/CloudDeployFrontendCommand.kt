package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

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
import com.noumenadigital.npl.cli.service.SourcesManager
import java.io.File

object CloudDeployFrontendCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud deploy frontend"
    override val description: String = "Deploy frontend sources to a NOUMENA Cloud Application"

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
                name = "frontend",
                description = "Path to the frontend source directory containing the index.html file",
                isRequired = false,
                valuePlaceholder = "<frontend>",
                takesPath = true,
                configFilePath = "/cloud/frontend",
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
                valuePlaceholder = "<clientId>",
                configFilePath = "/cloud/clientId",
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
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
        val parsedFrontend = parsedArguments["frontend"] as? String ?: "."
        val parsedUrl = parsedArguments["url"] as String
        val parsedClientId = parsedArguments["client-id"] as String
        val parsedClientSecret = parsedArguments["client-secret"] as String
        val parsedAuthUrl = parsedArguments["auth-url"] as String
        return CloudDeployFrontendCommand(
            app = parsedApp,
            tenant = parsedTenant,
            frontend = parsedFrontend,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )

        /*        if (structureSettings.frontEnd == null) {
                    throw RequiredParameterMissing(
                        parameterName = "frontend",
                        yamlExample = "structure:\n  frontend: <directory>",
                    )
                }*/
    }
}

class CloudDeployFrontendCommand(
    private val app: String,
    private val tenant: String,
    private val frontend: String,
    private val url: String,
    private val clientId: String,
    private val clientSecret: String,
    private val authUrl: String,
) : CommandExecutor {
    init {
        val frontendFile = File(frontend)
        if (!frontendFile.exists() || !frontendFile.isDirectory) {
            throw CloudCommandException(
                message = "Build directory does not exist or is not a directory - $frontend",
                commandName = "cloud deploy frontend",
            )
        }
    }

    val sourcesManager = SourcesManager(frontend)
    val noumenaCloudAuthConfig =
        NoumenaCloudAuthConfig.get(
            clientId = clientId,
            clientSecret = clientSecret,
            url = authUrl,
        )

    val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
    val config = NoumenaCloudConfig.get(app, tenant, url)
    val noumenaCloudClient = NoumenaCloudClient(config)
    val cloudDeployService = CloudDeployService(cloudAuthManager, noumenaCloudClient)

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
