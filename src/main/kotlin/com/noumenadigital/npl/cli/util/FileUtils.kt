package com.noumenadigital.npl.cli.util

import java.io.File
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

fun File.relativeOrAbsolute(): String {
    val dirPath = normalize().absolutePath
    val currentPath = File(".").normalize().absolutePath

    return if (dirPath != currentPath && dirPath.startsWith(currentPath)) {
        Paths.get(currentPath).relativize(Paths.get(dirPath)).toString()
    } else {
        absolutePath
    }
}
