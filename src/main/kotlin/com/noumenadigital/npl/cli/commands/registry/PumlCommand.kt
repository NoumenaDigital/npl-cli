package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.ExitCode.GENERAL_ERROR
import com.noumenadigital.npl.cli.ExitCode.SUCCESS
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import com.noumenadigital.pumlgen.NplPumlObjectGenerator.generateFiles
import java.io.File

data class PumlCommand(
    private val srcDir: String = ".",
    private val outputDir: String = ".",
) : CommandExecutor {
    override val commandName: String = "puml"
    override val description: String = "Generate a puml diagram from source in the given directory"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "sourceDir",
                description = "Directory containing NPL source files",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
            ),
            NamedParameter(
                name = "outputDir",
                description = "Directory to place generated output files (optional)",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<output directory>",
                takesPath = true,
                isRequiredForMcp = true,
            ),
        )

    override fun execute(output: ColorWriter): ExitCode {
        try {
            File(srcDir).let {
                if (!it.isDirectory() || !it.exists()) {
                    throw CommandExecutionException(
                        "Source directory does not exist or is not a directory: ${it.relativeOrAbsolute()}",
                    )
                }
            }

            val outputDirFile = File(outputDir).resolve("puml")
            val protosMap = CompilerService(SourcesManager(srcDir)).compileAndReport(output = output).userDefinedMap

            if (protosMap == null) {
                throw CommandExecutionException("No user defined types found, check sources and try again.")
            }

            val pumlFiles = generateFiles(protosMap)

            outputDirFile.mkdirs()
            output.info("Writing Puml files to ${outputDirFile.relativeOrAbsolute()}\n")

            pumlFiles.forEach {
                try {
                    it.write(outputDirFile.toPath())
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
        val parsedArgs = CommandArgumentParser.parse(params, parameters)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

        val srcDir = parsedArgs.getValue("sourceDir") ?: CURRENT_DIRECTORY
        val outputDir = parsedArgs.getValue("outputDir") ?: CURRENT_DIRECTORY
        return PumlCommand(srcDir = srcDir, outputDir = outputDir)
    }

    companion object {
        private const val CURRENT_DIRECTORY = "."
    }
}
