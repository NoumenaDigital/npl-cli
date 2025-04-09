package com.noumenadigital.npl.cli.model

import java.io.Writer

data class CommandContext(
    val output: Writer,
    val params: List<String>? = emptyList(),
)
