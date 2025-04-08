package com.noumenadigital.npl.cli

import java.io.OutputStreamWriter

fun main(args: Array<String>) {
    NplCommandExecutor().process(args.toList(), OutputStreamWriter(System.out))
}
