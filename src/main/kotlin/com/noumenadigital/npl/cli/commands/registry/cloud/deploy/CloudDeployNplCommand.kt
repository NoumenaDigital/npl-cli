package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.EnvironmentVariable
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.CloudDeployService
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.SourcesManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence
import kotlin.use

class CloudDeployNplCommand(
    private val sourcesManager: SourcesManager = SourcesManager("."),
    private val cloudDeployService: CloudDeployService =
        CloudDeployService(
            CloudAuthManager(),
            NoumenaCloudClient(NoumenaCloudConfig()),
        ),
) : CommandExecutor {
    override val commandName: String = "cloud deploy npl"
    override val description: String = "Deploy NPL sources to a NOUMENA Cloud Application"
    private val migrationFileName = "migration.yml"

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
                name = "migration",
                description = "Path to migration.yml",
                isRequired = false,
                valuePlaceholder = "<migration>",
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
                name = "clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "authUrl",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<authUrl>",
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

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val app = parsedArgs.getRequiredValue("app")
        val tenant = parsedArgs.getRequiredValue("tenant")
        val migration = parsedArgs.getValue("migration") ?: findSingleFile(migrationFileName).toString()
        val migrationFile = File(migration)
        if (!migrationFile.exists()) {
            throw CloudCommandException(
                message = "Migration file does not exist - $migration",
                commandName = "cloud deploy",
            )
        }
        val clientId = parsedArgs.getValue("clientId")
        val clientSecret = parsedArgs.getValue("clientSecret")
        val authUrl = parsedArgs.getValue("authUrl")
        val url = parsedArgs.getValue("url")
        val sourcesManager = SourcesManager(migrationFile.parent.toString())
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
            val saClientId = cloudDeployService.noumenaCloudClient.config.tenantSlug
            val saClientSecret =
                System.getenv("NPL_SERVICE_ACCOUNT_CLIENT_SECRET") ?: System.getProperty("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
            val archive = sourcesManager.getArchivedSources()

            if (!saClientSecret.isNullOrBlank()) {
                output.info("Preparing to deploy NPL application to NOUMENA Cloud using service account...")
                val accessToken = cloudDeployService.cloudAuthManager.getServiceAccountAccessToken(saClientId, saClientSecret)
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

    private fun findSingleFile(
        fileName: String,
        searchDir: String = ".",
    ): File {
        val srcDir = Paths.get(searchDir).toFile()

        if (!srcDir.exists() || !srcDir.isDirectory) {
            throw CommandExecutionException("Source path '$srcDir' does not exist or is not a directory.")
        }
        val matchedFiles =
            Files.walk(Paths.get(srcDir.toURI())).use { paths ->
                paths
                    .asSequence()
                    .filter { it.isRegularFile() && it.fileName.toString() == fileName }
                    .map(Path::toFile)
                    .toList()
            }

        return when {
            matchedFiles.isEmpty() -> throw CommandExecutionException("No '$fileName' file found in $srcDir")
            matchedFiles.size > 1 -> throw CommandExecutionException(
                "Multiple '$fileName' files found:\n${matchedFiles.joinToString("\n")}",
            )

            else -> matchedFiles.first()
        }
    }
}
