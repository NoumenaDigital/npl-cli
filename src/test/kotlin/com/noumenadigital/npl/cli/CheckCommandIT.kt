package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class CheckCommandIT :
    FunSpec({
        context("success") {
            test("single file") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "single_file")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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

            test("nplcontrib") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "nplcontrib")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath, "--contrib-libraries", "main/contrib/npl-migration-test.zip"),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 3 files in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("single file - relative path") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "single_file"))
                        .let {
                            File(".").canonicalFile.toPath().relativize(it)
                        }.toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Completed compilation for 6 files in XXX ms

                    NPL check completed successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }

            test("multiple packages") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("success", "versioned_dirs")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("failure", "single_file")).toAbsolutePath().toString()
                runCommand(listOf("check", "--source-dir", testDirPath)) {
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
                    getTestResourcesPath(listOf("failure", "multiple_files")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("failure", "multiple_packages")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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

            test("failure in test sources but not in main sources") {
                val testDirPath =
                    getTestResourcesPath(listOf("success", "test_failure")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
                    getTestResourcesPath(listOf("failure", "versioned_dir_errors")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
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
        }

        context("warnings") {
            test("warnings during compilation") {
                val testDirPath =
                    getTestResourcesPath(listOf("warnings", "compilation")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    $testDirPath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                    $testDirPath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                    $testDirPath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                    Completed compilation for 5 files with 3 warnings in XXX ms

                    NPL check completed with warnings.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("no NPL sources") {
                val testDirPath =
                    getTestResourcesPath(listOf("warnings", "no_sources")).toAbsolutePath().toString()
                runCommand(
                    commands = listOf("check", "--source-dir", testDirPath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    No NPL source files found
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        context("directory failures") {
            test("directory does not exist") {
                val nonExistentPath = getTestResourcesPath(listOf("non_existent_directory")).toAbsolutePath()
                runCommand(
                    commands = listOf("check", "--source-dir", nonExistentPath.toString()),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Target directory does not exist: ${nonExistentPath.toFile().relativeOrAbsolute()}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("path is not a directory") {
                // Create a temporary file to use as target
                val tempFile = File.createTempFile("temp", ".txt")
                tempFile.deleteOnExit()

                runCommand(
                    commands = listOf("check", "--source-dir", tempFile.absolutePath),
                ) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Target path is not a directory: ${tempFile.relativeOrAbsolute()}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        context("Yaml config happy path") {
            val testDirPath =
                getTestResourcesPath(listOf("success", "single_file")).toAbsolutePath().toString()

            TestUtils.withYamlConfig(
                """
                structure:
                  sourceDir: $testDirPath
                """.trimIndent(),
            ) {
                runCommand(
                    commands = listOf("check"),
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
    })
