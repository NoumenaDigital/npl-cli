package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.service.TestHarness
import com.noumenadigital.npl.testing.coverage.CoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.LineCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.NoCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.SonarQubeReporter
import org.tap4j.util.StatusValues
import java.io.File
import java.time.Duration

data class TestCommand(
    private val params: List<String> = emptyList(),
) : CommandExecutor {
    companion object {
        const val MIN_PADDING = 25
        const val COVERAGE_FILE = "coverage.xml"
    }

    override val commandName: String = "test"
    override val description: String = "Run the NPL tests"

    override val parameters: List<CommandParameter> =
        listOf(
            NamedParameter(
                name = "--sourceDir",
                description = "Source directory containing NPL tests to run",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
            ),
            NamedParameter(
                name = "--coverage",
                description = "Report test coverage details (printed to console as well as coverage.xml)",
                isRequired = false,
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = TestCommand(params)

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val parser = CommandArgumentParser()
            val parsedArgs = parser.parse(params, parameters)

            if (parsedArgs.unexpectedArgs.isNotEmpty()) {
                output.error("Unknown arguments: ${parsedArgs.unexpectedArgs.joinToString(" ")}")
                return ExitCode.USAGE_ERROR
            }

            val sourcePath = parsedArgs.getValue("--sourceDir") ?: "."
            val sourceDir = File(sourcePath)
            if (!sourceDir.isDirectory || !sourceDir.exists()) {
                output.error("Given source directory is either not a directory or does not exist. ${sourceDir.canonicalPath}")
                return ExitCode.GENERAL_ERROR
            }

            val showCoverage = parsedArgs.hasFlag("--coverage")
            val coverageAnalyzer: CoverageAnalyzer = coverageAnalyzer(showCoverage, sourceDir)
            val testHarness = TestHarness(SourcesManager(sourceDir.absolutePath), coverageAnalyzer)

            val start = System.nanoTime()
            val testResults = testHarness.runTest()
            handleResults(testResults, output)
            val isResultSuccess = testResults.all { !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut() }
            if (!isResultSuccess) {
                output.error("\nNPL test failed with errors.")
                return ExitCode.DATA_ERROR
            }
            if (testResults.all { it.tapResult.testResults.isEmpty() }) {
                output.error("No NPL tests found.")
                return ExitCode.DATA_ERROR
            }

            coverageAnalyzer.writeSummary(output::info)

            output.success(
                "\nNPL test completed successfully in ${
                    Duration.ofNanos(System.nanoTime() - start).toMillis()
                } ms.",
            )
            return ExitCode.SUCCESS
        } catch (e: CommandExecutionException) {
            output.error(e.message)
            return ExitCode.GENERAL_ERROR
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL test: ${e.message}", e)
        }
    }

    private fun coverageAnalyzer(
        showCoverage: Boolean,
        sourceDir: File,
    ): CoverageAnalyzer =
        if (showCoverage) {
            val coverageFile = File(".").canonicalFile.resolve(COVERAGE_FILE)
            LineCoverageAnalyzer(sourceDir.canonicalFile, SonarQubeReporter(coverageFile))
        } else {
            NoCoverageAnalyzer
        }

    private fun handleResults(
        testResults: List<TestHarness.TestHarnessResults>,
        output: ColorWriter,
    ) {
        val paddingResult = maxOf(testResults.maxOfOrNull { it.description.length } ?: 0, MIN_PADDING)
        testResults.forEach {
            val success = !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut()
            if (success) {
                handleSuccessResult(it, paddingResult, output)
            } else {
                handleFailedResult(it, paddingResult, output)
            }
        }
    }

    private fun handleFailedResult(
        it: TestHarness.TestHarnessResults,
        paddingResult: Int,
        output: ColorWriter,
    ) {
        val failed = it.tapResult.testResults.filter { result -> result.status == StatusValues.NOT_OK }
        val summary =
            formatDetailedSummary(
                description = it.description,
                success = false,
                numTests = it.tapResult.numberOfTestResults,
                executionTime = it.duration,
                failed = failed.size,
                explanation =
                    it.tapResult.bailOuts
                        .firstOrNull()
                        ?.reason
                        ?.let { reason -> normalizeWindowsPath(reason) },
                padding = paddingResult,
            )
        output.error(summary)

        failed.map { tr ->
            val failedTest = formatSummary(tr.description, false, padding = paddingResult)
            output.error(failedTest)
        }
    }

    private fun handleSuccessResult(
        it: TestHarness.TestHarnessResults,
        paddingResult: Int,
        output: ColorWriter,
    ) {
        val summary =
            formatDetailedSummary(
                description = it.description,
                success = true,
                numTests = it.tapResult.numberOfTestResults,
                executionTime = it.duration,
                padding = paddingResult,
            )

        output.success(summary)
    }

    private fun formatDetailedSummary(
        description: String,
        success: Boolean,
        numTests: Int,
        executionTime: Duration,
        failed: Int = 0,
        explanation: String? = null,
        padding: Int,
    ) = "${formatSummary(description, success, failed, padding)} ${
        numTests.toString().padEnd(
            4,
            ' ',
        )
    } tests in ${executionTime.toMillis()} ms${if (explanation != null) " ($explanation)" else ""}"

    private fun formatSummary(
        description: String,
        success: Boolean,
        failed: Int = 0,
        padding: Int,
    ) = "${formatDescription(description, success, padding)} ${formatSuccess(success)}${formatFailed(failed)}"

    private fun formatDescription(
        description: String,
        success: Boolean,
        padding: Int,
    ) = "'${normalizeWindowsPath(description)}' ".padEnd(
        padding + (if (success) 5 else 4),
        '.',
    )

    private fun formatFailed(failed: Int) = (if (failed > 0) "($failed)" else "").padEnd(10, ' ')

    private fun formatSuccess(success: Boolean) = if (success) "PASS" else "FAIL"

    private fun normalizeWindowsPath(message: String): String {
        if (File.separatorChar != '\\') {
            return message // Only normalize on Windows
        }

        // Convert /D:/path to D:\path format
        return message
            .replace(Regex("/([A-Za-z]):/")) { match ->
                "${match.groupValues[1]}:\\"
            }.replace('/', '\\')
    }
}
