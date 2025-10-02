package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.AppSettings.Other
import com.noumenadigital.npl.cli.commands.CommandArgumentParser.ParsedArguments
import com.noumenadigital.npl.cli.config.YAMLConfigParser
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.ArgumentParsingException
import java.io.File

data class NamedParameter(
    val name: String,
    val description: String,
    val defaultValue: String? = null,
    val isRequired: Boolean = false,
    val isHidden: Boolean = false,
    val valuePlaceholder: String? = null,
    val takesPath: Boolean = false,
    val isRequiredForMcp: Boolean = isRequired && !isHidden,
) {
    init {
        require(!name.startsWith("--")) { "Named parameters should not start with '--' in definition" }
    }

    val takesValue: Boolean = valuePlaceholder != null

    val cliName: String = "--$name"
}

/**
 * Simple command line argument parser supporting "--param value" format.
 */
object CommandArgumentParser {
    fun parse(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): ParsedArguments {
        val paramDefs = parameters.associateBy { it.cliName }

        val parsed = mutableMapOf<String, String>()
        val consumedIndices = mutableSetOf<Int>()

        // Pass 1: Consume "--param value" pairs
        for (i in 0 until args.size - 1) {
            if (i in consumedIndices) continue

            val arg = args[i]
            val nextArg = args[i + 1]
            val paramDef = if (arg.startsWith("--")) paramDefs[arg] else null

            if (paramDef != null && paramDef.takesValue && !nextArg.startsWith("--")) {
                parsed[paramDef.name] = nextArg
                consumedIndices.add(i)
                consumedIndices.add(i + 1)
            }
        }

        // Pass 2: Process remaining arguments for flags or unexpected items
        val unexpected = mutableListOf<String>()
        for (i in args.indices) {
            if (i in consumedIndices) continue

            val arg = args[i]
            val paramDef = if (arg.startsWith("--")) paramDefs[arg] else null

            if (paramDef != null && !paramDef.takesValue) {
                parsed[paramDef.name] = ""
            } else {
                unexpected.add(arg)
            }
        }

        return ParsedArguments(parsed, unexpected)
    }

    data class ParsedArguments(
        private val values: Map<String, String>,
        val unexpectedArgs: List<String>,
    ) {
        fun hasFlag(name: String): Boolean = values.containsKey(name)

        fun getValue(name: String): String? = values[name]

        fun getValueOrElse(
            name: String,
            defaultValue: String?,
        ): String? = values[name] ?: defaultValue
    }
}

object ArgumentParser {
    inline fun <reified T> parse(
        params: List<String>,
        namedParameters: List<NamedParameter>,
        genConfig: (settings: AppSettings) -> T,
    ): T {
        val yamlConfig = YAMLConfigParser.parse()
        val parsed = CommandArgumentParser.parse(params, namedParameters)

        if (parsed.unexpectedArgs.isNotEmpty()) {
            throw ArgumentParsingException("Unexpected arguments: ${parsed.unexpectedArgs.joinToString(" ")}")
        }

        return genConfig(toAppSettings(parsed, yamlConfig))
    }

    fun toAppSettings(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): AppSettings {
        fun String.toFile(): File = File(this)

        fun Boolean.orElse(default: Boolean?): Boolean = (takeIf { it } ?: default) == true

        return AppSettings(
            local =
                AppSettings.Local(
                    clientId = parsedArgs.getValueOrElse("client-id", yamlConfig?.local?.clientId),
                    clientSecret = parsedArgs.getValueOrElse("client-secret", yamlConfig?.local?.clientSecret),
                    managementUrl = parsedArgs.getValueOrElse("management-url", yamlConfig?.local?.managementUrl),
                    password = parsedArgs.getValueOrElse("password", yamlConfig?.local?.password),
                    username = parsedArgs.getValueOrElse("username", yamlConfig?.local?.username),
                ),
            cloud =
                AppSettings.Cloud(
                    app = parsedArgs.getValueOrElse("app", yamlConfig?.cloud?.app),
                    authUrl = parsedArgs.getValueOrElse("auth-url", yamlConfig?.cloud?.authUrl),
                    clear = parsedArgs.hasFlag("clear").orElse(yamlConfig?.cloud?.clear),
                    deploymentUrl = parsedArgs.getValueOrElse("deployment-url", yamlConfig?.cloud?.deploymentUrl),
                    target = parsedArgs.getValueOrElse("target", yamlConfig?.cloud?.target),
                    tenant = parsedArgs.getValueOrElse("tenant", yamlConfig?.cloud?.tenant),
                    url = parsedArgs.getValueOrElse("url", yamlConfig?.cloud?.url),
                ),
            structure =
                AppSettings.Structure(
                    frontEnd = parsedArgs.getValueOrElse("frontend", yamlConfig?.structure?.frontend)?.toFile(),
                    migrationDescriptorFile =
                        parsedArgs
                            .getValueOrElse("migration", yamlConfig?.structure?.migration)
                            ?.toFile(),
                    nplSourceDir = parsedArgs.getValueOrElse("source-dir", yamlConfig?.structure?.sourceDir)?.toFile(),
                    outputDir = parsedArgs.getValueOrElse("output-dir", yamlConfig?.structure?.outputDir)?.toFile(),
                    rulesFile = parsedArgs.getValueOrElse("rules", yamlConfig?.structure?.rules)?.toFile(),
                    testCoverage = parsedArgs.hasFlag("coverage").orElse(yamlConfig?.structure?.coverage),
                    testSourceDir =
                        parsedArgs.getValueOrElse("test-source-dir", yamlConfig?.structure?.testSourceDir)?.toFile(),
                ),
            other =
                Other(
                    projectDir = parsedArgs.getValue("project-dir")?.toFile(),
                    templateUrl = parsedArgs.getValue("template-url"),
                    minimal = parsedArgs.hasFlag("bare"),
                ),
        )
    }
}
