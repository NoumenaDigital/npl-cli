package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.registry.TestCommand.Companion.COVERAGE_FILE
import com.noumenadigital.npl.testing.coverage.LineCoverageAnalyzer
import com.noumenadigital.npl.testing.coverage.SonarQubeReporter
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.io.File

class TestHarnessTest :
    FunSpec({

        afterTest {
            File(COVERAGE_FILE).takeIf { it.exists() }?.delete()
        }

        data class TestContext(
            val tempDir: File = tempdir(),
            val withCoverage: Boolean,
        ) {
            val sourcesManager = SourcesManager(srcPath = tempDir.absolutePath, testSrcPath = tempDir.absolutePath)
            val harness =
                if (withCoverage) {
                    val coverageFile = File(".").resolve(COVERAGE_FILE)
                    TestHarness(
                        sourcesManager,
                        LineCoverageAnalyzer(tempDir, SonarQubeReporter(coverageFile)),
                    )
                } else {
                    TestHarness(sourcesManager)
                }

            fun createSourceFile(
                name: String,
                content: String = "package main",
            ) {
                File(tempDir, "$name.npl").writeText(content)
            }

            fun createTestFile(
                name: String,
                content: String =
                    """
                    package test
                    @test
                    function test(test: Test) -> {
                        test.assertEquals(100, 100, "Should be true");
                    };
                    """.trimIndent(),
            ) {
                val dir = File(tempDir, SRC_TEST_DIR).apply { mkdirs() }
                File(dir, "$name.npl").writeText(content)
            }
        }

        fun withTestContext(
            withCoverage: Boolean = false,
            test: TestContext.() -> Unit,
        ) {
            TestContext(withCoverage = withCoverage).apply(test)
        }

        test("should return results for successful compilation and test execution") {
            withTestContext {
                createSourceFile("main")
                createTestFile("test_case")

                val results = harness.runTest()

                results shouldHaveSize 1
                results.first().tapResult.containsNotOk() shouldBe false
                results
                    .first()
                    .tapResult.bailOuts.size shouldBe 0
                File(results.first().description).absolutePath shouldBe
                    File(tempDir, listOf("src", "test", "npl", "test_case.npl").joinToString(separator = File.separator)).absolutePath
            }
        }

        test("should create coverage.xml when coverage is selected") {
            withTestContext(withCoverage = true) {
                createSourceFile("main")
                createTestFile("test_case")

                val results = harness.runTest()

                results shouldHaveSize 1

                val coverageFile = File(COVERAGE_FILE)
                coverageFile.exists().shouldBeTrue()
                with(coverageFile.readLines()) {
                    size shouldBe 5
                    filter { it.contains("file path") } shouldHaveSize 2
                    filter { it.contains("main.npl") } shouldHaveSize 1
                    filter { it.contains("test_case.npl") } shouldHaveSize 1
                }
            }
        }

        test("should return compilation-only result if compilation fails") {
            withTestContext {
                createSourceFile("bad_source", "non_existing_function()")

                val results = harness.runTest()

                results shouldHaveSize 1
                results
                    .first()
                    .tapResult.bailOuts.size shouldBeEqual 1
                results.first().description shouldBe "compilation"
            }
        }
    }) {
    companion object {
        private val SRC_TEST_DIR = listOf("src", "test", "npl").joinToString(separator = File.separator)
    }
}
