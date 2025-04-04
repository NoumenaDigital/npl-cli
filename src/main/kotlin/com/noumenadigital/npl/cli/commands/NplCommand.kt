package com.noumenadigital.npl.cli.commands

import java.io.Writer

interface NplCommand {
    val commandDescription: String

    fun execute(output: Writer)
}
