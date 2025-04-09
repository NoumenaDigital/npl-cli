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
        test("null command cannot be executed") {
            withTestContext {
                executor.process(emptyList(), writer)
                val expectedOutput =
                    """
                    Executing command FAILED. No command to execute
                    """.trimIndent()
                writer.toString() shouldBe expectedOutput
            }
        }
    })
