package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TestCommandIT :
    FunSpec({
        context("success") {
            test("both main and test sources") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("test", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 2 files in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
        }
    })
