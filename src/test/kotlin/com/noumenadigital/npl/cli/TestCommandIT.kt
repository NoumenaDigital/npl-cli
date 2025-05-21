package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.commands.registry.TestCommand.Companion.COVERAGE_FILE
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class TestCommandIT :
    FunSpec(
        {
            val coverageFile = File(".").resolve(COVERAGE_FILE)

            beforeEach {
                if (coverageFile.exists()) {
                    coverageFile.delete()
                }
            }

            afterEach {
                if (coverageFile.exists()) {
                    coverageFile.delete()
                }
            }

            context("success") {
                test("both main and test sources") {
                    coverageFile.exists() shouldBe false

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms

                        NPL test completed successfully in XXX ms.
                        """.normalize()

                        coverageFile.exists() shouldBe false
                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }

                test("both main and test sources - with coverage") {
                    coverageFile.exists() shouldBe false

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath, "--coverage"),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms
                        Line coverage summary
                        --------------------------------------
                        src/main/npl/objects/iou/iou.npl   XXX%
                        src/test/npl/objects/test_iou.npl XXX%
                        --------------------------------------
                        Overall                            XXX%


                        NPL test completed successfully in XXX ms.
                        """.normalize()

                        coverageFile.exists() shouldBe true
                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }

                test("test compilation failure") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "test_failure")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        'compilation' ............... FAIL           0    tests in XXX ms ($testDirPath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod')

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
            }

            context("failure") {
                test("one test failed") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "test_assertion_failed")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou_failed.npl' . FAIL(1)        2    tests in XXX ms
                        'Amount owed should reflect payment ==> Expect '112300', got '50'.' .......................................................................... FAIL

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("all test failed") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "test_failed")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou_failed.npl' ... FAIL(2)        2    tests in XXX ms
                        'Amount owed should equal initial value ==> Expect '999', got '100'.' ................................................................ FAIL
                        'Amount owed should reflect payment ==> Expect '777', got '50'.' ..................................................................... FAIL
                        '$testDirPath/src/test/npl/objects/test_iou_failed_2.npl' . FAIL(2)        2    tests in XXX ms
                        'Amount owed should equal initial value ==> Expect '999', got '100'.' ................................................................ FAIL
                        'Amount owed should reflect payment ==> Expect '777', got '50'.' ..................................................................... FAIL

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("no test sources found") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "empty_tests_folder")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        No NPL tests found.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("sources compilation failure") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "single_file")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        'compilation' ............... FAIL           0    tests in XXX ms ($testDirPath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?)

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
            }

            context("warnings") {
                test("source code compilation warning") {
                    val testDirPath =
                        getTestResourcesPath(listOf("warnings", "compilation")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--sourceDir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms

                        NPL test completed successfully in XXX ms.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        },
    )
