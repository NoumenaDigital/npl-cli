package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.CommandProcessor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.io.Writer

class CommandProcessorServiceTest :
    FunSpec({
        data class TestContext(
            val commandsParser: CommandsParser = CommandsParser,
            val writer: Writer = StringWriter(),
            val executor: CommandProcessor = CommandProcessor(commandsParser),
        )

        fun withTestContext(test: TestContext.() -> Unit) {
            TestContext().apply(test)
        }
        test("should execute parsed command and write output") {
            withTestContext {
                executor.process(listOf("version"), writer)
                val expectedOutput =
                    """
                    Executing command 'version'...
                    I'm v1.0
                    Command 'version' finished SUCCESSFULLY.
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
            }
        }
        test("should execute 'help' command if no input provided") {
            withTestContext {
                executor.process(emptyList(), writer)
                val expectedOutput =
                    """
                    Executing command 'help'...
                    version    Display the current version of the NPL CLI
                    help       Display the description for npl-cli commands
                    check      Validate the correctness of the NPL sources (excluding test sources) in the current directory.

                    Command 'help' finished SUCCESSFULLY.
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
            }
        }

        test("should print error message if command cannot be found") {
            withTestContext {
                executor.process(listOf("nonExistingCommand"), writer)
                val expectedOutput =
                    """
                    Command not supported: 'nonexistingcommand'.
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
            }
        }

        test("should suggest another command if there is match with existing command") {
            withTestContext {
                executor.process(listOf("vers"), writer)
                val expectedOutput =
                    """
                    Command not supported: 'vers'. Did you mean 'version'?
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
            }
        }
    })
