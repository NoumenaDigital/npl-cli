package com.noumenadigital.npl.cli.util

import java.io.File

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

    return if (dirPath.startsWith(currentPath)) {
        dirPath.replace(currentPath.plus("/"), "")
    } else {
        absolutePath
    }
}
