package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CloudLoginCommandIT :
    FunSpec({
        class TestContext

        fun withTestContext(test: TestContext.() -> Unit) {
            val context =
                TestContext()
            context.test()
        }

        context("success") {
            test("cloud login") {
                withTestContext {
                    runCommand(
                        commands = listOf("cloud", "login"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                                Completed compilation for 1 file in XXX ms

                                No NPL protocols found in the target directory.
                                """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }
    })
