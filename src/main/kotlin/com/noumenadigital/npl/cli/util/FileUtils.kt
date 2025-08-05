package com.noumenadigital.npl.cli.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {
    fun findFiles(
        fileName: String,
        startDir: File = File("."),
    ): List<File> =
        startDir
            .walkTopDown()
            .filter { it.isFile && it.name.equals(fileName, ignoreCase = true) }
            .toList()
}

fun File.relativeToCurrentOrAbsolute(): String = relativeToOrNull(File("."))?.path?.takeIf { it.isNotBlank() } ?: absolutePath

fun Path.relativeToCurrent(): Path =
    Paths
        .get(".")
        .toAbsolutePath()
        .normalize()
        .relativize(this)
