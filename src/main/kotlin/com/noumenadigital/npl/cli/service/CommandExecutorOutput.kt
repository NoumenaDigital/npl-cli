package com.noumenadigital.npl.cli.service

import java.io.OutputStreamWriter
import java.io.Writer

interface CommandExecutorOutput {
    fun get(): Writer
}

class NplWriterOutput : CommandExecutorOutput {
    override fun get(): Writer = OutputStreamWriter(System.out, Charsets.UTF_8)
}
