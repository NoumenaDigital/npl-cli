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

    fun resolveOther(parsedArgs: ParsedArguments): OtherSettings =
        OtherSettings(
            projectDir = parsedArgs.getValue("project-dir")?.toFile(),
            templateUrl = parsedArgs.getValue("template-url"),
            minimal = parsedArgs.hasFlag("bare"),
        )

    // Convenience helpers to reduce duplication in commands
    fun parseArgs(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): ParsedArguments = CommandArgumentParser.parse(args, parameters)

    fun parseArgsOrThrow(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): ParsedArguments {
        val parsed = parseArgs(args, parameters)
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
    ): InitSettings {
        val parsed = parseArgs(args, parameters)
        return resolveInit(parsed)
    }
}
