package com.noumenadigital.npl.cli.intergration

import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.service.NplCommandsParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.io.Writer

class NplCliIntegrationTest :
    FunSpec({
        lateinit var commandsParser: NplCommandsParser
        lateinit var writer: Writer
        lateinit var executor: NplCommandExecutor

        beforeTest {
            commandsParser = NplCommandsParser
            executor = NplCommandExecutor(commandsParser)
            writer = StringWriter()
        }

        test("should execute parsed command and write output") {

            executor.process(listOf("version"), writer)

            val expectedOutput =
                """
                Executing command 'version'...
                I'm v1.0

                Command 'version' finished SUCCESSFULLY.

                """.trimIndent()
            writer.toString() shouldBe expectedOutput
        }

        test("only one command can be executed at the time") {

            executor.process(listOf("version", "help"), writer)

            val expectedOutput =
                """
                Invalid command line input. Only 1 command can be processed, but was [version, help]

                """.trimIndent()
            writer.toString() shouldBe expectedOutput
        }

        test("null command cannot be executed") {

            executor.process(emptyList(), writer)

            val expectedOutput =
                """
                Invalid command line input. No command to execute

                """.trimIndent()
            writer.toString() shouldBe expectedOutput
        }
    })
