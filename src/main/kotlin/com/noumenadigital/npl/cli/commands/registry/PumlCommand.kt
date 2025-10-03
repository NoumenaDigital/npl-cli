package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.ExitCode.GENERAL_ERROR
import com.noumenadigital.npl.cli.ExitCode.SUCCESS
import com.noumenadigital.npl.cli.commands.CommandConfig
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.settings.DefaultSettingsProvider
import com.noumenadigital.npl.cli.settings.SettingsProvider
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import java.io.File

data class PumlCommand(
    private val srcDir: String = ".",
    private val outputDir: String = ".",
    private val settings: SettingsProvider? = null,
) : CommandExecutor {
    override val commandName: String = "puml"
    override val description: String = "Generate a puml diagram from source in the given directory"

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
            ),
            NamedParameter(
                name = "output-dir",
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

    override fun createInstance(params: List<String>): CommandExecutor {
        val settings = DefaultSettingsProvider(params, parameters)
        val structure = settings.structure
        val config =
            PumlConfig(
                sourceDir = structure.nplSourceDir ?: File("."),
                outputDir = structure.outputDir ?: File("."),
            )

        return PumlCommand(config.sourceDir.absolutePath, config.outputDir.absolutePath, settings)
    }
}

data class PumlConfig(
    val sourceDir: File = File("."),
    val outputDir: File = File("."),
) : CommandConfig
