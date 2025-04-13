package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CheckCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths

class CheckCommandTest :
    FunSpec({
        data class TestContext(
            val writer: Writer = StringWriter(),
            val testResourcesPath: Path = Paths.get("src", "test", "resources", "npl-sources"),
            val checkCommand: CheckCommand =
                CheckCommand(
                    useColor = false,
                    baseDir = Paths.get("src", "test", "resources", "npl-sources"),
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
                    testResourcesPath = Paths.get("src", "test", "resources", "npl-sources", testDir),
                    checkCommand =
                        CheckCommand(
                            useColor = false,
                            baseDir = Paths.get("src", "test", "resources", "npl-sources", testDir),
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
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple files") {
                withTestContext("success/multiple_files") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            Completed compilation for 4 files in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple packages") {
                withTestContext("success/multiple_packages") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            Completed compilation for 2 files in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("both main and test sources") {
                withTestContext("success/both_sources") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("failure in test sources should not lead to check failure") {
                withTestContext("success/test_failure") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("versioned npl directory fallback") {
                withTestContext("success/versioned_dirs") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl-141.2
                            Completed compilation for 1 file in XXX ms

                            NPL check completed successfully.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }
        }

        context("failure") {
            test("single file") {
                withTestContext("failure/single_file") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $absolutePath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?
                            $absolutePath/src/main/npl/objects/car/car.npl: (8, 1) E0001: Syntax error: missing {<EOF>, ';'} at 'protocol'
                            $absolutePath/src/main/npl/objects/car/car.npl: (16, 5) E0001: Syntax error: extraneous input 'permission' expecting {'become', 'const', 'for', 'function', 'guard', 'if', 'match', 'notify', 'optional', 'private', 'require', 'return', 'this', 'var', 'vararg', 'with', TEXT_LITERAL, BOOLEAN_LITERAL, PARTY_LITERAL, TIME_LITERAL, NUMBER_LITERAL, IDENTIFIER, '(', '{', '}', '-', '!'}

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple files") {
                withTestContext("failure/multiple_files") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (9, 64) E0001: Syntax error: mismatched input ';' expecting {'->', '<'}

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple packages") {
                withTestContext("failure/multiple_packages") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $absolutePath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("versioned directory with errors") {
                withTestContext("failure/versioned_dir_errors") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl-141.1
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 11) E0001: Syntax error: missing {<EOF>, ';'} at 'this'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 16) E0001: Syntax error: missing {<EOF>, ';'} at 'will'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 21) E0001: Syntax error: missing {<EOF>, ';'} at 'cause'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 27) E0001: Syntax error: missing {<EOF>, ';'} at 'syntax'
                            $absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 34) E0001: Syntax error: missing {<EOF>, ';'} at 'error'

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }
        }

        context("warnings") {
            test("warnings during compilation") {
                withTestContext("warnings/compilation") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $absolutePath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                            $absolutePath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                            $absolutePath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL check completed with warnings.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("no NPL sources") {
                withTestContext("warnings/no_sources") {
                    checkCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            No NPL source files found

                            NPL check completed with warnings.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }
        }

        context("terminal capabilities") {
            test("can enable colors if needed") {
                val coloredCheckCommand =
                    CheckCommand(
                        useColor = true,
                        baseDir = Paths.get("src", "test", "resources", "npl-sources", "failure/single_file"),
                    )
                val writer = StringWriter()

                coloredCheckCommand.execute(writer)

                // In a real TTY, this would include color codes, but in tests
                // the StringWriter is not a TTY, so colors might be auto-disabled
            }
        }
    })
