package com.noumenadigital.npl.cli.commands.impl

import com.noumenadigital.npl.cli.commands.INplCommand
import java.io.Writer

class HelloCommand : INplCommand {
    override fun execute(output: Writer) {
        output.write("Hello")
    }
}