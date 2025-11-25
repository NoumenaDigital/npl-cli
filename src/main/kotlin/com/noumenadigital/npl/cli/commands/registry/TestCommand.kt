package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.service.TestHarness
import com.noumenadigital.npl.cli.util.normalizeWindowsPath
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import com.noumenadigital.npl.testing.coverage.CoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.LineCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.NoCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.SonarQubeReporter
import org.tap4j.model.Comment
import org.tap4j.util.StatusValues
import java.io.File
import java.time.Duration

object TestCommandDescriptor : CommandDescriptor {
    override val commandName: String = "test"
    override val description: String = "Run the NPL tests"
    override val supportsMcp: Boolean = true

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedTestSrcDir = parsedArguments["test-source-dir"] as? String ?: "."
        val parsedCoverage = !(parsedArguments["coverage"] == null || parsedArguments["coverage"] as? Boolean == false)
        val parsedOutputDir = parsedArguments["output-dir"] as? String ?: "."
        return TestCommand(
            testSrcDir = parsedTestSrcDir,
            outputDir = parsedOutputDir,
            coverage = parsedCoverage,
        )
    }

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "test-source-dir",
                description =
                    "Source directory containing NPL tests to run." +
                        " Must be a parent directory of all required sources (both production and test).",
                isRequired = false,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Structure.testSourceDir,
            ),
            NamedParameter(
                name = "coverage",
                description = "Report test coverage details (printed to console as well as coverage.xml)",
                isRequired = false,
                configFilePath = YamlConfig.Structure.coverage,
            ),
            NamedParameter(
                name = "output-dir",
                description = "Directory to place generated output files (optional)",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<output directory>",
                takesPath = true,
                isRequiredForMcp = false,
                configFilePath = YamlConfig.Structure.outputDir,
            ),
        )
}

data class TestCommand(
    private val testSrcDir: String = ".",
    private val outputDir: String = ".",
    private val coverage: Boolean = false,
) : CommandExecutor {
    companion object {
        const val MIN_PADDING = 25
        const val COVERAGE_FILE = "coverage.xml"
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val testSourceDir = File(testSrcDir)
            if (!testSourceDir.isDirectory) {
                output.error("Given NPL source directory is not a directory: ${testSourceDir.relativeOrAbsolute()}")
                return ExitCode.GENERAL_ERROR
            }
            if (!testSourceDir.exists()) {
                output.error("Given NPL source directory does not exist: ${testSourceDir.relativeOrAbsolute()}")
                return ExitCode.GENERAL_ERROR
            }

            val coverageAnalyzer =
                coverageAnalyzer(
                    showCoverage = coverage,
                    sourceDir = testSourceDir,
                    outputDir = outputDir,
                )
            val testHarness = TestHarness(SourcesManager(testSourceDir.absolutePath), coverageAnalyzer)

            val start = System.nanoTime()
            val testResults = testHarness.runTest()
            val failedTests = handleResults(testResults, output)
            val isResultSuccess = testResults.all { !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut() }
            val summary = "\nTests run: ${testResults.sumOf { it.tapResult.testResults.size }}, Failures: ${failedTests?.size}"
            if (!isResultSuccess) {
                output.error(summary)
                output.error(failedTests?.joinToString("\n").toString())
                output.info("\n------------------------------------------------")
                output.error("NPL test failed with errors.")
                output.info("------------------------------------------------")
                return ExitCode.DATA_ERROR
            }
            if (testResults.all { it.tapResult.testResults.isEmpty() }) {
                output.info(summary)
                output.info("\n------------------------------------------------")
                output.error("No NPL tests found.")
                output.info("------------------------------------------------")
                return ExitCode.DATA_ERROR
            }

            coverageAnalyzer.writeSummary(output::info)
            output.success(summary)
            output.info("\n------------------------------------------------")
            output.success(
                "NPL test completed successfully in ${
                    Duration.ofNanos(System.nanoTime() - start).toMillis()
                } ms.",
            )
            output.info("------------------------------------------------")
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
        outputDir: String = ".",
    ): CoverageAnalyzer =
        if (showCoverage) {
            val dir = File(outputDir).canonicalFile
            dir.mkdirs()
            val coverageFile = dir.resolve(COVERAGE_FILE)
            LineCoverageAnalyzer(sourceDir.canonicalFile, SonarQubeReporter(coverageFile))
        } else {
            NoCoverageAnalyzer
        }

    private fun handleResults(
        testResults: List<TestHarness.TestHarnessResults>,
        output: ColorWriter,
    ): Set<String>? {
        val paddingResult =
            maxOf(testResults.maxOfOrNull { it.description.normalizeWindowsPath().length } ?: 0, MIN_PADDING)
        val failedResultsSet: MutableSet<String> = mutableSetOf()
        testResults.forEach {
            val success = !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut()
            if (success) {
                handleSuccessResult(it, paddingResult, output)
            } else {
                failedResultsSet.addAll(handleFailedResult(it, paddingResult, output))
            }
        }
        return failedResultsSet
    }

    private fun handleFailedResult(
        it: TestHarness.TestHarnessResults,
        paddingResult: Int,
        output: ColorWriter,
    ): Set<String> {
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
                        ?.normalizeWindowsPath(),
                padding = paddingResult,
            )
        output.error(summary)

        val failedResults: MutableSet<String> = mutableSetOf()
        val strings =
            it.tapResult.comments
                .map { it -> it as Comment }
                .map { it.text }
                .splitBy("ERROR: at <root>(<root>:0)")
        strings.map { tr ->
            val fileName =
                it.description
                    .split("/")
                    .takeLast(2)
                    .joinToString("/")
            val testNameWithReference =
                tr
                    .asReversed()
                    .find {
                        it
                            .split("/")
                            .takeLast(2)
                            .joinToString("/")
                            .split(":")
                            .first() == fileName
                    }?.split("at ")
                    ?.last()
            val testName = testNameWithReference?.split("(")?.first()
            val last = testNameWithReference?.split("(")?.last()?.dropLast(1)
            val errorLineLocation = last?.split(":")?.last()
            val urlToFileFailureLocation = "file://" + it.description + ":" + errorLineLocation
            failedResults.add(urlToFileFailureLocation)
            val failedTest = formatSummary(testName ?: it.description, false, padding = paddingResult)
            output.error(failedTest)
            output.error("Stacktrace: ${tr.joinToString("\n")}")
        }
        return failedResults
    }

    fun List<String>.splitBy(delimiter: String): List<List<String>> {
        val result = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()

        for (item in this) {
            if (item == delimiter) {
                if (current.isNotEmpty()) {
                    result.add(current)
                    current = mutableListOf()
                }
            } else {
                current.add(item)
            }
        }

        if (current.isNotEmpty()) {
            result.add(current)
        }

        return result
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
    ) = "'${description.normalizeWindowsPath()}' ".padEnd(
        padding + (if (success) 5 else 4),
        '.',
    )

    private fun formatFailed(failed: Int) = (if (failed > 0) "($failed)" else "").padEnd(10, ' ')

    private fun formatSuccess(success: Boolean) = if (success) "PASS" else "FAIL"
}
