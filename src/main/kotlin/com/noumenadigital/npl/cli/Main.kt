package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.NplWriterOutput

fun main(args: Array<String>) {
    NplCommandExecutor().process(args.toList(), NplWriterOutput)
}
