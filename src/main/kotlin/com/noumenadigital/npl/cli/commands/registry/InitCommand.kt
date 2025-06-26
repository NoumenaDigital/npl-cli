package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.http.NoumenaGitRepoClient
import com.noumenadigital.npl.cli.service.ColorWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class InitCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    private val repoClient = NoumenaGitRepoClient()

    override val commandName: String = "init"
    override val description: String = "Initializes a new project"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "--project",
                description = "Name of the project. A directory of this name will be created in the current directory",
                isRequired = true,
                valuePlaceholder = "<project>",
            ),
            NamedParameter(
                name = "--bare",
                description = "Installs an empty project structure",
                isRequired = false,
            ),
            NamedParameter(
                name = "--test-url",
                description = "URL used for testing - must be local.",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<test-url>",
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        val parsedArgs = CommandArgumentParser.parse(args, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

        val projectName = parsedArgs.getRequiredValue("--project")
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
            parsedArgs.getValue("--test-url")?.let {
                repoClient.downloadTestArchive(it, archiveFile)
            } ?: repoClient.downloadBranchArchive(getBranch(parsedArgs), archiveFile)
            output.info("Successfully downloaded project files")
        } catch (e: Exception) {
            output.error(e.message.toString())
            return ExitCode.GENERAL_ERROR
        }

        archiveFile.unzip()
        output.info("Project successfully saved to ${projectDir.absolutePath}")

        archiveFile.parentFile.cleanUp()

        return ExitCode.SUCCESS
    }

    override fun createInstance(params: List<String>): CommandExecutor = InitCommand(params)

    private fun getBranch(parsedArgs: CommandArgumentParser.ParsedArguments): String =
        when (parsedArgs.getValue("--bare")) {
            null -> SupportedBranches.NO_SAMPLES.branchName
            else -> SupportedBranches.SAMPLES.branchName
        }

    private fun File.cleanUp() {
        walk().filter { it.isFile && it.name in filesToCleanup }.forEach { it.delete() }
    }

    private fun File.unzip() {
        fun String.removeTopDirectory() = split("/").drop(1).joinToString("/")

        val targetDir = parentFile
        ZipInputStream(FileInputStream(this)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val adjustedPath = entry.name.removeTopDirectory()
                if (adjustedPath.isBlank()) {
                    entry = zipIn.nextEntry
                    continue
                }

                val filePath = File(targetDir, adjustedPath)
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile.mkdirs()
                    FileOutputStream(filePath).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    private enum class SupportedBranches(
        val branchName: String,
    ) {
        SAMPLES("samples"),
        NO_SAMPLES("no-samples"),
    }

    companion object {
        private val filesToCleanup = listOf(".gitkeep", "project.zip")
    }
}
