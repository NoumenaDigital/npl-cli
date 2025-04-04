package com.noumenadigital.npl.cli.commands.impl

import com.noumenadigital.npl.cli.commands.NplCommand
import java.io.Writer

object VersionCommand : NplCommand {
    override val commandDescription = "Command to return current npl cli version"

    override fun execute(output: Writer) {
        output.write("I'm v1.0")
    }
}
