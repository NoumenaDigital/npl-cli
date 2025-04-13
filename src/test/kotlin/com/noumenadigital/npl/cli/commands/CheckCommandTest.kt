package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.CommandProcessor
import com.noumenadigital.npl.cli.commands.registry.CheckCommand.ANSI_RED
import com.noumenadigital.npl.cli.commands.registry.CheckCommand.ANSI_RESET
import com.noumenadigital.npl.cli.commands.registry.CheckCommand.ANSI_YELLOW
import com.noumenadigital.npl.cli.service.CommandsParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths

class CheckCommandTest :
    FunSpec({
        data class TestContext(
            val commandsParser: CommandsParser = CommandsParser,
            val writer: Writer = StringWriter(),
            val testResourcesPath: Path = Paths.get("src", "test", "resources", "npl-sources"),
            val executor: CommandProcessor = CommandProcessor(commandsParser = commandsParser, baseDir = testResourcesPath),
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
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
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $ANSI_RED$absolutePath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl/objects/car/car.npl: (8, 1) E0001: Syntax error: missing {<EOF>, ';'} at 'protocol'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl/objects/car/car.npl: (16, 5) E0001: Syntax error: extraneous input 'permission' expecting {'become', 'const', 'for', 'function', 'guard', 'if', 'match', 'notify', 'optional', 'private', 'require', 'return', 'this', 'var', 'vararg', 'with', TEXT_LITERAL, BOOLEAN_LITERAL, PARTY_LITERAL, TIME_LITERAL, NUMBER_LITERAL, IDENTIFIER, '(', '{', '}', '-', '!'}$ANSI_RESET

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple files") {
                withTestContext("failure/multiple_files") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $ANSI_RED$absolutePath/src/main/npl/objects/iou/iou.npl: (9, 64) E0001: Syntax error: mismatched input ';' expecting {'->', '<'}$ANSI_RESET

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("multiple packages") {
                withTestContext("failure/multiple_packages") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $ANSI_RED$absolutePath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'$ANSI_RESET

                            NPL check failed with errors.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("versioned directory with errors") {
                withTestContext("failure/versioned_dir_errors") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl-141.1
                            $ANSI_RED$absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 11) E0001: Syntax error: missing {<EOF>, ';'} at 'this'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 16) E0001: Syntax error: missing {<EOF>, ';'} at 'will'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 21) E0001: Syntax error: missing {<EOF>, ';'} at 'cause'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 27) E0001: Syntax error: missing {<EOF>, ';'} at 'syntax'$ANSI_RESET
                            $ANSI_RED$absolutePath/src/main/npl-141.1/objects/car/car.npl: (3, 34) E0001: Syntax error: missing {<EOF>, ';'} at 'error'$ANSI_RESET

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
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Looking for NPL files in src/main/npl
                            $ANSI_YELLOW$absolutePath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.$ANSI_RESET
                            $ANSI_YELLOW$absolutePath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused$ANSI_RESET
                            $ANSI_YELLOW$absolutePath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused$ANSI_RESET
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL check completed with warnings.
                            """.trimIndent() + "\n",
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                }
            }

            test("no NPL sources") {
                withTestContext("warnings/no_sources") {
                    executor.process(listOf("check"), writer)
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
    })
