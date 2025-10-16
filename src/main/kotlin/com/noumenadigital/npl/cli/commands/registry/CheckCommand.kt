package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.exception.CommandValidationException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File

object CheckCommandDescriptor : CommandDescriptor {
    override val commandName: String = "check"
    override val description: String = "Validate the correctness of NPL sources"
    override val supportsMcp: Boolean = true

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedSrcDir = parsedArguments["source-dir"] as? String ?: "."
        return CheckCommand(srcDir = parsedSrcDir)
    }

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "source-dir",
                description = "Directory containing NPL source files",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = "/structure/sourceDir",
            ),
        )
}

data class CheckCommand(
    private val srcDir: String,
) : CommandExecutor {
    init {
        checkDirectory(srcDir)
    }

    private val sourcesManager = SourcesManager(srcDir)
    private val compilerService = CompilerService(sourcesManager)

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val result = compilerService.compileAndReport(output = output)

            return when {
                result.hasErrors -> {
                    output.error("NPL check failed with errors.")
                    ExitCode.COMPILATION_ERROR
                }

                result.hasWarnings -> {
                    output.warning("NPL check completed with warnings.")
                    ExitCode.GENERAL_ERROR
                }

                else -> {
                    output.success("NPL check completed successfully.")
                    ExitCode.SUCCESS
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
            throw CommandValidationException("Target directory does not exist: ${dir.relativeOrAbsolute()}")
        }

        if (!dir.isDirectory) {
            throw CommandValidationException("Target path is not a directory: ${dir.relativeOrAbsolute()}")
        }
    }
}
