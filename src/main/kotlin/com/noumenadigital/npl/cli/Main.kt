package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.impl.NplCommandExecutor
import java.io.OutputStreamWriter


fun main(args: Array<String>) {
    val output = OutputStreamWriter(System.out, Charsets.UTF_8)
    NplCommandExecutor().process(args.toList(), output)
}



