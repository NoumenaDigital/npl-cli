package com.noumenadigital.npl.cli.commands.registry.cloud.deploy

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.ArgumentParser
import com.noumenadigital.npl.cli.commands.CommandConfig
import com.noumenadigital.npl.cli.commands.EnvironmentVariable
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.exception.RequiredParameterMissing
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
                valuePlaceholder = "<auth-url>",
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
        val config =
            ArgumentParser.parse(params, parameters) { settings ->
                CloudDeployNplConfig(
                    app = settings.cloud.app ?: throw RequiredParameterMissing("app"),
                    tenant = settings.cloud.tenant ?: throw RequiredParameterMissing("tenant"),
                    migration = settings.structure.migrationDescriptorFile ?: findSingleFile(migrationFileName),
                    url = settings.cloud.url,
                    clientId = settings.local.clientId,
                    clientSecret = settings.local.clientSecret,
                    authUrl = settings.cloud.authUrl,
                )
            }
        if (!config.migration.exists()) {
            throw CloudCommandException(
                message = "Migration file does not exist - ${config.migration}",
                commandName = "cloud deploy",
            )
        }
        val sourcesManager = SourcesManager(config.migration.parent.toString())
        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(config.clientId, config.clientSecret, config.authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudDeployService =
            CloudDeployService(
                CloudAuthManager(noumenaCloudAuthClient),
                NoumenaCloudClient(NoumenaCloudConfig.get(config.app, config.tenant, config.url)),
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

    fun findSingleFile(
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

data class CloudDeployNplConfig(
    val app: String,
    val tenant: String,
    val migration: File,
    val url: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val authUrl: String? = null,
) : CommandConfig
