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
    override val commandName: String = "cloud deploy"
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
                name = "--appId",
                description = "NOUMENA Cloud Application appId",
                isRequired = true,
                valuePlaceholder = "<appId>",
            ),
            NamedParameter(
                name = "--migration",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                valuePlaceholder = "<migration>",
            ),
            NamedParameter(
                name = "--url",
                description = "NOUMENA Cloud deployment URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
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
        val app = parsedArgs.getRequiredValue("--appId")
        val tenant = parsedArgs.getRequiredValue("--tenant")
        val migration = parsedArgs.getValue("--migration") ?: "./src/main/migration.yaml"
        val migrationFile = File(migration)
        if (!migrationFile.exists()) {
            throw CloudCommandException(
                message = "Migration file does not exist: $migration",
                commandName = "cloud deploy",
            )
        }
        val clientId = parsedArgs.getValue("--clientId")
        val clientSecret = parsedArgs.getValue("--clientSecret")
        val authUrl = parsedArgs.getValue("--authUrl")
        val url = parsedArgs.getValue("--url")
        val srcDir = migrationFile.parent.toString()
        val sourcesManager = SourcesManager(srcDir)
        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(clientId, clientSecret, authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudDeployService =
            CloudDeployService(
                CloudAuthManager(noumenaCloudAuthClient),
                NoumenaCloudClient(NoumenaCloudConfig.get(app, tenant, url)),
            )
        return CloudDeployNplCommand(sourcesManager, cloudDeployService)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val archive = sourcesManager.getArchivedSources()
            cloudDeployService.deployNplApplication(archive)
            output.success("NPL Application successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud deploy")
        }
    }
}
