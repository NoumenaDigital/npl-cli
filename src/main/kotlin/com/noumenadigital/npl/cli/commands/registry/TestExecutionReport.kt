package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.service.TestHarness
import com.noumenadigital.npl.cli.util.normalizeWindowsPath
import org.tap4j.model.Comment
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

    fun getAllFileLinks(): List<String> = results.filterIsInstance<Failure>().flatMap { it.fileLinks }

    fun getAllResultLines(): List<Pair<TestResultItem, String>> =
        results.flatMap { result ->
            result.lines.map { line -> result to line }
        }

    fun getSuccessSummaryLines(): List<String> =
        listOf(
            "",
            "Tests run: $totalTestsCount, Failures: $failedTestsCount",
            "",
            "------------------------------------------------",
            "NPL test completed successfully in $executionTimeMs ms.",
            "------------------------------------------------",
        )

    fun getErrorSummaryLines(): List<String> =
        listOf(
            "\nTests run: $totalTestsCount, Failures: $failedTestsCount",
            getAllFileLinks().joinToString("\n"),
            "",
            "------------------------------------------------",
            "NPL test failed with errors.",
            "------------------------------------------------",
        )

    fun getNoTestsSummaryLines(): List<String> =
        listOf(
            "Tests run: 0, Failures: 0",
            "",
            "------------------------------------------------",
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

            val allFileLinks = resultItems.filterIsInstance<Failure>().flatMap { it.fileLinks }.toSet()
            val totalTests = testResults.sumOf { it.tapResult.testResults.size }

            val isResultSuccess = testResults.all { !it.tapResult.containsNotOk() && !it.tapResult.containsBailOut() }

            return TestExecutionReport(
                results = resultItems,
                executionTimeMs = executionTimeMs,
                isSuccess = isResultSuccess,
                failedTestsCount = allFileLinks.size,
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
            val failed = result.tapResult.testResults.filter { it.status == StatusValues.NOT_OK }
            val summary =
                formatDetailedSummary(
                    description = result.description,
                    success = false,
                    numTests = result.tapResult.numberOfTestResults,
                    executionTime = result.duration,
                    failed = failed.size,
                    explanation =
                        result.tapResult.bailOuts
                            .firstOrNull()
                            ?.reason
                            ?.normalizeWindowsPath(),
                    padding = paddingResult,
                )

            val comments =
                result.tapResult.comments
                    .map { (it as Comment).text }
                    .map { it.replace("ERROR: ", "") }
            val stackTraces = splitBy(comments, "at <root>(<root>:0)")

            val fileLinks = mutableListOf<String>()
            val formattedStackTraces = mutableListOf<String>()

            stackTraces.forEach { tr ->
                val fileName =
                    result.description
                        .split("/")
                        .takeLast(2)
                        .joinToString("/")
                val testNameWithReference =
                    tr
                        .asReversed()
                        .find { line ->
                            line
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

                if (errorLineLocation != null) {
                    fileLinks.add("file://" + result.description + ":" + errorLineLocation)
                }

                val failedTestSummary = formatFileSummary(testName ?: result.description, false, padding = paddingResult)
                formattedStackTraces.add(failedTestSummary)
                formattedStackTraces.add("ERROR: " + tr.joinToString("\n"))
            }

            return Failure(summary, formattedStackTraces, fileLinks)
        }

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
