package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.NO_SAMPLES
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient.Companion.SupportedBranches.SAMPLES
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.util.ZipExtractor
import java.io.File

class InitCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    private val repoClient = NoumenaGitRepoClient()

    override val commandName: String = "init"
    override val description: String = "Initializes a new project"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "--name",
                description = "Name of the project. A directory of this name will be created in the current directory",
                isRequired = true,
                valuePlaceholder = "<name>",
            ),
            NamedParameter(
                name = "--bare",
                description = "Installs an empty project structure",
                defaultValue = "false",
                isRequired = false,
            ),
            NamedParameter(
                name = "--template-url",
                description = "URL of repository containing a desired project template. Overrides the default template",
                isRequired = false,
                valuePlaceholder = "<template-url>",
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        val parsedArgs = CommandArgumentParser.parse(args, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            if (parsedArgs.unexpectedArgs.contains("--name")) {
                output.error("Project name cannot be empty.")
                return ExitCode.GENERAL_ERROR
            }
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

        val projectName = parsedArgs.getRequiredValue("--name")
        val projectDir =
            File(projectName).apply {
                if (exists()) {
                    output.error("Directory $canonicalPath already exists.")
                    return ExitCode.GENERAL_ERROR
                }
                if (!mkdir()) {
                    output.error("Failed to create directory $canonicalFile.")
                    return ExitCode.GENERAL_ERROR
                }
            }

        val archiveFile = projectDir.resolve("project.zip")

        try {
            val templateUrl = parsedArgs.getValue("--template-url") ?: repoClient.getDefaultUrl(parsedArgs)
            repoClient.downloadTemplateArchive(templateUrl, archiveFile)
            output.info("Successfully downloaded project files")
        } catch (e: Exception) {
            output.error(e.message.toString())
            return ExitCode.GENERAL_ERROR
        }

        ZipExtractor.unzip(archiveFile, skipTopDirectory = true)
        output.info("Project successfully saved to ${projectDir.absolutePath}")

        archiveFile.parentFile.cleanUp()

        return ExitCode.SUCCESS
    }

    override fun createInstance(params: List<String>): CommandExecutor = InitCommand(params)

    private fun NoumenaGitRepoClient.getDefaultUrl(parsedArgs: CommandArgumentParser.ParsedArguments): String =
        when (parsedArgs.getValue("--bare")) {
            null -> getBranchUrl(NO_SAMPLES)
            else -> getBranchUrl(SAMPLES)
        }

    private fun File.cleanUp() {
        walk().filter { it.isFile && it.name in filesToCleanup }.forEach { it.delete() }
    }

    companion object {
        private val filesToCleanup = listOf(".gitkeep", "project.zip")
    }
}
