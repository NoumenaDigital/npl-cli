package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.CompilerService
import com.noumenadigital.npl.cli.service.SourcesManager.collectSourcesFromDirectory
import com.noumenadigital.npl.lang.TestValue
import com.noumenadigital.npl.testing.TapListener
import com.noumenadigital.npl.testing.TestExecutor
import com.noumenadigital.npl.testing.TestPlan
import com.noumenadigital.npl.testing.coverage.NoCoverageAnalyzer
import org.tap4j.consumer.TapConsumerFactory
import org.tap4j.util.StatusValues
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

data class TestCommand(
    private val targetDir: String = ".",
    private val testDir: String = "$targetDir/src/test/npl",
    private val compilerService: CompilerService = CompilerService(targetDir, "$targetDir/npl-contrib"),
) : CommandExecutor {
    override val commandName: String = "test"
    override val description: String = "Run the NPL tests"

    override val parameters: List<CommandParameter> =
        listOf(
            CommandParameter(
                name = "directory",
                description = "Source directory containing NPL tests to run",
                defaultValue = ".",
                isRequired = false,
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val targetDir = params.firstOrNull() ?: parameters.find { it.name == "directory" }?.defaultValue ?: "."
        return TestCommand(
            targetDir = targetDir,
            compilerService = CompilerService(targetDir, "$targetDir/npl-contrib"),
        )
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val sources = collectSourcesFromDirectory(targetDir)
            val testResults =
                runTest("compilation", output) { listener ->
                    TestExecutor.compile(
                        listener,
                        sources,
                        compilerService.compilerConfiguration("$targetDir/npl-contrib"),
                    )
                }

            val success =
                if (testResults.success) {
                    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1).run {
                        testResults.result?.let { result ->
                            val results = invokeAll(createCallables(result, output)).map { c -> c.get() }
                            shutdown()
                            results.all { it.success }
                        } ?: false
                    }
                } else {
                    false
                }

            output.success("\nNPL test completed successfully.")
            return ExitCode.SUCCESS
        } catch (e: CommandExecutionException) {
            output.error(e.message)
            return ExitCode.GENERAL_ERROR
        } catch (e: Exception) {
            throw CommandExecutionException("Failed to run NPL check: ${e.message}", e)
        }
    }

    private inline fun <reified R> runTest(
        description: String,
        cliOutput: ColorWriter,
        runnable: (TestValue.Listener) -> R?,
    ): TestResult<R> {
        val start = System.nanoTime()
        val output = ByteArrayOutputStream()
        val listener = TapListener(OutputStreamWriter(output))

        listener.testingStarted()
        val result: R? = runnable(listener)
        listener.testingFinished()

        val consumer = TapConsumerFactory.makeTap13Consumer()
        val (tapResult, duration) =
            ByteArrayInputStream(output.toByteArray()).use { input ->
                consumer.load(input.reader()) to Duration.ofNanos(System.nanoTime() - start)
            }
        if (tapResult.numberOfTestResults == 0 && tapResult.numberOfBailOuts == 0) {
            return TestResult(result, true)
        }

        val success = !tapResult.containsNotOk() && !tapResult.containsBailOut()
        when (success) {
            true -> {
                val summary =
                    formatDetailedSummary(
                        description,
                        true,
                        tapResult.numberOfTestResults,
                        duration,
                    )

                cliOutput.success(summary)
            }

            false -> {
                val failed = tapResult.testResults.filter { it.status == StatusValues.NOT_OK }
                val summary =
                    formatDetailedSummary(
                        description,
                        false,
                        tapResult.numberOfTestResults,
                        duration,
                        failed.size,
                        tapResult.bailOuts.firstOrNull()?.reason,
                    )
                cliOutput.error(summary)

                // Display which tests failed
                failed.map { tr ->
                    val failedTest = formatSummary(tr.description, false)
                    cliOutput.error(failedTest)
                }
            }
        }

        return TestResult(result, success)
    }

    private fun createCallables(
        compilationOutput: TestExecutor.Output,
        output: ColorWriter,
    ) = collectSourcesFromDirectory(testDir)
        .map { source ->
            Callable {
                runTest(source.location.path, output) { listener ->
                    TestExecutor.run(
                        listener,
                        compilationOutput,
                        TestPlan(mapOf(source.location to TestPlan.FilePlan())),
                        NoCoverageAnalyzer,
                    )
                }
            }
        }

    private fun formatFailed(failed: Int) = (if (failed > 0) "($failed)" else "").padEnd(10, ' ')

    private fun formatDescription(
        description: String,
        success: Boolean,
    ) = "'$description' ".padEnd(
        collectSourcesFromDirectory(testDir).fold(25) { a, c ->
            max(
                a,
                c.location.toString().length,
            )
        } + (if (success) 5 else 4),
        '.',
    )

    private fun formatSuccess(success: Boolean) = if (success) "PASS" else "FAIL"

    private fun formatSummary(
        description: String,
        success: Boolean,
        failed: Int = 0,
    ) = "${formatDescription(description, success)} ${formatSuccess(success)}${formatFailed(failed)}"

    private fun formatDetailedSummary(
        description: String,
        success: Boolean,
        numTests: Int,
        executionTime: Duration,
        failed: Int = 0,
        explanation: String? = null,
    ) = "${formatSummary(description, success, failed)} ${
        numTests.toString().padEnd(
            4,
            ' ',
        )
    } tests in ${executionTime.toMillis()} ms${if (explanation != null) " ($explanation)" else ""}"

    private data class TestResult<R>(
        val result: R?,
        val success: Boolean,
    )
}
