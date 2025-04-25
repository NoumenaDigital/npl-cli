package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.lang.Source
import java.io.File
import java.nio.file.Files

object SourcesManager {
    private const val NPL_EXTENSION = ".npl"

    fun collectSourcesFromDirectory(directory: String): List<Source> {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) {
            throw CommandExecutionException("No NPL source files found")
        }
        val sources = mutableListOf<Source>()
        Files
            .walk(dir.toPath())
            .filter { Files.isRegularFile(it) && it.toString().endsWith(NPL_EXTENSION) }
            .sorted()
            .forEach {
                sources.add(Source.create(it.toUri().toURL()))
            }
        if (sources.isEmpty()) {
            throw CommandExecutionException("No NPL source files found")
        }
        return sources
    }
}
