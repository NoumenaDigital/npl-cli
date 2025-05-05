package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter

sealed interface CommandParameter {
    val name: String
    val description: String
    val defaultValue: String?
    val isRequired: Boolean
}

data class NamedParameter(
    override val name: String,
    override val description: String,
    override val defaultValue: String? = null,
    override val isRequired: Boolean = false,
    val valuePlaceholder: String? = null, // e.g., "<value>"
) : CommandParameter {
    init {
        require(name.startsWith("--")) { "Named parameters must start with '--'" }
    }
}

data class PositionalParameter(
    override val name: String,
    override val description: String,
    override val defaultValue: String? = null,
    override val isRequired: Boolean = false,
) : CommandParameter {
    init {
        require(!name.startsWith("--")) { "Positional parameters must not start with '--'" }
    }
}

sealed interface CommandExecutor {
    val commandName: String
    val description: String

    val parameters: List<CommandParameter>
        get() = emptyList()

    fun createInstance(params: List<String>): CommandExecutor = this

    fun execute(output: ColorWriter): ExitCode
}
