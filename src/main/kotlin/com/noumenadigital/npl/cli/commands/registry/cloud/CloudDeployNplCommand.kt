package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandParameter
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

class CloudDeployNplCommand(
    val sourcesManager: SourcesManager = SourcesManager("."),
    val cloudDeployService: CloudDeployService =
        CloudDeployService(
            CloudAuthManager(),
            NoumenaCloudClient(NoumenaCloudConfig()),
        ),
) : CommandExecutor {
    override val commandName: String = "cloud deploy-npl"
    override val description: String = "Deploy NPL sources to a Noumena Cloud"

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "--tenant",
                description = "NOUMENA Cloud Tenant name",
                isRequired = true,
                valuePlaceholder = "<tenant>",
            ),
            NamedParameter(
                name = "--app",
                description = "NOUMENA Cloud Application name",
                isRequired = true,
                valuePlaceholder = "<app>",
            ),
            NamedParameter(
                name = "--migration",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                valuePlaceholder = "<migration>",
            ),
            NamedParameter(
                name = "--nclUrl",
                description = "NOUMENA Cloud deployment URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<nclUrl>",
            ),
            NamedParameter(
                name = "--clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "--clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "--authUrl",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<authUrl>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val app = parsedArgs.getRequiredValue("--app")
        val tenant = parsedArgs.getRequiredValue("--tenant")
        val migration = parsedArgs.getValue("--migration") ?: "./src/main/migration.yaml"
        val clientId = parsedArgs.getValue("--clientId")
        val clientSecret = parsedArgs.getValue("--clientSecret")
        val authUrl = parsedArgs.getValue("--authUrl")
        val srcDir = File(migration).parent.toString()
        val sourcesManager = SourcesManager(srcDir)
        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.getInstance(clientId, clientSecret, authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudDeployService =
            CloudDeployService(CloudAuthManager(noumenaCloudAuthClient), NoumenaCloudClient(NoumenaCloudConfig(app, tenant)))
        return CloudDeployNplCommand(sourcesManager, cloudDeployService)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val archive = sourcesManager.getArchivedSources()
            cloudDeployService.deployNplApplication(archive)
            output.success("NPL Application successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud deploy-npl")
        }
    }
}
