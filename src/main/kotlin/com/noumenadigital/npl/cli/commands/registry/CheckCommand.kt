package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import java.io.File

data class CheckCommand(
    private val srcDir: String = ".",
    private val compilerService: CompilerService = CompilerService(SourcesManager(srcDir)),
) : CommandExecutor {
    override val commandName: String = "check"
    override val description: String = "Validate the correctness of NPL sources"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "sourceDir",
                description = "Source directory containing NPL protocols",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

        val srcDir = parsedArgs.getValue("sourceDir") ?: "."
        return CheckCommand(srcDir = srcDir)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            checkDirectory(srcDir)
            val result = compilerService.compileAndReport(output = output)

            when {
                result.hasErrors -> {
                    output.error("NPL check failed with errors.")
                    return ExitCode.COMPILATION_ERROR
                }

                result.hasWarnings -> {
                    output.warning("NPL check completed with warnings.")
                    return ExitCode.GENERAL_ERROR
                }

                else -> {
                    output.success("NPL check completed successfully.")
                    return ExitCode.SUCCESS
                }
            }
        } catch (e: CommandExecutionException) {
            output.error(e.message)
            return ExitCode.GENERAL_ERROR
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL check: ${e.message}", e)
        }
    }

    private fun checkDirectory(directory: String) {
        val dir = File(directory)

        if (!dir.exists()) {
            throw CommandExecutionException("Target directory does not exist: $directory")
        }

        if (!dir.isDirectory) {
            throw CommandExecutionException("Target path is not a directory: $directory")
        }
    }
}
