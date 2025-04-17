package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.CheckCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths

class CheckCommandTest :
    FunSpec({
        fun getTestResourcesPath(subPath: String = ""): Path {
            val rootDir = File("..").canonicalFile
            return Paths.get(rootDir.toString(), "test-resources", "npl-sources", subPath)
        }

        data class TestContext(
            val writer: Writer = StringWriter(),
            val testResourcesPath: Path = getTestResourcesPath(),
            val checkCommand: CheckCommand =
                CheckCommand(
                    useColor = false,
                    targetDir = testResourcesPath.toAbsolutePath().toString(),
                ),
        ) {
            val absolutePath: String get() = testResourcesPath.toAbsolutePath().toString()
        }

        fun withTestContext(
            testDir: String,
            test: TestContext.() -> Unit,
        ) {
            val context =
                TestContext(
                    testResourcesPath = getTestResourcesPath(testDir),
                    checkCommand =
                        CheckCommand(
                            useColor = false,
                            targetDir = getTestResourcesPath(testDir).toAbsolutePath().toString(),
                        ),
                )
            context.apply(test)
        }

        fun normalizeOutput(output: String): String =
            output
                // Normalize line endings
                .replace("\r\n", "\n")
                // Normalize durations
                .replace(Regex("in \\d+ ms"), "in XXX ms")

        context("success") {
            test("single file") {
                withTestContext("success/single_file") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }

            test("multiple files") {
                withTestContext("success/multiple_files") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            Completed compilation for 4 files in XXX ms

                            NPL check completed successfully.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }

            test("multiple packages") {
                withTestContext("success/multiple_packages") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            Completed compilation for 2 files in XXX ms

                            NPL check completed successfully.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }

            test("both main and test sources") {
                withTestContext("success/both_sources") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            Completed compilation for 2 files in XXX ms

                            NPL check completed successfully.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }

            test("versioned npl directory fallback") {
                withTestContext("success/versioned_dirs") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }
        }

        context("failure") {
            test("single file") {
                withTestContext("failure/single_file") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?
                            $absolutePath/src/main/npl/objects/car/car.npl: (8, 1) E0001: Syntax error: missing {<EOF>, ';'} at 'protocol'
                            $absolutePath/src/main/npl/objects/car/car.npl: (16, 5) E0001: Syntax error: extraneous input 'permission' expecting {'become', 'const', 'for', 'function', 'guard', 'if', 'match', 'notify', 'optional', 'private', 'require', 'return', 'this', 'var', 'vararg', 'with', TEXT_LITERAL, BOOLEAN_LITERAL, PARTY_LITERAL, TIME_LITERAL, NUMBER_LITERAL, IDENTIFIER, '(', '{', '}', '-', '!'}

                            NPL check failed with errors.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }

            test("multiple files") {
                withTestContext("failure/multiple_files") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (9, 64) E0001: Syntax error: mismatched input ';' expecting {'->', '<'}

                            NPL check failed with errors.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }

            test("multiple packages") {
                withTestContext("failure/multiple_packages") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                            NPL check failed with errors.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }

            test("failure in test sources but not in main sources") {
                withTestContext("success/test_failure") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod'

                            NPL check failed with errors.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }

            test("versioned directory with errors") {
                withTestContext("failure/versioned_dir_errors") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 11) E0001: Syntax error: missing {<EOF>, ';'} at 'this'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 16) E0001: Syntax error: missing {<EOF>, ';'} at 'will'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 21) E0001: Syntax error: missing {<EOF>, ';'} at 'cause'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 27) E0001: Syntax error: missing {<EOF>, ';'} at 'syntax'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 34) E0001: Syntax error: missing {<EOF>, ';'} at 'error'

                            NPL check failed with errors.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }
        }

        context("warnings") {
            test("warnings during compilation") {
                withTestContext("warnings/compilation") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            $absolutePath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                            $absolutePath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                            $absolutePath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL check completed with warnings.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.GENERAL_ERROR
                }
            }

            test("no NPL sources") {
                withTestContext("warnings/no_sources") {
                    val exitCode = checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in $absolutePath
                            No NPL source files found

                            NPL check completed with warnings.

                            """.trimIndent(),
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.GENERAL_ERROR
                }
            }
        }

        context("terminal capabilities") {
            test("can enable colors if needed") {
                val coloredCheckCommand =
                    CheckCommand(
                        useColor = true,
                        targetDir = getTestResourcesPath("failure/single_file").toAbsolutePath().toString(),
                    )
                val writer = StringWriter()

                val exitCode = coloredCheckCommand.execute(writer)

                // In a real TTY, this would include color codes, but in tests
                // the StringWriter is not a TTY, so colors might be auto-disabled
                exitCode shouldBe ExitCode.COMPILATION_ERROR
            }
        }

        context("directory failures") {
            test("directory does not exist") {
                val nonExistentPath = getTestResourcesPath("non_existent_directory").toAbsolutePath()
                val writer = StringWriter()
                val checkCommand =
                    CheckCommand(
                        useColor = false,
                        targetDir = nonExistentPath.toString(),
                    )

                val exitCode = checkCommand.execute(writer)

                normalizeOutput(writer.toString()) shouldBe "Target directory does not exist: $nonExistentPath\n"
                exitCode shouldBe ExitCode.GENERAL_ERROR
            }

            test("target is not a directory") {
                // Create a temporary file to use as target
                val tempFile = File.createTempFile("temp", ".txt")
                tempFile.deleteOnExit()

                val writer = StringWriter()
                val checkCommand =
                    CheckCommand(
                        useColor = false,
                        targetDir = tempFile.absolutePath,
                    )

                val exitCode = checkCommand.execute(writer)

                normalizeOutput(writer.toString()) shouldBe "Target path is not a directory: ${tempFile.absolutePath}\n"
                exitCode shouldBe ExitCode.GENERAL_ERROR
            }
        }
    })
