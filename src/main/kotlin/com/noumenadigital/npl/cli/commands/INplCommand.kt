package com.noumenadigital.npl.cli.commands

import java.io.Writer

interface INplCommand {
    val commandDescription: String

    fun execute(output: Writer)

}
