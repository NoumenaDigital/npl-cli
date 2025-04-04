package com.noumenadigital.npl.cli.commands.impl

import com.noumenadigital.npl.cli.commands.NplCommand
import java.io.Writer

object HelloCommand : NplCommand {
    override val commandDescription = "Command to return hello string"

    override fun execute(output: Writer) {
        output.write("Hello")
    }
}
