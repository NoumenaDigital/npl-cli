package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.exception.RequiredParameterMissing

sealed interface CommandParameter {
    val name: String
    val description: String
    val defaultValue: String?
    val isRequired: Boolean
    val isHidden: Boolean
}

data class NamedParameter(
    override val name: String,
    override val description: String,
    override val defaultValue: String? = null,
    override val isRequired: Boolean = false,
    override val isHidden: Boolean = false,
    val valuePlaceholder: String? = null,
) : CommandParameter {
    init {
        require(name.startsWith("--")) { "Named parameters must start with '--'" }
    }

    val takesValue: Boolean = valuePlaceholder != null
}

data class PositionalParameter(
    override val name: String,
    override val description: String,
    override val defaultValue: String? = null,
    override val isRequired: Boolean = false,
    override val isHidden: Boolean = false,
) : CommandParameter {
    init {
        require(!name.startsWith("--")) { "Positional parameters must not start with '--'" }
    }
}

/**
 * Simple command line argument parser supporting "--param value" format.
 */
object CommandArgumentParser {
    fun parse(
        args: List<String>,
        parameters: List<CommandParameter>,
    ): ParsedArguments {
        val paramDefs =
            parameters
                .filterIsInstance<NamedParameter>()
                .associateBy { it.name }

        val parsed = mutableMapOf<String, String>()
        val consumedIndices = mutableSetOf<Int>()

        // Pass 1: Consume "--param value" pairs
        for (i in 0 until args.size - 1) {
            if (i in consumedIndices) continue

            val arg = args[i]
            val nextArg = args[i + 1]
            val paramDef = if (arg.startsWith("--")) paramDefs[arg] else null

            if (paramDef != null && paramDef.takesValue && !nextArg.startsWith("--")) {
                parsed[arg] = nextArg
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
                parsed[arg] = ""
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

        fun getRequiredValue(name: String): String = values[name] ?: throw RequiredParameterMissing(name)
    }
}
