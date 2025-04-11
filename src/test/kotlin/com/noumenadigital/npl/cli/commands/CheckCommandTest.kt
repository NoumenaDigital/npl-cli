package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.CommandProcessor
import com.noumenadigital.npl.cli.service.CommandsParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Paths

class CheckCommandTest :
    FunSpec({
        // Test context for setting up the test environment
        data class TestContext(
            val commandsParser: CommandsParser = CommandsParser,
            val writer: Writer = StringWriter(),
            val executor: CommandProcessor = CommandProcessor(commandsParser),
            val originalDir: String = System.getProperty("user.dir"),
        )

        // Helper function to run tests in specific directory
        fun withTestContext(
            testDir: String,
            test: TestContext.() -> Unit,
        ) {
            val context = TestContext()
            try {
                // Change to the test directory
                val testPath = Paths.get("src", "test", "resources", "npl-sources", testDir).toString()
                System.setProperty("user.dir", Paths.get(context.originalDir, testPath).toString())

                // Run the test
                context.apply(test)
            } finally {
                // Restore the original directory
                System.setProperty("user.dir", context.originalDir)
            }
        }

        context("success") {
            test("single file") {
                withTestContext("success/single_file") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        Completed compilation for 1 file in 87 ms

                        NPL check completed successfully.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("multiple files") {
                withTestContext("success/multiple_files") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        Completed compilation for 4 files in 92 ms

                        NPL check completed successfully.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("multiple packages") {
                withTestContext("success/multiple_packages") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        Completed compilation for 2 files in 75 ms

                        NPL check completed successfully.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("both main and test sources") {
                withTestContext("success/both_sources") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        Completed compilation for 1 file in 63 ms
                        Looking for NPL files in src/test/npl
                        Completed compilation for 1 file in 58 ms

                        NPL check completed successfully.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("failure in test sources should not lead to check failure") {
                withTestContext("success/test_failure") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        Completed compilation for 1 file in 71 ms
                        Looking for NPL files in src/test/npl
                        src/test/npl/objects/test_iou_error.npl: (12, 15) W0042: Method 'undefinedMethod' does not exist on type 'Iou'
                        Completed compilation for 1 file with 1 warning in 67 ms

                        NPL check completed with warnings.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }
        }

        context("failure") {
            test("single file") {
                withTestContext("failure/single_file") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        src/main/npl/objects/car/car.npl: (10, 5) E0001: Syntax error: missing closing bracket '}'

                        NPL check failed with errors.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("multiple files") {
                withTestContext("failure/multiple_files") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        src/main/npl/objects/car/car.npl: (5, 8) E0003: Unknown type 'Color'
                        src/main/npl/objects/iou/iou.npl: (9, 1) E0004: Function declaration without implementation

                        NPL check failed with errors.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("multiple packages") {
                withTestContext("failure/multiple_packages") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        src/main/npl/objects/car/car.npl: (9, 67) E0003: Unknown type 'Vehicle'
                        src/main/npl/objects/iou/iou.npl: (7, 12) E0003: Unknown type 'Color'

                        NPL check failed with errors.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }
        }

        context("warnings") {
            test("warnings during compilation") {
                withTestContext("warnings/compilation") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        src/main/npl/objects/car/car.npl: (14, 16) W0012: Missing ownership declaration
                        src/main/npl/processes/demo.npl: (17, 5) W0051: Undefined entity 'Settle'
                        Completed compilation for 2 files with 2 warnings in 82 ms

                        NPL check completed with warnings.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("no NPL sources") {
                withTestContext("warnings/no_sources") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src
                        No NPL source files found

                        NPL check completed with warnings.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }

            test("multiple warnings") {
                withTestContext("warnings/multiple") {
                    executor.process(listOf("check"), writer)
                    val expectedOutput =
                        """
                        Executing command 'check'...
                        Looking for NPL files in src/main/npl
                        src/main/npl/objects/car/car.npl: (14, 16) W0012: Missing ownership declaration
                        src/main/npl/processes/process.npl: (17, 5) W0051: Undefined entity 'Settle'
                        Completed compilation for 2 files with 2 warnings in 85 ms

                        NPL check completed with warnings.
                        Command 'check' finished SUCCESSFULLY.
                        """.trimIndent()
                    writer.toString() shouldBe expectedOutput
                }
            }
        }
    })
