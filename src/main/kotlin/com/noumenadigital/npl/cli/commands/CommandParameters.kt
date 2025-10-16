package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.exception.ArgumentParsingException

data class NamedParameter(
    val name: String,
    val description: String,
    val defaultValue: String? = null,
    val isRequired: Boolean = false,
    val isHidden: Boolean = false,
    val valuePlaceholder: String? = null,
    val takesPath: Boolean = false,
    val isRequiredForMcp: Boolean = isRequired && !isHidden,
    val configFilePath: String = "/$name",
) {
    init {
        require(!name.startsWith("--")) { "Named parameters should not start with '--' in definition" }
    }

    val takesValue: Boolean = valuePlaceholder != null

    val cliName: String = "--$name"
}

object DeprecationNotifier {
    private val sink: ThreadLocal<((String) -> Unit)?> = ThreadLocal()

    fun setSink(handler: ((String) -> Unit)?) {
        if (handler == null) {
            sink.remove()
        } else {
            sink.set(handler)
        }
    }

    fun warn(message: String) {
        val handler = sink.get()
        if (handler != null) {
            handler(message)
        } else {
            System.err.println(message)
        }
    }
}

/**
 * Simple command line argument parser supporting "--param value" format.
 */
object CommandArgumentParser {
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
        val values: Map<String, String>,
        val unexpectedArgs: List<String>,
    ) {
        fun hasFlag(name: String): Boolean = values.containsKey(name)

        fun getValue(name: String): String? = values[name]

        fun getValueOrElse(
            name: String,
            deprecatedNames: List<String>,
            defaultValue: String?,
        ): String? {
            val canonical = values[name]
            if (canonical != null) return canonical

            if (deprecatedNames.isEmpty()) return defaultValue

            val tokens = unexpectedArgs
            for (i in tokens.indices) {
                val token = tokens[i]
                if (!token.startsWith("--")) continue
                val withoutDashes = token.removePrefix("--")
                if (withoutDashes in deprecatedNames) {
                    val value =
                        if (i + 1 < tokens.size && !tokens[i + 1].startsWith("--")) {
                            tokens[i + 1]
                        } else {
                            ""
                        }
                    return value
                }
            }

            return defaultValue
        }

        fun withoutDeprecatedUnexpectedNames(deprecatedNames: Set<String>): ParsedArguments {
            if (deprecatedNames.isEmpty() || unexpectedArgs.isEmpty()) return this
            val tokens = unexpectedArgs
            val filtered = mutableListOf<String>()
            var i = 0
            while (i < tokens.size) {
                val t = tokens[i]
                if (t.startsWith(prefix = "--")) {
                    val paramName = t.removePrefix(prefix = "--")
                    if (paramName in deprecatedNames) {
                        val kebabCase = paramName.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()
                        DeprecationNotifier.warn("Parameter '--$paramName' is deprecated; use '--$kebabCase' instead.")
                        i +=
                            if (i + 1 < tokens.size && !tokens[i + 1].startsWith(prefix = "--")) {
                                2
                            } else {
                                1
                            }
                        continue
                    }
                }
                filtered.add(t)
                i += 1
            }
            return ParsedArguments(values, filtered)
        }
    }
}
