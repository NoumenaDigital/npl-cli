package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.util.ColorWriter
import com.noumenadigital.npl.lang.CompileFailure
import com.noumenadigital.npl.lang.CompilerConfiguration
import com.noumenadigital.npl.lang.Loader
import com.noumenadigital.npl.lang.Source
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

data class CheckCommand(
    private val useColor: Boolean = true,
    private val baseDir: Path? = null,
) : CommandExecutor {
    companion object {
        const val COMMAND_DESCRIPTION =
            "Validate the correctness of the NPL sources (excluding test sources) in the current directory."
        private const val NPL_EXTENSION = ".npl"
    }

    override val commandName: String = "check"

    override fun execute(output: Writer): ExitCode {
        if (useColor) {
            AnsiConsole.systemInstall()
        }

        val colorOutput = ColorWriter(output, useColor)
        var sourceDirectoryMissing = false

        try {
            val defaultSourceDir = "src/main/npl"
            var sourceDir = defaultSourceDir
            val defaultSourceDirExists = resolveFile(defaultSourceDir).exists()

            if (!defaultSourceDirExists) {
                // Look for src/main/npl* directories and select the one with highest number
                val nplDirs = findNplVersionDirectories()
                if (nplDirs.isNotEmpty()) {
                    sourceDir = nplDirs.last()
                }
            }

            val sourceDirExists = resolveFile(sourceDir).exists()

            colorOutput.writeln("Looking for NPL files in $sourceDir")

            val mainResult =
                if (sourceDirExists) {
                    checkDirectory(sourceDir, colorOutput)
                } else {
                    colorOutput.writeln("No NPL source files found\n")
                    sourceDirectoryMissing = true
                    CompilationResult(0, 0, 0, true)
                }

            // Make sure we have exactly ONE newline before the result message
            if (sourceDirExists) {
                colorOutput.write("\n")
            }

            when {
                mainResult.hasErrors -> {
                    colorOutput.writeln("NPL check failed with errors.")
                    return ExitCode.COMPILATION_ERROR
                }
                mainResult.hasWarnings || sourceDirectoryMissing -> {
                    colorOutput.writeln("NPL check completed with warnings.")
                    return ExitCode.GENERAL_ERROR
                }
                else -> {
                    colorOutput.writeln("NPL check completed successfully.")
                    return ExitCode.SUCCESS
                }
            }
        } catch (e: Exception) {
            colorOutput.writeln("\nNPL check failed: ${e.message}")
            throw CommandExecutionException("Failed to run NPL check: ${e.message}", e)
        } finally {
            if (useColor) {
                AnsiConsole.systemUninstall()
            }
        }
    }

    private fun findNplVersionDirectories(): List<String> {
        val srcMainDir = resolveFile("src/main")
        if (!srcMainDir.exists() || !srcMainDir.isDirectory) {
            return emptyList()
        }

        val nplDirPattern = "npl.*".toRegex()
        return srcMainDir
            .listFiles()
            ?.filter { it.isDirectory && nplDirPattern.matches(it.name) }
            ?.map { "src/main/${it.name}" }
            ?.sortedBy { dir ->
                // Extract version number if present
                val versionMatch = "npl-([0-9.]+)".toRegex().find(dir)
                versionMatch?.groupValues?.get(1) ?: dir
            } ?: emptyList()
    }

    private fun resolveFile(path: String): File = baseDir?.resolve(path)?.toFile() ?: File(path)

    private fun checkDirectory(
        directory: String,
        output: ColorWriter,
    ): CompilationResult {
        val dir = resolveFile(directory)
        if (!dir.exists() || !dir.isDirectory) {
            output.writeln("No NPL source files found\n")
            return CompilationResult(0, 0, 0, true)
        }

        val sources = collectNplSources(dir)

        if (sources.isEmpty()) {
            output.writeln("No NPL source files found\n")
            return CompilationResult(0, 0, 0, true)
        }

        val startTime = System.nanoTime()

        val compileResult = compile(sources, output)

        val duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis()

        if (compileResult.hasErrors) {
            return compileResult
        } else {
            val warningText =
                if (compileResult.hasWarnings) {
                    " with ${compileResult.warningCount} warning" +
                        (if (compileResult.warningCount > 1) "s" else "")
                } else {
                    ""
                }
            output.writeln(
                "Completed compilation for ${sources.size} file" + (if (sources.size > 1) "s" else "") + "$warningText in $duration ms",
            )
            return compileResult
        }
    }

    private fun collectNplSources(directory: File): List<Source> {
        val sources = mutableListOf<Source>()

        try {
            Files
                .walk(directory.toPath())
                .filter { Files.isRegularFile(it) && it.toString().endsWith(NPL_EXTENSION) }
                .forEach { sources.add(Source.create(it.toUri().toURL())) }
        } catch (e: Exception) {
            // Just return what we have so far
        }

        return sources
    }

    private fun compile(
        sources: List<Source>,
        output: ColorWriter,
    ): CompilationResult {
        if (sources.isEmpty()) {
            return CompilationResult(0, 0, 0, true)
        }

        val compilerConfiguration =
            CompilerConfiguration(
                tag = null,
                quirksMode = false, // TODO: is this what we want?
            )

        val compileResult =
            try {
                Loader(compilerConfiguration).loadPackages(sources)
            } catch (e: Exception) {
                throw CommandExecutionException("Failed to compile NPL sources: ${e.message}", e)
            }

        var errorCount = 0
        val warningCount: Int

        when (compileResult) {
            is CompileFailure -> {
                errorCount = compileResult.errors.size
                warningCount = compileResult.warnings.size

                if (warningCount > 0) {
                    compileResult.warnings.forEach { warning ->
                        output.yellowln(warning.description)
                    }
                }

                compileResult.errors.forEach { error ->
                    output.redln(error.description)
                }
            }
            else -> {
                // Only warnings, no errors
                warningCount = compileResult.warnings.size
                compileResult.warnings.forEach { warning ->
                    output.yellowln(warning.description)
                }
            }
        }

        return CompilationResult(sources.size, errorCount, warningCount, errorCount == 0)
    }

    private data class CompilationResult(
        val fileCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val success: Boolean,
    ) {
        val hasErrors: Boolean
            get() = errorCount > 0

        val hasWarnings: Boolean
            get() = warningCount > 0
    }
}
