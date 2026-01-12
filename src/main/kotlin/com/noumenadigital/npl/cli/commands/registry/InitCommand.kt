package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.NO_SAMPLES
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.SAMPLES
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedRepos.FRONTEND
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.util.ZipExtractor
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File
import java.util.UUID

object InitCommandDescriptor : CommandDescriptor {
    override val commandName: String = "init"
    override val description: String = "Initializes a new project"
    override val supportsMcp: Boolean = false

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedProjectDir = parsedArguments["project-dir"] as? String
        val parsedBare = !(parsedArguments["bare"] == null || parsedArguments["bare"] as? Boolean == false)
        val parsedFrontend = !(parsedArguments["frontend"] == null || parsedArguments["frontend"] as? Boolean == false)
        val parsedTemplateUrl = parsedArguments["template-url"] as? String
        return InitCommand(parsedProjectDir, parsedBare, parsedFrontend, parsedTemplateUrl)
    }

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "project-dir",
                description = "Directory where project files will be stored. Created if it doesn’t exist",
                valuePlaceholder = "<project-dir>",
                isRequired = false,
                configFilePath = YamlConfig.Structure.initProjectDir,
            ),
            NamedParameter(
                name = "bare",
                description = "Installs an empty project structure",
                defaultValue = "false",
                isRequired = false,
                configFilePath = YamlConfig.Structure.initBare,
            ),
            NamedParameter(
                name = "frontend",
                description = "Installs a frontend project structure",
                defaultValue = "false",
                isRequired = false,
                configFilePath = YamlConfig.Structure.initFrontend,
            ),
            NamedParameter(
                name = "template-url",
                description = "URL of a repository containing a ZIP archive of the project template. Overrides the default template",
                isRequired = false,
                valuePlaceholder = "<template-url>",
                configFilePath = YamlConfig.Structure.initTemplateUrl,
            ),
        )
}

class InitCommand(
    private val projectDir: String? = null,
    private val bare: Boolean? = false,
    private val frontend: Boolean? = false,
    private val templateUrl: String? = null,
) : CommandExecutor {
    private val repoClient = NoumenaGitRepoClient()

    override fun execute(output: ColorWriter): ExitCode {
        fun ColorWriter.displayError(message: String) = error("npl init: $message")

        if (bare == true && frontend == true) {
            output.displayError("Cannot use --bare and --frontend together.")
            return ExitCode.USAGE_ERROR
        }

        if (templateUrl != null && (bare == true || frontend == true)) {
            output.displayError("Cannot use --template-url with --bare or --frontend.")
            return ExitCode.USAGE_ERROR
        }

        val projectDirFile =
            projectDir?.let {
                File(it)
                    .apply {
                        if (exists()) {
                            output.displayError("Directory ${relativeOrAbsolute()} already exists.")
                            return ExitCode.GENERAL_ERROR
                        }
                        if (!mkdir()) {
                            output.displayError("Failed to create directory ${relativeOrAbsolute()}.")
                            return ExitCode.GENERAL_ERROR
                        }
                    }
            } ?: File(".")

        val archiveFile =
            projectDirFile.resolve("project${UUID.randomUUID()}.zip").also {
                it.deleteOnExit()
            }

        try {
            val templateUrl = templateUrl ?: repoClient.getDefaultUrl(bare == true, frontend == true)
            repoClient.downloadTemplateArchive(templateUrl, archiveFile)
            output.info("Successfully downloaded project files")
        } catch (e: Exception) {
            output.displayError(e.message.toString())
            return ExitCode.GENERAL_ERROR
        }

        try {
            ZipExtractor.unzip(archiveFile, skipTopDirectory = true, errorOnConflict = true)
            output.info("Project successfully saved to ${projectDirFile.relativeOrAbsolute()}")
        } catch (e: Exception) {
            output.displayError("Failed to extract project files. ${e.message}")
            return ExitCode.GENERAL_ERROR
        }

        try {
            archiveFile.run {
                parentFile.cleanUp()
                delete()
            }
        } catch (_: Exception) {
            output.displayError("Failed to cleanup temporary files")
        }

        return ExitCode.SUCCESS
    }

    private fun NoumenaGitRepoClient.getDefaultUrl(
        isBare: Boolean,
        isFrontend: Boolean,
    ): String =
        when {
            isFrontend -> getRepoUrl(FRONTEND)
            isBare -> getBranchUrl(NO_SAMPLES)
            else -> getBranchUrl(SAMPLES)
        }

    private fun File.cleanUp() {
        walk().filter { f -> f.isFile && (f.name in filesToCleanup) }.forEach { f -> f.delete() }
    }

    companion object {
        private val filesToCleanup = listOf("project.zip")
    }
}
