package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ITOpenapiCommand :
    FunSpec({
        context("success") {
            test("no protocols") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "single_file")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 1 file in XXX ms

                    No NPL protocols found in the target directory.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.NO_INPUT.code
                }
            }

            test("multiple files") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                        Completed compilation for 4 files in XXX ms

                        Generating openapi for /processes
                        Generating openapi for /objects/iou
                        """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("multiple packages") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                            Completed compilation for 2 files in XXX ms

                            Generating openapi for /objects/iou
                            Generating openapi for /objects/car
                            """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("test failure") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "test_failure")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod'

                    NPL openapi failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }
        }

        context("failures") {
            test("multiple packages") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("failure", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                        $testDirPath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                        $testDirPath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                        $testDirPath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                        NPL openapi failed with errors.
                        """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }
        }

        context("warnings") {
            test("warnings during compilation") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("warnings", "compilation")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                            $testDirPath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                            $testDirPath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                            $testDirPath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL openapi completed with warnings.
                            Generating openapi for /processes
                            Generating openapi for /objects/car
                            Generating openapi for /objects/iou
                            """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("no NPL sources") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("warnings", "no_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("openapi", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    No NPL source files found

                    NPL openapi completed with warnings.
                    No NPL protocols found in the target directory.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.NO_INPUT.code
                }
            }
        }
    })
