package com.noumenadigital.npl.cli.config

import com.noumenadigital.npl.cli.commands.CommandArgumentParser.parseArgsOrThrow
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.exception.RequiredParameterMissing

interface SettingsProvider

class DefaultSettingsProvider(
    private val args: List<String>,
    private val commandDescriptor: CommandDescriptor,
) : SettingsProvider {
    private val parsed by lazy { parseArgsOrThrow(args, commandDescriptor.parameters) }

    fun getParsedCommandArgumentsWithBasicValidation(): Map<String, Any> {
        YAMLConfigParser.reload()
        val configFileParsedValues =
            commandDescriptor.parameters
                .mapNotNull { parameter ->
                    val value = YAMLConfigParser.getValue(parameter.configFilePath)
                    value?.let { parameter.name to it }
                }.toMap()

        val resultArguments = configFileParsedValues + parsed.values

        val missingRequired =
            commandDescriptor.parameters
                .filter { it.isRequired && it.name !in resultArguments.keys }

        if (missingRequired.isNotEmpty()) {
            throw RequiredParameterMissing(
                usageInstruction = commandDescriptor.usageInstruction,
                parameterNames = missingRequired.map { it.name },
                yamlExamples = missingRequired.map { it.configFilePath },
            )
        }

        return resultArguments
    }
}
