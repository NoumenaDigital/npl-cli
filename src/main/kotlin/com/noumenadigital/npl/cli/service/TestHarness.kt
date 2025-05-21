package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.lang.TestValue
import com.noumenadigital.npl.testing.TapListener
import com.noumenadigital.npl.testing.TestExecutor
import com.noumenadigital.npl.testing.TestPlan
import com.noumenadigital.npl.testing.coverage.CoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.NoCoverageAnalyzer
import org.tap4j.consumer.TapConsumerFactory
import org.tap4j.model.TestSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class TestHarness(
    private val sourcesManager: SourcesManager,
    private val analyzer: CoverageAnalyzer = NoCoverageAnalyzer,
) {
    fun runTest(): List<TestHarnessResults> {
        val sources = sourcesManager.getNplSources()
        val compilationResult =
            runTest("compilation") { listener ->
                TestExecutor
                    .compile(
                        listener,
                        sources,
                        compilerConfiguration(sourcesManager.nplContribLibrary),
                    )?.also {
                        analyzer.registerSources(sources.map { File(it.location.path) })
                        analyzer.registerCodeToCover(it.protoMap)
                    }
            }
        val isCompilationSuccess =
            !compilationResult.tapResult.containsNotOk() && !compilationResult.tapResult.containsBailOut()
        if (isCompilationSuccess) {
            val result =
                Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)).run {
                    compilationResult.result?.let { compilationOutput ->
                        val results = invokeAll(createCallables(compilationOutput, analyzer)).map { c -> c.get() }
                        shutdown()
                        results
                            .filter { it.tapResult.testResults.isNotEmpty() }
                            .sortedBy { it.description }
                            .map { TestHarnessResults(it.tapResult, it.duration, it.description) }
                    }
                }

            analyzer.writeReport()
            return result ?: emptyList()
        } else {
            return listOf(
                TestHarnessResults(
                    compilationResult.tapResult,
                    compilationResult.duration,
                    compilationResult.description,
                ),
            )
        }
    }

    private inline fun <reified R> runTest(
        description: String,
        runnable: (TestValue.Listener) -> R?,
    ): TestResult<R> {
        val start = System.nanoTime()
        val output = ByteArrayOutputStream()
        val listener = TapListener(PrintWriter(OutputStreamWriter(output)))

        listener.testingStarted()
        val result: R? = runnable(listener)
        listener.testingFinished()

        val consumer = TapConsumerFactory.makeTap13Consumer()
        val (tapResult, duration) =
            ByteArrayInputStream(output.toByteArray()).use { input ->
                consumer.load(input.reader()) to Duration.ofNanos(System.nanoTime() - start)
            }
        return TestResult(result, tapResult, duration, description)
    }

    private fun createCallables(
        compilationOutput: TestExecutor.Output,
        analyzer: CoverageAnalyzer,
    ) = sourcesManager
        .getNplSources()
        .map { source ->
            Callable {
                runTest(source.location.path) { listener ->
                    TestExecutor.run(
                        listener,
                        compilationOutput,
                        TestPlan(mapOf(source.location to TestPlan.FilePlan())),
                        analyzer,
                    )
                }
            }
        }

    data class TestResult<R>(
        val result: R?,
        val tapResult: TestSet,
        val duration: Duration,
        val description: String,
    )

    data class TestHarnessResults(
        val tapResult: TestSet,
        val duration: Duration,
        val description: String,
    )
}
