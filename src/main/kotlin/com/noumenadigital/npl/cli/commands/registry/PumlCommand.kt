package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.ExitCode.GENERAL_ERROR
import com.noumenadigital.npl.cli.ExitCode.SUCCESS
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File

object PumlCommandDescriptor : CommandDescriptor {
    override val commandName: String = "puml"
    override val description: String = "Generate a puml diagram from source in the given directory"
    override val supportsMcp: Boolean = true

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedSrcDir = parsedArguments["source-dir"] as? String ?: "."
        val parsedOutputDir = parsedArguments["output-dir"] as? String ?: "."
        return PumlCommand(
            srcDir = parsedSrcDir,
            outputDir = parsedOutputDir,
        )
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
                configFilePath = YamlConfig.Structure.sourceDir,
            ),
            NamedParameter(
                name = "output-dir",
                description = "Directory to place generated output files (optional)",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<output directory>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Structure.outputDir,
            ),
        )
}

data class PumlCommand(
    private val srcDir: String,
    private val outputDir: String,
) : CommandExecutor {
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
            val protosMap =
                CompilerService(SourcesManager(srcDir)).compileAndReport(output = output).userDefinedMap
                    ?: throw CommandExecutionException("No user defined types found, check sources and try again.")

            val pumlFiles =
                com.noumenadigital.pumlgen.NplPumlObjectGenerator
                    .generateFiles(protosMap)

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
}
