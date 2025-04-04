package com.noumenadigital.npl.cli.commands.impl

import com.noumenadigital.npl.cli.commands.INplCommand
import java.io.Writer

class VersionCommand : INplCommand {
    override fun execute(output: Writer) {
        output.write("I'm v1.0")
    }
}