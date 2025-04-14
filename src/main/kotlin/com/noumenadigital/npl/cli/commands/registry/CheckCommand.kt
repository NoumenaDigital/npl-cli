package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.lang.CompileFailure
import com.noumenadigital.npl.lang.CompilerConfiguration
import com.noumenadigital.npl.lang.Loader
import com.noumenadigital.npl.lang.Source
import org.fusesource.jansi.Ansi
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

    private fun formatWithColor(
        text: String,
        colorizer: (Ansi) -> Ansi,
    ): String =
        if (useColor) {
            colorizer(Ansi.ansi()).a(text).reset().toString()
        } else {
            text
        }

    override fun execute(output: Writer): ExitCode {
        if (useColor) {
            AnsiConsole.systemInstall()
        }

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

            output.write("Looking for NPL files in $sourceDir\n")

            val mainResult =
                if (sourceDirExists) {
                    checkDirectory(sourceDir, output)
                } else {
                    output.write("No NPL source files found\n\n")
                    sourceDirectoryMissing = true
                    CompilationResult(0, 0, 0, true)
                }

            // Make sure we have exactly ONE newline before the result message
            if (sourceDirExists) {
                output.write("\n")
            }

            when {
                mainResult.hasErrors -> {
                    output.write("NPL check failed with errors.\n")
                    return ExitCode.COMPILATION_ERROR
                }
                mainResult.hasWarnings || sourceDirectoryMissing -> {
                    output.write("NPL check completed with warnings.\n")
                    return ExitCode.GENERAL_ERROR
                }
                else -> {
                    output.write("NPL check completed successfully.\n")
                    return ExitCode.SUCCESS
                }
            }
        } catch (e: Exception) {
            output.write("\nNPL check failed: ${e.message}\n")
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
        output: Writer,
    ): CompilationResult {
        val dir = resolveFile(directory)
        if (!dir.exists() || !dir.isDirectory) {
            output.write("No NPL source files found\n\n")
            return CompilationResult(0, 0, 0, true)
        }

        val sources = collectNplSources(dir)

        if (sources.isEmpty()) {
            output.write("No NPL source files found\n\n")
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
            output.write(
                "Completed compilation for ${sources.size} file" + (if (sources.size > 1) "s" else "") + "$warningText in $duration ms\n",
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
        output: Writer,
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
                        output.write(formatWithColor(warning.description, Ansi::fgYellow) + "\n")
                    }
                }

                compileResult.errors.forEach { error ->
                    output.write(formatWithColor(error.description, Ansi::fgRed) + "\n")
                }
            }
            else -> {
                // Only warnings, no errors
                warningCount = compileResult.warnings.size
                compileResult.warnings.forEach { warning ->
                    output.write(formatWithColor(warning.description, Ansi::fgYellow) + "\n")
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
