package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CommandExecutionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File

class SourcesManagerTest :
    FunSpec({

        data class TestContext(
            val projectDir: File,
        ) {
            val manager = SourcesManager(projectDir.absolutePath)

            fun createTestFile(
                relativePath: String,
                name: String,
                content: String = "// NPL code",
            ) {
                val dir = File(projectDir, relativePath).apply { mkdirs() }
                File(dir, "$name.npl").writeText(content)
            }
        }

        fun withTestContext(
            projectDir: File = tempdir(),
            test: TestContext.() -> Unit,
        ) {
            TestContext(projectDir).apply(test)
        }

        test("should return list of NPL sources") {
            withTestContext {
                createTestFile("src/test/npl", "iou_test")
                createTestFile("src/main/npl", "car")

                val sources = manager.getNplSources()
                sources shouldHaveSize 2
            }
        }

        test("should throw if no NPL test sources exist") {
            withTestContext {
                File(projectDir, "src/test/npl").mkdirs()
                shouldThrow<CommandExecutionException> {
                    manager.getNplTestSources()
                }.message shouldBe "No NPL test files found"
            }
        }

        test("should return list of NPL sources from root") {
            withTestContext {
                createTestFile("", "iou")
                createTestFile("", "car")

                val sources = manager.getNplSources()
                sources shouldHaveSize 2
            }
        }

        test("should throw if no NPL sources found in project root") {
            withTestContext {
                val exception =
                    shouldThrow<CommandExecutionException> {
                        manager.getNplSources()
                    }

                exception.message shouldBe "No NPL source files found"
            }
        }

        test("should throw if given directory does not exist") {
            withTestContext(File("nonexistingDir")) {
                val invalidDir = File(projectDir, "/src/test/npl").absolutePath

                val exception =
                    shouldThrow<CommandExecutionException> {
                        manager.getNplTestSources()
                    }
                exception.message shouldBe "Directory $invalidDir does not exist"
            }
        }
    })
