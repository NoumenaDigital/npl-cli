package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.ExitCode.GENERAL_ERROR
import com.noumenadigital.npl.cli.ExitCode.SUCCESS
import com.noumenadigital.npl.cli.commands.CommandParameter
import com.noumenadigital.npl.cli.commands.PositionalParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.pumlgen.NplPumlObjectGenerator.generateFiles
import java.io.File

data class PumlCommand(
    private val srcDir: String = ".",
    private val outputDir: String = ".",
) : CommandExecutor {
    override val commandName: String = "puml"
    override val description: String = "Generate a puml diagram from source in the given directory"

    override val parameters: List<CommandParameter> =
        listOf(
            PositionalParameter(
                name = "directory",
                description = "Source directory containing NPL protocols",
                defaultValue = ".",
                isRequired = false,
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        try {
            if (!File(srcDir).isDirectory() || !File(srcDir).exists()) {
                throw CommandExecutionException("Source directory does not exist or is not a directory: $srcDir")
            }

            val outputDir = File(outputDir).resolve("puml")
            val protosMap = CompilerService(SourcesManager(srcDir)).compileAndReport(output = output).userDefinedMap

            if (protosMap == null) {
                throw CommandExecutionException("No user defined types found, check sources and try again.")
            }

            val pumlFiles = generateFiles(protosMap)

            outputDir.mkdirs()
            output.info("Writing Puml files to ${outputDir.canonicalPath}\n")

            pumlFiles.forEach {
                try {
                    it.write(outputDir.toPath())
                } catch (e: Exception) {
                    throw CommandExecutionException(e.message!!, e.cause)
                }
            }

            output.success("Puml diagram generated successfully.")
            return SUCCESS
        } catch (e: CommandExecutionException) {
            output.error(e.message)
            return GENERAL_ERROR
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL puml: ${e.message}", e)
        }
    }

    override fun createInstance(params: List<String>): CommandExecutor {
        val srcDir =
            params.firstOrNull() ?: parameters.find { it.name == "directory" }?.defaultValue ?: CURRENT_DIRECTORY
        return PumlCommand(srcDir = srcDir)
    }

    companion object {
        private const val CURRENT_DIRECTORY = "."
    }
}
