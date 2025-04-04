package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.impl.NplCommandExecutor
import java.io.OutputStreamWriter


fun main(args: Array<String>) {
    NplCommandExecutor().process(args.toList(), OutputStreamWriter(System.out, Charsets.UTF_8))
}



