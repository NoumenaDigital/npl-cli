package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.ColorWriter
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandProcessor().process(args.toList(), ColorWriter(OutputStreamWriter(System.out), true))
    exitProcess(exitCode.code)
}
