package com.noumenadigital.npl.cli.service

import java.io.OutputStreamWriter
import java.io.Writer

sealed interface CommandExecutorOutput {
    fun get(): Writer
}

data object NplWriterOutput : CommandExecutorOutput {
    override fun get(): Writer = OutputStreamWriter(System.out, Charsets.UTF_8)
}
