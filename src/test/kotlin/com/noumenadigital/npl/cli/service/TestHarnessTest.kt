package com.noumenadigital.npl.cli.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.io.File

class TestHarnessTest :
    FunSpec({

        data class TestContext(
            val tempDir: File = tempdir(),
        ) {
            val sourcesManager = SourcesManager(tempDir.absolutePath)
            val harness = TestHarness(sourcesManager)

            fun createSourceFile(
                name: String,
                content: String = "package main",
            ) {
                File(tempDir, "$name.npl").writeText(content)
            }

            fun createTestFile(
                name: String,
                content: String = "package test",
            ) {
                val dir = File(tempDir, "src/test/npl").apply { mkdirs() }
                File(dir, "$name.npl").writeText(content)
            }
        }

        fun withTestContext(test: TestContext.() -> Unit) {
            TestContext().apply(test)
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
                results.first().description shouldBe File(tempDir, "src/test/npl/test_case.npl").absolutePath
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
    })
