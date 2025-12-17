package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.SourcesManager
import com.noumenadigital.npl.cli.service.TestHarness
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import com.noumenadigital.npl.testing.coverage.CoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.LineCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.NoCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.SonarQubeReporter
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
        val parsedSrcDir = parsedArguments["source-dir"] as? String
        val contribLibraries =
            parsedArguments["contrib-libraries"]
                ?.toString()
                ?.split(',')
                ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                ?.takeIf { it.isNotEmpty() }
        return TestCommand(
            testSrcDir = parsedTestSrcDir,
            outputDir = parsedOutputDir,
            srcDir = parsedSrcDir,
            coverage = parsedCoverage,
            contribLibraries = contribLibraries,
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
                name = "source-dir",
                description = "Directory containing NPL source files",
                defaultValue = ".",
                isRequired = false,
                valuePlaceholder = "<directory>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Structure.sourceDir,
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
            NamedParameter(
                name = "contrib-libraries",
                description = "Paths to zip archives containing NPL-Contrib libraries, comma separated wo space (optional)",
                isRequired = false,
                valuePlaceholder = "<contrib-libraries>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Structure.contribLibraries,
            ),
        )
}

data class TestCommand(
    private val testSrcDir: String = ".",
    private val srcDir: String? = testSrcDir,
    private val outputDir: String = ".",
    private val coverage: Boolean = false,
    private val contribLibraries: List<String>?,
) : CommandExecutor {
    companion object {
        const val COVERAGE_FILE = "coverage.xml"
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val testSourceDir = File(testSrcDir)
            var sourceDir: File? = null
            if (srcDir != null) {
                sourceDir = File(srcDir)
            }

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
            val testHarness =
                TestHarness(
                    SourcesManager(
                        testSrcPath = testSourceDir.absolutePath,
                        srcPath = sourceDir?.absolutePath ?: testSourceDir.absolutePath,
                        contribLibraries = contribLibraries,
                    ),
                    coverageAnalyzer,
                )

            val start = System.nanoTime()
            val testResults = testHarness.runTest()
            val executionTime = Duration.ofNanos(System.nanoTime() - start).toMillis()
            val report = TestExecutionReport.from(testResults, executionTime)

            report.allResultLines().forEach { (result, line) ->
                when (result) {
                    is TestExecutionReport.Success -> output.success(line)
                    is TestExecutionReport.Failure -> output.error(line)
                }
            }

            if (!report.isSuccess) {
                report.getErrorSummaryLines().forEach { output.error(it) }
                return ExitCode.DATA_ERROR
            } else if (report.totalTestsCount == 0) {
                report.getNoTestsSummaryLines().forEach { output.info(it) }
                return ExitCode.DATA_ERROR
            } else {
                coverageAnalyzer.writeSummary(output::info)
                report.getSuccessSummaryLines().forEach { output.success(it) }
            }
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
}
