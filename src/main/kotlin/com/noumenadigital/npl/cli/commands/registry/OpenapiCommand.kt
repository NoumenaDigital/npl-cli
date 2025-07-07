package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandParameter
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.lang.Proto
import com.noumenadigital.npl.lang.ProtocolProto
import com.noumenadigital.npl.lang.Type
import com.noumenadigital.npl.naming.TaggerUtils.untagged
import com.noumenadigital.npl.party.assignment.models.PartyAssignments
import com.noumenadigital.npl.party.assignment.parsing.PartyAssignmentRulesParser.parseYamlString
import com.noumenadigital.npl.party.assignment.validation.PartyAssignmentRulesValidator
import com.noumenadigital.platform.nplapi.ApiConfiguration
import com.noumenadigital.platform.nplapi.openapi.OpenAPIGenerator
import io.swagger.v3.core.util.Yaml
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists

data class OpenapiCommand(
    private val srcDir: String = ".",
    private val ruleDescriptorPath: String? = null,
    private val compilerService: CompilerService = CompilerService(SourcesManager(srcDir)),
) : CommandExecutor {
    override val commandName: String = "openapi"
    override val description: String = "Generate the openapi specifications of NPL api"

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "sourceDir",
                description = "Source directory containing NPL protocols",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
            ),
            NamedParameter(
                name = "rules",
                description =
                    "Path to the party automation rules descriptor. If omitted, generated document will not reflect the current system",
                isRequired = false,
                valuePlaceholder = "<rules descriptor path>",
            ),
        )

    companion object {
        private const val CURRENT_DIRECTORY = "."
        private const val DEFAULT_OPENAPI_URI = "http://localhost:12000"
    }

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = parseParams(params)

        if (parsedArgs.unexpectedArgs.isNotEmpty()) {
            throw CommandExecutionException("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
        }

        val srcDir = parsedArgs.getValue("sourceDir") ?: "."
        val rules = parsedArgs.getValue("rules")
        return OpenapiCommand(srcDir, rules)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val sourcePath = Paths.get(srcDir)
            if (sourcePath.notExists()) {
                output.error("Source directory does not exist: $srcDir")
                return ExitCode.USAGE_ERROR
            }

            val partyAssignments =
                ruleDescriptorPath?.let {
                    val file = File(it)
                    if (it.isBlank() || !file.exists() || file.isDirectory) {
                        output.error("Rules descriptor is invalid, blank or does not exist: $it")
                        return ExitCode.USAGE_ERROR
                    }

                    try {
                        parseYamlString(file.readText()).toPartyAssignments()
                    } catch (e: Exception) {
                        output.error("Failed while parsing the party automation rules: ${e.message}")
                        return ExitCode.GENERAL_ERROR
                    }
                } ?: PartyAssignments()

            val compilationResult = compilerService.compileAndReport(output = output)
            if (compilationResult.hasErrors) {
                output.error("NPL openapi failed with errors.")
                return ExitCode.COMPILATION_ERROR
            }
            if (compilationResult.hasWarnings) {
                output.warning("NPL openapi has compilation warnings")
            }

            val protocolsPerPackage: Map<String, List<ProtocolProto>>? =
                compilationResult.protos
                    ?.filterIsInstance<ProtocolProto>()
                    ?.groupBy { it.protoId.qualifiedPath.toString() }
                    ?.toSortedMap()

            if (protocolsPerPackage.isNullOrEmpty()) {
                output.error("No NPL protocols found in the target directory.")
                return ExitCode.GENERAL_ERROR
            }

            protocolsPerPackage.forEach { (packagePath, protocols) ->
                output.info("Generating openapi for ${packagePath.removePrefix()}")

                val apiGen =
                    try {
                        OpenAPIGenerator(ApiConfiguration(URI(DEFAULT_OPENAPI_URI), null), packagePath)
                    } catch (e: URISyntaxException) {
                        throw CommandExecutionException("Failed to run openapi generation", e)
                    }

                try {
                    validatePartyRules(partyAssignments, compilationResult.protos)
                } catch (e: Exception) {
                    output.error("Failed while validating the Party automation rules: ${e.message}")
                    return ExitCode.GENERAL_ERROR
                }

                val openApi = apiGen.generate(protocols, partyAssignments)
                val packageName = packagePath.removePrefix().replace("/", ".")

                writeToFile(
                    Yaml.pretty(openApi),
                    Paths.get(CURRENT_DIRECTORY, "openapi", "$packageName-openapi.yml"),
                )
            }
            output.success("NPL openapi completed successfully.")
            return ExitCode.SUCCESS
        } catch (e: CommandExecutionException) {
            output.error(e.message)
            return ExitCode.GENERAL_ERROR
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL check: ${e.message}", e)
        }
    }

    private fun validatePartyRules(
        rules: PartyAssignments,
        allProtos: List<Proto<Type>?>,
    ) {
        val protosMap = allProtos.filterIsInstance<ProtocolProto>().associateBy { it.protoId.toString().untagged() }

        rules.ruleSet.forEach { rule ->
            val proto = protosMap[rule.untaggedPrototypeId.toString()]

            if (proto == null) {
                error("No matching prototype found matching [" + rule.untaggedPrototypeId + "]")
            }
            PartyAssignmentRulesValidator.validateParties(rule, proto.actualType.parties)
        }
    }

    private fun parseParams(args: List<String>) = CommandArgumentParser.parse(args, parameters)

    private fun String.removePrefix(): String = removePrefix("/")

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
