package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
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

class CloudServiceAccountDeployFrontendCommand(
    private val sourcesManager: SourcesManager = SourcesManager("."),
    private val cloudDeployService: CloudDeployService = CloudDeployService(
        CloudAuthManager(),
        NoumenaCloudClient(NoumenaCloudConfig()),
    ),
    private val clientId: String? = null,
    private val clientSecret: String? = null,
) : CommandExecutor {
    override val commandName: String = "cloud service-deploy-frontend"
    override val description: String = "Deploy frontend sources to NOUMENA Cloud using service account credentials"

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
                name = "clientId",
                description = "Service account client ID",
                isRequired = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "clientSecret",
                description = "Service account client secret",
                isRequired = true,
                valuePlaceholder = "<clientSecret>",
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
                name = "authUrl",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<authUrl>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val app = parsedArgs.getRequiredValue("app")
        val tenant = parsedArgs.getRequiredValue("tenant")
        val clientId = parsedArgs.getRequiredValue("clientId")
        val clientSecret = parsedArgs.getRequiredValue("clientSecret")
        val frontendDir = parsedArgs.getRequiredValue("frontend")
        val dir = File(frontendDir)
        if (!dir.exists() || !dir.isDirectory) {
            throw CloudCommandException(
                message = "Build directory does not exist or is not a directory - $frontendDir",
                commandName = commandName,
            )
        }
        val authUrl = parsedArgs.getValue("authUrl")
        val url = parsedArgs.getValue("url")

        val sourcesManager = SourcesManager(dir.toString())
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(NoumenaCloudAuthConfig.get(null, null, authUrl))
        val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
        val cloudDeployService = CloudDeployService(
            cloudAuthManager,
            NoumenaCloudClient(NoumenaCloudConfig.get(app, tenant, url)),
        )
        return CloudServiceAccountDeployFrontendCommand(sourcesManager, cloudDeployService, clientId, clientSecret)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val serviceAccountClientId = clientId ?: throw CloudCommandException("Service account client ID is required")
            val serviceAccountClientSecret = clientSecret ?: throw CloudCommandException("Service account client secret is required")

            output.info("Preparing to deploy frontend to NOUMENA Cloud using service account...")

            val accessToken = cloudDeployService.cloudAuthManager.getServiceAccountAccessToken(serviceAccountClientId, serviceAccountClientSecret)
            output.success("Successfully authenticated with service account credentials")

            val archive = sourcesManager.getArchivedSources()
            cloudDeployService.deployFrontendWithToken(archive, accessToken)

            output.success("Frontend successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, commandName)
        }
    }
}

