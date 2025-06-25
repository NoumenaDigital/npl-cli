package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.util.zip.ZipInputStream

class InitCommand(
    private val args: List<String> = emptyList(),
) : CommandExecutor {
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
        val projectName = parsedArgs.getRequiredValue("--project")
        val projectUrl =
            try {
                parseTestUrl(parsedArgs) ?: parseProdUrl(parsedArgs)
            } catch (e: Exception) {
                output.error(e.message.toString())
                return ExitCode.GENERAL_ERROR
            }

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

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
            downloadRepoArchive(archiveFile, projectUrl)
            output.info("Successfully downloaded project files")
        } catch (e: Exception) {
            output.error("Error occurred while downloading project files: ${e.message}")
            return ExitCode.GENERAL_ERROR
        }

        archiveFile.unzip()
        output.info("Project successfully saved to ${projectDir.absolutePath}")

        archiveFile.parentFile.cleanUp()

        return ExitCode.SUCCESS
    }

    override fun createInstance(params: List<String>): CommandExecutor = InitCommand(params)

    private fun parseProdUrl(parsedArgs: CommandArgumentParser.ParsedArguments): String =
        parsedArgs.getValue("--bare")?.let { BARE_URL } ?: SAMPLES_URL

    private fun parseTestUrl(parsedArgs: CommandArgumentParser.ParsedArguments): String? =
        parsedArgs.getValue("--test-url")?.also { validateTestUrl(it) }

    private fun validateTestUrl(url: String) {
        val host = URI.create(url).host

        require(host == "localhost" || host == "127.0.0.1" || host == "::1") {
            "Only localhost or equivalent allowed for --test-url (was '$host')"
        }
    }

    private fun downloadRepoArchive(
        archivePath: File,
        url: String,
    ) {
        val client = HttpClients.createDefault()
        val request = HttpGet(URI.create(url))

        client.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                FileOutputStream(archivePath).use { output ->
                    response.entity.content.copyTo(output)
                }
            } else {
                error("Failed to retrieve project files. Status returned: $statusCode")
            }
        }
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
                    filePath.parentFile.mkdirs() // ensure parent directories exist
                    FileOutputStream(filePath).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    companion object {
        private const val SAMPLES_URL = "https://github.com/NoumenaDigital/npl-init/archive/refs/heads/samples.zip"
        private const val BARE_URL = "https://github.com/NoumenaDigital/npl-init/archive/refs/heads/no-samples.zip"
        private val filesToCleanup = listOf(".gitkeep", "project.zip")
    }
}
