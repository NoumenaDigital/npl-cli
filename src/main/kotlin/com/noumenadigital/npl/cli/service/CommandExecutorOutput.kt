package com.noumenadigital.npl.cli.service

import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets

sealed interface CommandExecutorOutput {
    fun get(): Writer
}

data object NplWriterOutput : CommandExecutorOutput {
    private val writer: Writer by lazy {
        OutputStreamWriter(System.out, StandardCharsets.UTF_8)
    }

    override fun get(): Writer = writer
}
