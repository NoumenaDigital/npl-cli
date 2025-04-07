package com.noumenadigital.npl.cli.commands.registry

import java.io.Writer

sealed interface NplCommand {
    val commandDescription: String

    fun execute(output: Writer)
}
