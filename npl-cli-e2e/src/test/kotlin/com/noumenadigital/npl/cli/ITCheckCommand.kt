package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class ITCheckCommand :
    FunSpec({
        context("success") {
            test("single file") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "single_file")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 1 file in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("multiple files") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 4 files in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("multiple packages") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
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

            test("both main and test sources") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
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

            test("versioned npl directory fallback") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "versioned_dirs")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 1 file in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
        }

        context("failures") {
            test("single file") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("failure", "single_file")).toAbsolutePath().toString()
                runCommand(listOf("check", testDirPath)) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?
                    $testDirPath/src/main/npl/objects/car/car.npl: (8, 1) E0001: Syntax error: missing {<EOF>, ';'} at 'protocol'
                    $testDirPath/src/main/npl/objects/car/car.npl: (16, 5) E0001: Syntax error: extraneous input 'permission' expecting {'become', 'const', 'for', 'function', 'guard', 'if', 'match', 'notify', 'optional', 'private', 'require', 'return', 'this', 'var', 'vararg', 'with', TEXT_LITERAL, BOOLEAN_LITERAL, PARTY_LITERAL, TIME_LITERAL, NUMBER_LITERAL, IDENTIFIER, '(', '{', '}', '-', '!'}

                    NPL check failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }

            test("multiple files") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("failure", "multiple_files")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl/objects/iou/iou.npl: (9, 64) E0001: Syntax error: mismatched input ';' expecting {'->', '<'}

                    NPL check failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }

            test("multiple packages") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("failure", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                    $testDirPath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                    $testDirPath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                    NPL check failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }

            test("failure in test sources") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("success", "test_failure")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod'

                    NPL check failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }

            test("versioned directory with errors") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("failure", "versioned_dir_errors")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl-141.1/objects/car/car.npl: (3, 11) E0001: Syntax error: missing {<EOF>, ';'} at 'this'
                    $testDirPath/src/main/npl-141.1/objects/car/car.npl: (3, 16) E0001: Syntax error: missing {<EOF>, ';'} at 'will'
                    $testDirPath/src/main/npl-141.1/objects/car/car.npl: (3, 21) E0001: Syntax error: missing {<EOF>, ';'} at 'cause'
                    $testDirPath/src/main/npl-141.1/objects/car/car.npl: (3, 27) E0001: Syntax error: missing {<EOF>, ';'} at 'syntax'
                    $testDirPath/src/main/npl-141.1/objects/car/car.npl: (3, 34) E0001: Syntax error: missing {<EOF>, ';'} at 'error'

                    NPL check failed with errors.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                }
            }

            test("directory does not exist") {
                val nonExistentPath = "${TestUtils.getTestResourcesPath()}/non_existent_dir"
                runCommand(
                    commands = listOf("check", nonExistentPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Target directory does not exist: $nonExistentPath
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("path is not a directory") {
                // Create a temporary file
                val tempFile = File.createTempFile("test", ".tmp")
                tempFile.deleteOnExit()

                runCommand(
                    commands = listOf("check", tempFile.absolutePath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Target path is not a directory: ${tempFile.absolutePath}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        context("warnings") {
            test("warnings during compilation") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("warnings", "compilation")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                    $testDirPath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                    $testDirPath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                    Completed compilation for 4 files with 3 warnings in XXX ms

                    NPL check completed with warnings.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("no NPL sources") {
                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("warnings", "no_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    No NPL source files found

                    NPL check completed with warnings.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }
    })
