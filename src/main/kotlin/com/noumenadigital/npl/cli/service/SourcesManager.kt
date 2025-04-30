package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.lang.Source
import java.io.File
import java.nio.file.Files

class SourcesManager(
    private val projectDirectoryPath: String,
) {
    companion object {
        private const val NPL_EXTENSION = ".npl"
    }

    val nplContribLibrary: String = "$projectDirectoryPath/npl-contrib"

    fun getNplSources(): List<Source> {
        collectSourcesFromDirectory(projectDirectoryPath).let { sources ->
            if (sources.isEmpty()) {
                throw CommandExecutionException("No NPL source files found")
            }
            return sources
        }
    }

    private fun collectSourcesFromDirectory(directoryPath: String): List<Source> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw CommandExecutionException("Directory $directoryPath does not exist")
        }
        val sources = mutableListOf<Source>()
        Files
            .walk(dir.toPath())
            .filter { Files.isRegularFile(it) && it.toString().endsWith(NPL_EXTENSION) }
            .sorted()
            .forEach {
                sources.add(Source.create(it.toUri().toURL()))
            }
        return sources
    }
}
