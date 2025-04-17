package com.noumenadigital.npl.cli

import java.io.OutputStreamWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandProcessor().process(args.toList(), OutputStreamWriter(System.out))
    exitProcess(exitCode.code)
}
