package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.util.normalizeWindowsPath
import com.noumenadigital.npl.lang.Source
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

class SourcesManager(
    private val srcPath: String,
) {
    companion object {
        private const val NPL_EXTENSION = ".npl"
        private const val CONTRIB_PATH = "npl-contrib"
    }

    val nplContribLibrary: String = File(srcPath, CONTRIB_PATH).path

    fun getNplSources(): List<Source> {
        collectSourcesFromDirectory(srcPath).let { sources ->
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

    fun getArchivedSources(): ByteArray {
        val migrationFile = findMigrationFile("migration.yml")
        val basePath = Paths.get(migrationFile.parent)
        val outputStream = ByteArrayOutputStream()

        ZipOutputStream(outputStream).use { zipOut ->
            Files.walk(basePath).forEach { path ->
                if (Files.isRegularFile(path)) {
                    val relativePath = basePath.relativize(path).toString().normalizeWindowsPath()
                    zipOut.putNextEntry(ZipEntry(relativePath))
                    Files.newInputStream(path).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
        return outputStream.toByteArray()
    }

    fun findMigrationFile(fileName: String): File {
        val srcDir = Paths.get(srcPath).toFile()

        if (!srcDir.exists() || !srcDir.isDirectory) {
            throw CommandExecutionException("Source path '$srcPath' does not exist or is not a directory.")
        }
        val matchedFiles =
            Files.walk(Paths.get(srcPath)).use { paths ->
                paths
                    .asSequence()
                    .filter { it.isRegularFile() && it.fileName.toString() == fileName }
                    .map(Path::toFile)
                    .toList()
            }

        return when {
            matchedFiles.isEmpty() -> throw CommandExecutionException("No '$fileName' file found in $srcPath")
            matchedFiles.size > 1 -> throw CommandExecutionException(
                "Multiple '$fileName' files found:\n${matchedFiles.joinToString("\n")}",
            )
            else -> matchedFiles.first()
        }
    }
}
