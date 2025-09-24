package com.noumenadigital.npl.cli.commands

data class EnvironmentVariable(
    val name: String,
    val description: String,
    val isRequired: Boolean = false,
) {
    init {
        require(name == name.uppercase()) { "Environment variable name '$name' must be upper case" }
    }

    val type: String = "Environment Variable"
}
