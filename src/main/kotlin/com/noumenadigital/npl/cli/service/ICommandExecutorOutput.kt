package com.noumenadigital.npl.cli.service

import java.io.OutputStreamWriter
import java.io.Writer

interface ICommandExecutorOutput {
    fun get(): Writer
}

class NplWriterOutput : ICommandExecutorOutput {
    override fun get(): Writer {
        return OutputStreamWriter(System.out, Charsets.UTF_8)
    }
}
