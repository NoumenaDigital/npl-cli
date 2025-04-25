package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.SourcesManager.collectSourcesFromDirectory
import com.noumenadigital.npl.contrib.NplContribConfiguration
import com.noumenadigital.npl.lang.CompileFailure
import com.noumenadigital.npl.lang.CompilerConfiguration
import com.noumenadigital.npl.lang.Loader
import com.noumenadigital.npl.lang.Proto
import com.noumenadigital.npl.lang.Source
import com.noumenadigital.npl.lang.Type
import java.time.Duration

data class CompilerService(
    private val sourcesDir: String,
    private val nplContribLibrary: String = "$sourcesDir/npl-contrib",
) {
    fun compileAndReport(output: ColorWriter): CompilationResult {
        val nplSources = collectSourcesFromDirectory(sourcesDir)
        val compileResult = compileSource(nplSources, output)
        reportCompilationResults(nplSources.size, compileResult, output)
        output.info()
        return compileResult
    }

    private fun compileSource(
        sources: List<Source>,
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
        if (result.hasErrors) return // Early exit if there are errors

        val filePluralization = if (sourceCount > 1) "s" else ""
        val warningPluralization = if (result.warningCount > 1) "s" else ""

        val warningMessagePart =
            when {
                result.hasWarnings -> " with ${result.warningCount} warning$warningPluralization"
                else -> ""
            }

        val successMessage =
            "Completed compilation for $sourceCount file$filePluralization$warningMessagePart in ${result.duration} ms"

        if (result.hasWarnings) {
            output.warning(successMessage)
        } else {
            output.success(successMessage)
        }
    }

    private fun compile(
        sources: List<Source>,
        nplContribLibrary: String,
        output: ColorWriter,
    ): CompilationResult {
        if (sources.isEmpty()) {
            output.error("No NPL source files found")
            return CompilationResult(0, 1, 0, false)
        }

        val compilerConfiguration =
            compilerConfiguration(nplContribLibrary)

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

    fun compilerConfiguration(nplContribLibrary: String): CompilerConfiguration {
        val compilerConfiguration =
            CompilerConfiguration(
                tag = null,
                quirksMode = false, // TODO: is this what we want?
                nplContribConfiguration = NplContribConfiguration(nplContribPath = nplContribLibrary),
            )
        return compilerConfiguration
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
