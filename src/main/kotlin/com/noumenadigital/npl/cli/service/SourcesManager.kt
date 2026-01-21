package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.util.normalizeWindowsPath
import com.noumenadigital.npl.contrib.DefaultNplContribLoader
import com.noumenadigital.npl.contrib.NplContribConfiguration
import com.noumenadigital.npl.lang.Source
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SourcesManager(
    private val srcPath: String,
    private val contribLibraries: List<String>? = null,
    private val testSrcPath: String = srcPath,
) {
    companion object {
        private const val NPL_EXTENSION = ".npl"
        private const val CONTRIB_PATH = "npl-contrib"
    }

    val nplContribLibrary: String = File(srcPath, CONTRIB_PATH).path

    fun getNplContribLibConfiguration(): NplContribConfiguration =
        NplContribConfiguration(
            nplContribPath = nplContribLibrary,
        )

    fun getNplSources(): List<Source> {
        collectSourcesFromDirectory(srcPath).let { sources ->
            if (sources.isEmpty()) {
                throw CommandExecutionException("No NPL source files found")
            }
            val contribSources =
                getFileObjectFromByteArray().use {
                    DefaultNplContribLoader.extractNplContribLibSources(
                        contribLibraries ?: emptyList(),
                        it,
                    )
                }

            return sources + contribSources
        }
    }

    fun getNplTestSources(): List<Source> {
        collectSourcesFromDirectory(testSrcPath).let { sources ->
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
        val basePath = Paths.get(srcPath)
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

    fun getFileObjectFromByteArray(): FileObject {
        val archivedSources = getArchivedSources()
        val tempFile = Files.createTempFile("vfs-archive", ".zip")
        tempFile.toFile().deleteOnExit()
        Files.write(tempFile, archivedSources)
        val archiveUri = "zip:${tempFile.toUri()}!/"
        val archiveRoot = VFS.getManager().resolveFile(archiveUri)
        return archiveRoot
    }
}
