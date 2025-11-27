package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.service.TestHarness
import com.noumenadigital.npl.cli.util.normalizeWindowsPath
import org.tap4j.util.StatusValues
import java.time.Duration

data class TestExecutionReport(
    val results: List<TestResultItem>,
    val executionTimeMs: Long,
    val isSuccess: Boolean,
    val failedTestsCount: Int,
    val totalTestsCount: Int,
) {
    sealed class TestResultItem {
        abstract val lines: List<String>
    }

    data class Success(
        val summary: String,
    ) : TestResultItem() {
        override val lines: List<String> = listOf(summary)
    }

    data class Failure(
        val summary: String,
        val stackTraces: List<String>,
        val fileLinks: List<String>,
    ) : TestResultItem() {
        override val lines: List<String> = listOf(summary) + stackTraces
    }

    fun allFileLinks(): List<String> = results.filterIsInstance<Failure>().flatMap { it.fileLinks }

    fun allResultLines(): List<Pair<TestResultItem, String>> =
        results.flatMap { result ->
            result.lines.map { line -> result to line }
        }

    fun getSuccessSummaryLines(): List<String> =
        listOf(
            "\nTests run: $totalTestsCount, Failures: $failedTestsCount",
            "\n------------------------------------------------",
            "NPL test completed successfully in $executionTimeMs ms.",
            "------------------------------------------------",
        )

    fun getErrorSummaryLines(): List<String> =
        listOf(
            "\nTests run: $totalTestsCount, Failures: $failedTestsCount",
            allFileLinks().joinToString("\n"),
            "\n------------------------------------------------",
            "NPL test failed with errors.",
            "------------------------------------------------",
        ).filter { it.isNotEmpty() }

    fun getNoTestsSummaryLines(): List<String> =
        listOf(
            "\nTests run: 0, Failures: 0",
            "\n------------------------------------------------",
            "No NPL tests found.",
            "------------------------------------------------",
        )

    companion object {
        private const val MIN_PADDING = 25

        fun from(
            testResults: List<TestHarness.TestHarnessResults>,
            executionTimeMs: Long,
        ): TestExecutionReport {
            val paddingResult =
                maxOf(testResults.maxOfOrNull { it.description.normalizeWindowsPath().length } ?: 0, MIN_PADDING)

            val resultItems =
                testResults.map { result ->
                    val success = !result.tapResult.containsNotOk() && !result.tapResult.containsBailOut()
                    if (success) {
                        createSuccessItem(result, paddingResult)
                    } else {
                        createFailureItem(result, paddingResult)
                    }
                }

            val totalTests = testResults.sumOf { it.tapResult.testResults.size }

            val isResultSuccess = testResults.all { !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut() }

            return TestExecutionReport(
                results = resultItems,
                executionTimeMs = executionTimeMs,
                isSuccess = isResultSuccess,
                failedTestsCount =
                    testResults.sumOf {
                        it.tapResult.testResults.count { testResult ->
                            testResult.status ==
                                StatusValues.NOT_OK
                        }
                    },
                totalTestsCount = totalTests,
            )
        }

        private fun createSuccessItem(
            result: TestHarness.TestHarnessResults,
            paddingResult: Int,
        ): Success {
            val summary =
                formatDetailedSummary(
                    description = result.description,
                    success = true,
                    numTests = result.tapResult.numberOfTestResults,
                    executionTime = result.duration,
                    padding = paddingResult,
                )
            return Success(summary)
        }

        private fun createFailureItem(
            result: TestHarness.TestHarnessResults,
            paddingResult: Int,
        ): Failure {
            val comments = extractComments(result)
            val stackTraces = extractStackTraces(comments)

            val fileLinks = mutableListOf<String>()
            val formattedStackTraces = mutableListOf<String>()

            stackTraces
                .filter { it.isNotEmpty() }
                .forEach { tr ->
                    val (testName, errorLine) = extractTestInfo(result.description, tr)
                    fileLinks += buildFileLink(result.description, errorLine)
                    formattedStackTraces += formatFileSummary(testName, false, padding = paddingResult)
                    formattedStackTraces += formatErrorStack(tr)
                }

            val summary = buildSummary(result, paddingResult)
            return Failure(summary, formattedStackTraces, fileLinks)
        }

        private fun extractComments(result: TestHarness.TestHarnessResults): List<String> =
            result.tapResult.comments
                ?.mapNotNull { it?.text }
                ?.map { it.replace("ERROR: ", "") }
                .orEmpty()

        private fun extractStackTraces(comments: List<String>): List<List<String>> =
            runCatching {
                splitBy(comments, "at <root>(<root>:0)")
            }.getOrElse { emptyList() }

        private fun extractTestInfo(
            description: String,
            trace: List<String>,
        ): Pair<String, String?> {
            val fileName = description.split("/").takeLast(2).joinToString("/")

            val testNameWithReference =
                trace
                    .asReversed()
                    .find { line ->
                        val tail =
                            line
                                .split("/")
                                .takeLast(2)
                                .joinToString("/")
                                .split(":")
                                .firstOrNull()
                        tail == fileName
                    }?.split("at ")
                    ?.lastOrNull()

            val testName =
                testNameWithReference
                    ?.substringBefore("(")
                    ?.takeIf { it.isNotBlank() }
                    ?: description

            val lastPart =
                testNameWithReference
                    ?.substringAfter("(")
                    ?.dropLast(1)
                    ?.takeIf { it.contains(":") }

            val errorLine =
                lastPart
                    ?.substringAfterLast(":")
                    ?.takeIf { it.all(Char::isDigit) }

            return testName to errorLine
        }

        private fun buildFileLink(
            description: String,
            line: String?,
        ): String =
            if (!line.isNullOrBlank() &&
                description.isNotBlank() &&
                (description.startsWith("/") || description.startsWith("\\"))
            ) {
                "file://".normalizeWindowsPath() + description + ":" + line
            } else {
                description
            }

        private fun formatErrorStack(trace: List<String>): String {
            val body = trace.joinToString("\n")
            return "ERROR: " + body.ifBlank { "ERROR: Unknown" }
        }

        private fun buildSummary(
            result: TestHarness.TestHarnessResults,
            padding: Int,
        ): String =
            formatDetailedSummary(
                description = result.description,
                success = false,
                numTests = result.tapResult.numberOfTestResults,
                executionTime = result.duration,
                failed = result.tapResult.testResults.count { it.status == StatusValues.NOT_OK },
                explanation =
                    result.tapResult.bailOuts
                        .firstOrNull()
                        ?.reason
                        ?.normalizeWindowsPath(),
                padding = padding,
            )

        private fun splitBy(
            list: List<String>,
            delimiter: String,
        ): List<List<String>> {
            val result = mutableListOf<MutableList<String>>()
            var current = mutableListOf<String>()

            for (item in list) {
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

        private fun formatDetailedSummary(
            description: String,
            success: Boolean,
            numTests: Int,
            executionTime: Duration,
            failed: Int = 0,
            explanation: String? = null,
            padding: Int,
        ) = "${formatFileSummary(description, success, failed, padding)} ${
            numTests.toString().padEnd(4, ' ')
        } tests in ${executionTime.toMillis()} ms${if (explanation != null) " ($explanation)" else ""}"

        private fun formatFileSummary(
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
}
