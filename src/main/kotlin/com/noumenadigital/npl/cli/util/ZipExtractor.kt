package com.noumenadigital.npl.cli.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipExtractor {
    companion object {
        fun unzip(
            archiveFile: File,
            skipTopDirectory: Boolean = false,
        ) = apply {
            val targetDir = archiveFile.parentFile
            ZipInputStream(FileInputStream(archiveFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val relativePath =
                        entry.name.let {
                            if (!skipTopDirectory) it else it.split("/").drop(1).joinToString("/")
                        }
                    if (relativePath.isBlank()) {
                        entry = zipIn.nextEntry
                        continue
                    }

                    targetDir.extractZipEntry(relativePath, entry, zipIn)
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        private fun File.extractZipEntry(
            relativePath: String,
            entry: ZipEntry,
            zipIn: ZipInputStream,
        ) {
            val filePath = File(this, relativePath)
            if (entry.isDirectory) {
                filePath.mkdirs()
            } else {
                filePath.parentFile.mkdirs()
                FileOutputStream(filePath).use { out ->
                    zipIn.copyTo(out)
                }
            }
        }
    }
}
