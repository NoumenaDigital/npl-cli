package com.noumenadigital.npl.cli.settings

import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.CommandArgumentParser.ParsedArguments
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.ArgumentParsingException

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
}
