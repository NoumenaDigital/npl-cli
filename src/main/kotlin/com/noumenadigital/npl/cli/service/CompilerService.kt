package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.contrib.NplContribConfiguration
import com.noumenadigital.npl.lang.CompileFailure
import com.noumenadigital.npl.lang.CompilerConfiguration
import com.noumenadigital.npl.lang.Loader
import com.noumenadigital.npl.lang.Proto
import com.noumenadigital.npl.lang.Source
import com.noumenadigital.npl.lang.Type
import java.io.File
import java.nio.file.Files
import java.time.Duration

object CompilerService {
    private const val NPL_EXTENSION = ".npl"

    fun compileAndReport(
        sourcesDir: String,
        nplContribLibrary: String = "$sourcesDir/npl-contrib",
        output: ColorWriter,
    ): CompilationResult {
        val nplSources = collectSourcesFromDirectory(sourcesDir, output)
        if (nplSources.isEmpty()) {
            return CompilationResult(0, 0, 1, true)
        }
        val compileResult = compileSource(nplSources, nplContribLibrary, output)
        reportCompilationResults(nplSources.size, compileResult, output)
        output.info()
        return compileResult
    }

    private fun compileSource(
        sources: List<Source>,
        nplContribLibrary: String,
        output: ColorWriter,
    ): CompilationResult {
        val startTime = System.nanoTime()
        val compileResult = compile(sources, nplContribLibrary, output)
        val duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis()

        compileResult.duration = duration
        return compileResult
    }

    private fun reportCompilationResults(
        sourceCount: Int,
        result: CompilationResult,
        output: ColorWriter,
    ) {
        if (!result.hasErrors) {
            val warningText =
                if (result.hasWarnings) {
                    " with ${result.warningCount} warning" +
                        (if (result.warningCount > 1) "s" else "")
                } else {
                    ""
                }

            val successMessage =
                "Completed compilation for $sourceCount file" +
                    (if (sourceCount > 1) "s" else "") +
                    "$warningText in ${result.duration} ms"

            if (result.hasWarnings) {
                output.info(successMessage)
            } else {
                output.success(successMessage)
            }
        }
    }

    private fun collectNplSources(directory: File): List<Source> {
        val sources = mutableListOf<Source>()

        try {
            Files
                .walk(directory.toPath())
                .filter { Files.isRegularFile(it) && it.toString().endsWith(NPL_EXTENSION) }
                .sorted()
                .forEach { sources.add(Source.create(it.toUri().toURL())) }
        } catch (_: Exception) {
            // Just return what we have so far
        }

        return sources
    }

    private fun compile(
        sources: List<Source>,
        nplContribLibrary: String,
        output: ColorWriter,
    ): CompilationResult {
        if (sources.isEmpty()) {
            return CompilationResult(0, 0, 1, true)
        }

        val compilerConfiguration =
            CompilerConfiguration(
                tag = null,
                quirksMode = false, // TODO: is this what we want?
                nplContribConfiguration = NplContribConfiguration(nplContribPath = nplContribLibrary),
            )

        val compileResult =
            try {
                Loader(compilerConfiguration).loadPackages(sources)
            } catch (e: Exception) {
                throw CommandExecutionException("Failed to compile NPL sources: ${e.message}", e)
            }

        var errorCount = 0
        val warningCount: Int
        var protos: List<Proto<Type>> = emptyList()

        when (compileResult) {
            is CompileFailure -> {
                errorCount = compileResult.errors.size
                warningCount = compileResult.warnings.size

                if (warningCount > 0) {
                    compileResult.warnings.forEach { warning ->
                        output.warning(warning.description)
                    }
                }

                compileResult.errors.forEach { error ->
                    output.error(error.description)
                }
            }

            else -> {
                // Only warnings, no errors
                warningCount = compileResult.warnings.size
                compileResult.warnings.forEach { warning ->
                    output.warning(warning.description)
                }
                protos = compileResult.throwOnError().protos.all
            }
        }

        return CompilationResult(sources.size, errorCount, warningCount, errorCount == 0, protos = protos)
    }

    private fun collectSourcesFromDirectory(
        directory: String,
        output: ColorWriter,
    ): List<Source> {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) {
            output.info("No NPL source files found\n")
            return emptyList()
        }

        val sources = collectNplSources(dir)

        if (sources.isEmpty()) {
            output.info("No NPL source files found\n")
        }

        return sources
    }

    data class CompilationResult(
        val fileCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val success: Boolean,
        var duration: Long = 0,
        val protos: List<Proto<Type>>? = null,
    ) {
        val hasErrors: Boolean
            get() = errorCount > 0

        val hasWarnings: Boolean
            get() = warningCount > 0
    }
}
