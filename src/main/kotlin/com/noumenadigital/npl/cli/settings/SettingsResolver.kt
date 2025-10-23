package com.noumenadigital.npl.cli.settings

import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandArgumentParser.ParsedArguments
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YAMLConfigParser
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.ArgumentParsingException
import java.io.File

object SettingsResolver {
    private val DEPRECATED_CLI_PARAMS =
        setOf(
            "authUrl",
            "clientId",
            "clientSecret",
            "managementUrl",
            "frontEnd",
            "sourceDir",
            "outputDir",
            "testSourceDir",
            "projectDir",
            "templateUrl",
        )

    private fun String.toFile(): File = File(this)

    private fun Boolean.orElse(default: Boolean?): Boolean = (takeIf { it } ?: default) == true

    fun resolveCloud(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): CloudSettings =
        CloudSettings(
            app =
                parsedArgs.getValueOrElse(
                    name = "app",
                    defaultValue = yamlConfig?.cloud?.app,
                ),
            authUrl =
                parsedArgs.getValueOrElse(
                    name = "auth-url",
                    deprecatedNames = listOf("authUrl"),
                    defaultValue = yamlConfig?.cloud?.authUrl,
                ),
            clear = parsedArgs.hasFlag(name = "clear").orElse(yamlConfig?.cloud?.clear),
            tenant =
                parsedArgs.getValueOrElse(
                    name = "tenant",
                    defaultValue = yamlConfig?.cloud?.tenant,
                ),
            url =
                parsedArgs.getValueOrElse(
                    name = "url",
                    defaultValue = yamlConfig?.cloud?.url,
                ),
            clientId =
                parsedArgs.getValueOrElse(
                    name = "client-id",
                    deprecatedNames = listOf("clientId"),
                    defaultValue = yamlConfig?.cloud?.clientId,
                ),
            clientSecret =
                parsedArgs.getValueOrElse(
                    name = "client-secret",
                    deprecatedNames = listOf("clientSecret"),
                    defaultValue = yamlConfig?.cloud?.clientSecret,
                ),
        )

    fun resolveLocal(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): LocalSettings =
        LocalSettings(
            managementUrl =
                parsedArgs.getValueOrElse(
                    name = "management-url",
                    deprecatedNames = listOf("managementUrl"),
                    defaultValue = yamlConfig?.local?.managementUrl,
                ) ?: "http://localhost:12400/realms/noumena",
            authUrl =
                parsedArgs.getValueOrElse(
                    name = "auth-url",
                    deprecatedNames = listOf("authUrl"),
                    defaultValue = yamlConfig?.local?.authUrl,
                ) ?: "http://localhost:11000",
            password =
                parsedArgs.getValueOrElse(
                    name = "password",
                    defaultValue = yamlConfig?.local?.password,
                ),
            username =
                parsedArgs.getValueOrElse(
                    name = "username",
                    defaultValue = yamlConfig?.local?.username,
                ),
            clientId =
                parsedArgs.getValueOrElse(
                    name = "client-id",
                    deprecatedNames = listOf("clientId"),
                    defaultValue = yamlConfig?.local?.clientId,
                ),
            clientSecret =
                parsedArgs.getValueOrElse(
                    name = "client-secret",
                    deprecatedNames = listOf("clientSecret"),
                    defaultValue = yamlConfig?.local?.clientSecret,
                ),
            clear = parsedArgs.hasFlag(name = "clear").orElse(yamlConfig?.local?.clear),
        )

    fun resolveStructure(
        parsedArgs: ParsedArguments,
        yamlConfig: YamlConfig?,
    ): StructureSettings =
        StructureSettings(
            frontEnd =
                parsedArgs
                    .getValueOrElse(
                        name = "frontend",
                        deprecatedNames = listOf("frontEnd"),
                        defaultValue = yamlConfig?.structure?.frontend,
                    )?.toFile(),
            migrationDescriptorFile =
                parsedArgs
                    .getValueOrElse(name = "migration", defaultValue = yamlConfig?.structure?.migration)
                    ?.toFile(),
            nplSourceDir =
                parsedArgs
                    .getValueOrElse(
                        name = "source-dir",
                        deprecatedNames = listOf("sourceDir"),
                        defaultValue = yamlConfig?.structure?.sourceDir,
                    )?.toFile(),
            outputDir =
                parsedArgs
                    .getValueOrElse(
                        name = "output-dir",
                        deprecatedNames = listOf("outputDir"),
                        defaultValue = yamlConfig?.structure?.outputDir,
                    )?.toFile(),
            rulesFile =
                parsedArgs
                    .getValueOrElse(
                        name = "rules",
                        defaultValue = yamlConfig?.structure?.rules,
                    )?.toFile(),
            testCoverage = parsedArgs.hasFlag("coverage").orElse(yamlConfig?.structure?.coverage),
            testSourceDir =
                parsedArgs
                    .getValueOrElse(
                        name = "test-source-dir",
                        deprecatedNames = listOf("testSourceDir"),
                        defaultValue = yamlConfig?.structure?.testSourceDir,
                    )?.toFile(),
        )

    fun parseArgs(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): ParsedArguments {
        val parsed = CommandArgumentParser.parse(args, parameters)
        return parsed.withoutDeprecatedUnexpectedNames(deprecatedNames = DEPRECATED_CLI_PARAMS)
    }

    fun parseArgsOrThrow(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): ParsedArguments {
        val parsed = CommandArgumentParser.parse(args, parameters)
        val filteredUnexpected = parsed.withoutDeprecatedUnexpectedNames(deprecatedNames = DEPRECATED_CLI_PARAMS).unexpectedArgs
        if (filteredUnexpected.isNotEmpty()) {
            throw ArgumentParsingException("Unexpected arguments: ${filteredUnexpected.joinToString(separator = " ")}")
        }
        return parsed
    }

    fun loadYamlConfig(): YamlConfig? = YAMLConfigParser.parse()

    fun resolveInit(parsedArgs: ParsedArguments): InitSettings =
        InitSettings(
            projectDir =
                parsedArgs
                    .getValueOrElse(
                        name = "project-dir",
                        deprecatedNames = listOf("projectDir"),
                        defaultValue = null,
                    )?.let { File(it) },
            bare = parsedArgs.hasFlag(name = "bare"),
            templateUrl =
                parsedArgs.getValueOrElse(
                    name = "template-url",
                    deprecatedNames = listOf("templateUrl"),
                    defaultValue = null,
                ),
        )

    fun resolveInitFrom(
        args: List<String>,
        parameters: List<NamedParameter>,
    ): InitSettings {
        val parsed = parseArgs(args, parameters)
        return resolveInit(parsed)
    }
}
