package com.noumenadigital.npl.cli

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object TestUtils {
    data class TestContext(
        val commands: List<String>,
        val workingDirectory: File = File("."),
        val process: Process = buildProcess(commands, workingDirectory),
        val output: String =
            process.inputStream
                .bufferedReader()
                .readText()
                .trimIndent(),
    )

    private fun buildProcess(
        commands: List<String>,
        workingDirectory: File,
    ): Process =
        ProcessBuilder(getNplExecutable(), *commands.toTypedArray())
            .directory(workingDirectory)
            .redirectErrorStream(true)
            .start()

    fun getTestResourcesPath(subPath: List<String> = emptyList()): Path {
        val rootDir = File("..").canonicalFile
        return Paths
            .get(rootDir.toString(), "test-resources", "npl-sources", *subPath.toTypedArray())
            .toAbsolutePath()
            .normalize()
    }

    fun getNplExecutable(): String {
        val rootDir = File(".").canonicalFile
        return rootDir.resolve("target/npl").absolutePath
    }

    fun String.normalize(): String =
        replace("\r\n", "\n")
            // Normalize durations
            .replace(Regex("in \\d+ ms"), "in XXX ms")
            // Remove any ANSI color codes
            .replace(Regex("\\e\\[[0-9;]*m"), "")
            .trimIndent()

    fun runCommand(
        commands: List<String>,
        test: TestContext.() -> Unit,
    ) {
        TestContext(commands = commands).apply(test)
    }
}
