package com.noumenadigital.npl.cli.commands

import java.io.Writer

interface INplCommand {

    fun execute(output: Writer){
        output.write("Not implemented yet")
    }

}
