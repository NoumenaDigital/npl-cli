package com.noumenadigital.npl.cli.settings

import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandArgumentParser.ParsedArguments
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YAMLConfigParser
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.ArgumentParsingException
import java.io.File

object SettingsResolver {
    private fun String.toFile(): File = File(this)

    private fun Boolean.orElse(default: Boolean?): Boolean = (takeIf { it } ?: default) == true

    // Hardcoded deprecated CLI parameter name replacements (without leading dashes)
    // Example: --projectDir -> --project-dir
    private val defaultDeprecatedNames: Map<String, String> =
        mapOf(
            // Common across commands
            "sourceDir" to "source-dir",
            "outputDir" to "output-dir",
            // init
            "projectDir" to "project-dir",
            "templateUrl" to "template-url",
            // structure
            "testSourceDir" to "test-source-dir",
            "frontEnd" to "frontend",
            // cloud/local auth
            "clientId" to "client-id",
            "clientSecret" to "client-secret",
            "authUrl" to "auth-url",
            "managementUrl" to "management-url",
        )

    private fun normalizeDeprecatedArgs(
        args: List<String>,
        deprecatedNames: Map<String, String>,
    ): Pair<List<String>, List<String>> {
        if (args.isEmpty() || deprecatedNames.isEmpty()) return Pair(args, emptyList())

        val warnings = mutableListOf<String>()
        val normalized =
            args.map { token ->
                if (token.startsWith("--")) {
                    val name = token.removePrefix("--")
                    val replacement = deprecatedNames[name]
                    if (replacement != null) {
                        warnings.add("Parameter '--$name' is deprecated; use '--$replacement' instead.")
                        "--$replacement"
                    } else {
                        token
                    }
                } else {
                    token
                }
            }
        return Pair(normalized, warnings)
    }

    fun resolveCloud(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): CloudSettings =
        CloudSettings(
            app = parsedArgs.getValueOrElse("app", yamlConfig?.cloud?.app),
            authUrl = parsedArgs.getValueOrElse("auth-url", yamlConfig?.cloud?.authUrl),
            clear = parsedArgs.hasFlag("clear").orElse(yamlConfig?.cloud?.clear),
            tenant = parsedArgs.getValueOrElse("tenant", yamlConfig?.cloud?.tenant),
            url = parsedArgs.getValueOrElse("url", yamlConfig?.cloud?.url),
            clientId = parsedArgs.getValueOrElse("client-id", yamlConfig?.cloud?.clientId),
            clientSecret = parsedArgs.getValueOrElse("client-secret", yamlConfig?.cloud?.clientSecret),
        )

    fun resolveLocal(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): LocalSettings =
        LocalSettings(
            managementUrl =
                parsedArgs.getValueOrElse("management-url", yamlConfig?.local?.managementUrl) ?: "http://localhost:12400/realms/noumena",
            authUrl = parsedArgs.getValueOrElse("auth-url", yamlConfig?.local?.authUrl) ?: "http://localhost:11000",
            password = parsedArgs.getValueOrElse("password", yamlConfig?.local?.password),
            username = parsedArgs.getValueOrElse("username", yamlConfig?.local?.username),
            clientId = parsedArgs.getValueOrElse("client-id", yamlConfig?.local?.clientId),
            clientSecret = parsedArgs.getValueOrElse("client-secret", yamlConfig?.local?.clientSecret),
            clear = parsedArgs.hasFlag("clear").orElse(yamlConfig?.local?.clear),
        )

    fun resolveStructure(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): StructureSettings =
        StructureSettings(
            frontEnd = parsedArgs.getValueOrElse("frontend", yamlConfig?.structure?.frontend)?.toFile(),
            migrationDescriptorFile =
                parsedArgs
                    .getValueOrElse("migration", yamlConfig?.structure?.migration)
                    ?.toFile(),
            nplSourceDir = parsedArgs.getValueOrElse("source-dir", yamlConfig?.structure?.sourceDir)?.toFile(),
            outputDir = parsedArgs.getValueOrElse("output-dir", yamlConfig?.structure?.outputDir)?.toFile(),
            rulesFile = parsedArgs.getValueOrElse("rules", yamlConfig?.structure?.rules)?.toFile(),
            testCoverage = parsedArgs.hasFlag("coverage").orElse(yamlConfig?.structure?.coverage),
            testSourceDir = parsedArgs.getValueOrElse("test-source-dir", yamlConfig?.structure?.testSourceDir)?.toFile(),
        )

    // Convenience helpers to reduce duplication in commands
    fun parseArgs(
        args: List<String>,
        parameters: List<NamedParameter>,
        deprecatedNames: Map<String, String> = defaultDeprecatedNames,
    ): ParsedArguments {
        val (normalized, warnings) = normalizeDeprecatedArgs(args, deprecatedNames)
        val parsed = CommandArgumentParser.parse(normalized, parameters)
        return parsed.withWarnings(warnings)
    }

    fun parseArgsOrThrow(
        args: List<String>,
        parameters: List<NamedParameter>,
        deprecatedNames: Map<String, String> = defaultDeprecatedNames,
    ): ParsedArguments {
        val parsed = parseArgs(args, parameters, deprecatedNames)
        if (parsed.unexpectedArgs.isNotEmpty()) {
            throw ArgumentParsingException("Unexpected arguments: ${parsed.unexpectedArgs.joinToString(" ")}")
        }
        return parsed
    }

    fun loadYamlConfig(): YamlConfig? = YAMLConfigParser.parse()

    fun resolveInit(parsedArgs: ParsedArguments): InitSettings =
        InitSettings(
            projectDir = parsedArgs.getValue("project-dir")?.let { File(it) },
            bare = parsedArgs.hasFlag("bare"),
            templateUrl = parsedArgs.getValue("template-url"),
        )

    fun resolveInitFrom(
        args: List<String>,
        parameters: List<NamedParameter>,
        deprecatedNames: Map<String, String> = defaultDeprecatedNames,
    ): InitSettings {
        val parsed = parseArgs(args, parameters, deprecatedNames)
        return resolveInit(parsed)
    }
}
