package com.noumenadigital.npl.cli.commands.registry

import com.google.common.collect.Multimap
import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.lang.Proto
import com.noumenadigital.npl.lang.Type
import com.noumenadigital.pumlgen.NplPumlObjectGenerator.generateFiles
import java.io.File
import java.net.URL
import java.nio.file.Path

fun interface WritableArtifact {
    fun write(outputDirectory: Path?)
}

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
        if (!File(srcDir).isDirectory() || !File(srcDir).exists()) {
            output.error("Source directory does not exist or is not a directory: $srcDir")
            return ExitCode.GENERAL_ERROR
        }

        val outputDir =
            File(outputDir).resolve("puml").also {
                if (!it.mkdirs()) {
                    output.error("Failed to create output directory: ${it.canonicalPath}")
                    return ExitCode.INTERNAL_ERROR
                }
                output.info("Puml output directory: ${it.canonicalPath}\n")
            }

        val protosMap =
            CompilerService(SourcesManager(srcDir))
                .compileAndReport(output = output)
                .userDefinedMap
                ?: error("No user defined types found")

        writePumlFilesToOutput(protosMap, outputDir)

        output.success("Puml diagram generated successfully.")

        return ExitCode.SUCCESS
    }

    private fun writePumlFilesToOutput(
        protosMap: Multimap<URL, Proto<Type>>,
        outputDir: File,
    ) {
        val pumlFiles =
            generateFiles(protosMap)
                .map { e ->
                    WritableArtifact { outputDirectory ->
                        outputDirectory?.let { e.write(it) }
                    }
                }

        pumlFiles.forEach { it.write(outputDir.toPath()) }
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
