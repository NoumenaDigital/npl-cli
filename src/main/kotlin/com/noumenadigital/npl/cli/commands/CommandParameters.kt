package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.exception.RequiredParameterMissing

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

        fun getRequiredValue(name: String): String = values[name] ?: throw RequiredParameterMissing(name)
    }
}
