package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.CommandProcessor
import com.noumenadigital.npl.cli.ExitCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class CommandProcessorServiceTest :
    FunSpec({
        data class TestContext(
            val commandsParser: CommandsParser = CommandsParser,
            val writer: ColorWriter = ColorWriter(StringWriter(), true),
            val executor: CommandProcessor = CommandProcessor(commandsParser),
        )

        fun withTestContext(test: TestContext.() -> Unit) {
            TestContext().apply(test)
        }
        test("should execute parsed command and write output") {
            withTestContext {
                val exitCode = executor.process(listOf("version"), writer)
                val expectedOutput =
                    """
                    I'm v1.0
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.SUCCESS
            }
        }
        test("should execute 'help' command if no input provided") {
            withTestContext {
                val exitCode = executor.process(emptyList(), writer)
                val expectedOutput =
                    """
                    version    Display the current version of the NPL CLI
                    help       Display the description for npl-cli commands
                    check      Validate the correctness of NPL sources
                                 <directory>  Target directory containing NPL source files to check (defaults to current directory)
                    openapi    Generate the openapi specifications of NPL api
                                 <directory>  Target directory containing NPL protocols (defaults to current directory)
                    """.trimIndent()

                writer.toString().trimIndent() shouldBe expectedOutput
                exitCode shouldBe ExitCode.SUCCESS
            }
        }

        test("should print error message if command cannot be found") {
            withTestContext {
                val exitCode = executor.process(listOf("nonExistingCommand"), writer)
                val expectedOutput =
                    """
                    Command not supported: 'nonexistingcommand'.
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.CONFIG_ERROR
            }
        }

        test("should suggest another command if there is match with existing command") {
            withTestContext {
                val exitCode = executor.process(listOf("vers"), writer)
                val expectedOutput =
                    """
                    Command not supported: 'vers'. Did you mean 'version'?
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.CONFIG_ERROR
            }
        }
    })
