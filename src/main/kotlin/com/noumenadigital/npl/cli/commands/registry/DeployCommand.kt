package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.DeployResult
import com.noumenadigital.npl.cli.service.DeployService
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File

object DeployCommandDescriptor : CommandDescriptor {
    override val commandName: String = "deploy"
    override val description: String = "Deploy NPL sources to a Noumena Engine instance"
    override val usageInstruction: String =
        """
        deploy --source-dir <directory> [--clear]

        Deploys NPL sources to a Noumena Engine instance.

        Arguments:
          --source-dir <directory>   Directory containing NPL sources (required).
                             IMPORTANT: The directory must contain a valid NPL source structure, including
                             migrations. E.g.:
                              main
                              ├── npl-1.0
                              └── migration.yml

        Options:
          --clear             Clear application contents before deployment.

        Configuration is read from npl.yml (current dir).
        """.trimIndent()

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "source-dir",
                description = "Directory containing NPL sources",
                isRequired = true,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = "/structure/sourceDir",
            ),
            NamedParameter(
                name = "clear",
                description = "Clear application contents before deployment",
                isRequired = false,
                configFilePath = "/local/clear",
            ),
            NamedParameter(
                name = "username",
                description = "Username for deployment to Noumena Engine instance",
                isRequired = true,
                valuePlaceholder = "<username>",
                isHidden = true,
                configFilePath = "/local/username",
            ),
            NamedParameter(
                name = "password",
                description = "Password for deployment to Noumena Engine instance",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<password>",
                configFilePath = "/local/password",
            ),
            NamedParameter(
                name = "management-url",
                description = "Url of the Noumena Engine management endpoint",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = "/local/managementUrl",
            ),
            NamedParameter(
                name = "client-id",
                description = "Client ID for deployment to Noumena Engine instance",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<client-id>",
                configFilePath = "/local/clientId",
            ),
            NamedParameter(
                name = "client-secret",
                description = "Client secret for deployment to Noumena Engine instance",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<secret>",
                configFilePath = "/local/clientSecret",
            ),
            NamedParameter(
                name = "auth-url",
                description = "Authentication URL of the Noumena Engine instance",
                isRequired = true,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = "/local/authUrl",
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedSourceDir = parsedArguments["source-dir"] as String
        val parsedClear = !(parsedArguments["clear"] == null || parsedArguments["clear"] as? Boolean == false)
        val parsedUsername = parsedArguments["username"] as String
        val parsedPassword = parsedArguments["password"] as String
        val parsedManagementUrl = parsedArguments["management-url"] as String
        val parsedClientSecret = parsedArguments["client-secret"] as String
        val parsedClientId = parsedArguments["client-id"] as String
        val parsedAuthUrl = parsedArguments["auth-url"] as String
        return DeployCommand(
            sourceDir = parsedSourceDir,
            clear = parsedClear,
            username = parsedUsername,
            password = parsedPassword,
            managementUrl = parsedManagementUrl,
            clientSecret = parsedClientSecret,
            clientId = parsedClientId,
            authUrl = parsedAuthUrl,
        )
    }
}

class DeployCommand(
    private val sourceDir: String,
    private val clear: Boolean? = false,
    private val username: String,
    private val password: String,
    private val managementUrl: String,
    private val clientSecret: String,
    private val clientId: String,
    private val authUrl: String,
) : CommandExecutor {
    val deployService =
        DeployService(
            username = username,
            password = password,
            managementUrl = managementUrl,
            clientId = clientId,
            clientSecret = clientSecret,
            authUrl = authUrl,
        )

    override fun execute(output: ColorWriter): ExitCode {
        val sourceDirFile = File(sourceDir)
        if (!sourceDirFile.exists()) {
            output.error("Source directory does not exist: ${sourceDirFile.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        if (!sourceDirFile.isDirectory) {
            output.error("Source path is not a directory: ${sourceDirFile.relativeOrAbsolute()}")
            return ExitCode.GENERAL_ERROR
        }

        if (clear == true) {
            when (val clearResult = deployService.clearApplication()) {
                is DeployResult.ClearSuccess -> {
                    output.info("Application contents cleared for $managementUrl")
                }

                is DeployResult.ClearFailed -> {
                    output.error("Failed to clear application contents: ${clearResult.exception.message ?: "Unknown error"}")
                    return ExitCode.GENERAL_ERROR
                }

                else -> {
                    output.error("Unexpected result during clear operation: $clearResult")
                    return ExitCode.GENERAL_ERROR
                }
            }
        }

        return when (val deployResult = deployService.deploySourcesAndMigrations(sourceDirFile.absolutePath)) {
            is DeployResult.Success -> {
                output.success("Successfully deployed NPL sources and migrations to $managementUrl.")
                ExitCode.SUCCESS
            }

            is DeployResult.DeploymentFailed -> {
                output.error("Error deploying NPL sources: ${deployResult.exception.message ?: "Unknown deployment failure"}")
                ExitCode.GENERAL_ERROR
            }

            else -> {
                output.error("Internal error: Unhandled deployment result state: $deployResult")
                ExitCode.INTERNAL_ERROR
            }
        }
    }
}
