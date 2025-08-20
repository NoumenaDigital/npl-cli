package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
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

class CloudServiceAccountDeployCommand(
    private val sourcesManager: SourcesManager = SourcesManager("."),
    private val cloudDeployService: CloudDeployService = CloudDeployService(
        CloudAuthManager(),
        NoumenaCloudClient(NoumenaCloudConfig()),
    ),
    private val clientId: String? = null,
    private val clientSecret: String? = null,
) : CommandExecutor {
    override val commandName: String = "cloud service-deploy"
    override val description: String = "Deploy NPL sources to NOUMENA Cloud using service account credentials"
    private val migrationFileName = "migration.yml"

    override val parameters: List<NamedParameter> = listOf(
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
            description = "Service account client ID (or env NPL_SA_CLIENT_ID)",
            isRequired = true,
            valuePlaceholder = "<clientId>",
        ),
        NamedParameter(
            name = "clientSecret",
            description = "Service account client secret (or env NPL_SA_CLIENT_SECRET)",
            isRequired = true,
            valuePlaceholder = "<clientSecret>",
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
        val clientId = parsedArgs.getValue("clientId")
            ?: System.getenv("NPL_SA_CLIENT_ID")
            ?: System.getProperty("NPL_SA_CLIENT_ID")
        val clientSecret = parsedArgs.getValue("clientSecret")
            ?: System.getenv("NPL_SA_CLIENT_SECRET")
            ?: System.getProperty("NPL_SA_CLIENT_SECRET")
        if (clientId.isNullOrBlank()) {
            throw CloudCommandException("Service account client ID is required (use --clientId or env NPL_SA_CLIENT_ID)")
        }
        if (clientSecret.isNullOrBlank()) {
            throw CloudCommandException("Service account client secret is required (use --clientSecret or env NPL_SA_CLIENT_SECRET)")
        }
        val migration = parsedArgs.getValue("migration") ?: findSingleFile(migrationFileName).toString()
        val migrationFile = File(migration)

        if (!migrationFile.exists()) {
            throw CloudCommandException(
                message = "Migration file does not exist - $migration",
                commandName = "cloud service-deploy",
            )
        }

        val authUrl = parsedArgs.getValue("authUrl")
        val url = parsedArgs.getValue("url")
        val sourcesManager = SourcesManager(migrationFile.parent.toString())

        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(null, null, authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
        val cloudDeployService = CloudDeployService(
            cloudAuthManager,
            NoumenaCloudClient(NoumenaCloudConfig.get(app, tenant, url)),
        )

        return CloudServiceAccountDeployCommand(sourcesManager, cloudDeployService, clientId, clientSecret)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val serviceAccountClientId = clientId ?: throw CloudCommandException("Service account client ID is required")
            val serviceAccountClientSecret = clientSecret ?: throw CloudCommandException("Service account client secret is required")

            output.info("Preparing to deploy NPL application to NOUMENA Cloud using service account...")

            // Get a token using service account credentials WITHOUT persisting it
            val accessToken = cloudDeployService.cloudAuthManager.getServiceAccountAccessToken(serviceAccountClientId, serviceAccountClientSecret)
            output.success("Successfully authenticated with service account credentials")

            // Package the application using SourcesManager
            val archive = sourcesManager.getArchivedSources()

            // Deploy using the token directly
            cloudDeployService.deployNplApplicationWithToken(archive, accessToken)

            output.success("NPL Application successfully deployed to NOUMENA Cloud.")
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud service-deploy")
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

        val matchedFiles = Files.walk(Paths.get(srcDir.toURI())).use { paths ->
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
