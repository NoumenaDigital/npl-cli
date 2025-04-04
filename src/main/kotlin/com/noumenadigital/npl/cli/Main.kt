package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.NplWriterOutput
import com.noumenadigital.npl.cli.service.impl.NplCommandExecutor


fun main(args: Array<String>) {
    NplCommandExecutor().process(args.toList(), NplWriterOutput())
}



