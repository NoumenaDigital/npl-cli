package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService.compileAndReport
import com.noumenadigital.npl.lang.ProtocolProto
import com.noumenadigital.platform.nplapi.ApiConfiguration
import com.noumenadigital.platform.nplapi.openapi.OpenAPIGenerator
import io.swagger.v3.core.util.Yaml
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths

data class OpenapiCommand(
    private val targetDir: String = ".",
) : CommandExecutor {
    override val commandName: String = "openapi"
    override val description: String = "Generate the openapi specifications of NPL api"
    private val defaultUri = "http://localhost:12000"

    override val parameters: List<CommandParameter> =
        listOf(
            CommandParameter(
                name = "directory",
                description = "Target directory containing NPL protocols",
                defaultValue = ".",
                isRequired = false,
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val targetDir = params.firstOrNull() ?: parameters.find { it.name == "directory" }?.defaultValue ?: "."
        return OpenapiCommand(targetDir = targetDir)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val compilationResult = compileAndReport(sourcesDir = targetDir, output = output)
            when {
                compilationResult.hasErrors -> {
                    output.error("NPL openapi failed with errors.")
                    return ExitCode.COMPILATION_ERROR
                }

                else -> {
                    if (compilationResult.hasWarnings) { // might be unused
                        output.warning("NPL openapi has compilation warnings")
                    }
                    val protocolsPerPackage: Map<String, List<ProtocolProto>>? =
                        compilationResult.protos
                            ?.filterIsInstance<ProtocolProto>()
                            ?.groupBy { it.protoId.qualifiedPath.toString() }

                    if (protocolsPerPackage.isNullOrEmpty()) {
                        output.error("No NPL protocols found in the target directory.")
                        return ExitCode.DATA_ERROR
                    }

                    protocolsPerPackage.forEach { (packagePath, protocols) ->
                        output.info("Generating openapi for $packagePath")

                        val apiGen =
                            try {
                                OpenAPIGenerator(ApiConfiguration(URI(defaultUri), null), packagePath)
                            } catch (e: URISyntaxException) {
                                throw CommandExecutionException("Failed to run openapi generation", e)
                            }

                        val openApi = apiGen.generate(protocols)
                        val packageName = packagePath.removePrefix("/").replace("/", ".")

                        writeToFile(
                            Yaml.pretty(openApi),
                            Paths.get(targetDir, "openapi", "$packageName-openapi.yml"),
                        )
                    }
                    output.success("NPL openapi completed successfully.")
                    return ExitCode.SUCCESS
                }
            }
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL check: ${e.message}", e)
        }
    }

    private fun writeToFile(
        content: String,
        filePath: Path,
    ) {
        try {
            val file = filePath.toFile()
            file.parentFile?.mkdirs()

            file.writeText(content)
        } catch (e: IOException) {
            throw CommandExecutionException("Failed to write to file: ${e.message}", e)
        }
    }
}
