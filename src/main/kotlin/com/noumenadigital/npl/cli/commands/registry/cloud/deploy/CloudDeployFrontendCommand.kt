package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

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
                name = "frontend",
                description = "Path to the frontend source directory containing the index.html file",
                isRequired = false,
                valuePlaceholder = "<frontend>",
                takesPath = true,
                configFilePath = YamlConfig.Structure.frontend,
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

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedApp = parsedArguments["app"] as String
        val parsedTenant = parsedArguments["tenant"] as String
        val parsedFrontend = parsedArguments["frontend"] as? String ?: "."
        val parsedUrl = parsedArguments["url"] as? String ?: "https://portal.noumena.cloud"
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedAuthUrl = parsedArguments["auth-url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"
        return CloudDeployFrontendCommand(
            app = parsedApp,
            tenant = parsedTenant,
            frontend = parsedFrontend,
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
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
            val archive = sourcesManager.getArchivedSources()

            cloudDeployService.deployFrontend(
                archive = archive,
                output = output,
                actionName = "deploy frontend to NOUMENA Cloud",
            )

            output.success("Frontend successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud deploy frontend")
        }
    }
}
