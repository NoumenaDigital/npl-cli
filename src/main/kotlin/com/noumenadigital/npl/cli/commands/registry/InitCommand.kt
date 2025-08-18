package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.NO_SAMPLES
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.SAMPLES
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.util.ZipExtractor
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File
import java.util.UUID

class InitCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    private val repoClient = NoumenaGitRepoClient()

    override val commandName: String = "init"
    override val description: String = "Initializes a new project"
    override val supportsMcp: Boolean = false

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "projectDir",
                description = "Directory where project files will be stored. Created if it doesn’t exist",
                valuePlaceholder = "<projectDir>",
            ),
            NamedParameter(
                name = "bare",
                description = "Installs an empty project structure",
                defaultValue = "false",
                isRequired = false,
            ),
            NamedParameter(
                name = "templateUrl",
                description = "URL of a repository containing a ZIP archive of the project template. Overrides the default template",
                isRequired = false,
                valuePlaceholder = "<templateUrl>",
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        fun ColorWriter.displayError(message: String) = error("npl init: $message")

        val parsedArgs = CommandArgumentParser.parse(args, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            if (parsedArgs.unexpectedArgs.contains("name")) {
                output.displayError("Project name cannot be empty.")
                return ExitCode.GENERAL_ERROR
            }
            output.displayError("Unknown arguments found: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
            return ExitCode.GENERAL_ERROR
        }

        if (parsedArgs.getValue("templateUrl") != null && parsedArgs.hasFlag("bare")) {
            output.displayError("Cannot use --bare and --templateUrl together.")
            return ExitCode.USAGE_ERROR
        }

        val projectPath = parsedArgs.getValue("projectDir")
        val projectDir =
            projectPath?.let {
                File(it).apply {
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

        val archiveFile = projectDir.resolve("project${UUID.randomUUID()}.zip")

        try {
            val templateUrl = parsedArgs.getValue("templateUrl") ?: repoClient.getDefaultUrl(parsedArgs)
            repoClient.downloadTemplateArchive(templateUrl, archiveFile)
            output.info("Successfully downloaded project files")
        } catch (e: Exception) {
            output.displayError(e.message.toString())
            return ExitCode.GENERAL_ERROR
        }

        try {
            ZipExtractor.unzip(archiveFile, skipTopDirectory = true, errorOnConflict = true)
            output.info("Project successfully saved to ${projectDir.relativeOrAbsolute()}")
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

    override fun createInstance(params: List<String>): CommandExecutor = InitCommand(params)

    private fun NoumenaGitRepoClient.getDefaultUrl(parsedArgs: CommandArgumentParser.ParsedArguments): String =
        if (parsedArgs.hasFlag("bare")) {
            getBranchUrl(NO_SAMPLES)
        } else {
            getBranchUrl(SAMPLES)
        }

    private fun File.cleanUp() {
        walk().filter { f -> f.isFile && (f.name in filesToCleanup) }.forEach { f -> f.delete() }
    }

    companion object {
        private val filesToCleanup = listOf("project.zip")
    }
}
