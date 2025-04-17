package com.noumenadigital.npl.cli

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.io.File

class ITBinaryCommands :
    FunSpec({
        data class TestContext(
            val commands: String,
            val process: Process =
                ProcessBuilder("./target/npl", commands)
                    .directory(File("."))
                    .redirectErrorStream(true)
                    .start(),
            val output: String = process.inputStream.bufferedReader().readText(),
        )

        fun runWithCommand(
            commands: List<String>,
            test: TestContext.() -> Unit,
        ) {
            TestContext(commands.joinToString(" ")).apply(test)
        }

        test("version command should return correct value") {
            runWithCommand(listOf("version")) {
                process.waitFor()

                val expectedOutput =
                    """
                    I'm v1.0
                    """.trimIndent()

                output shouldBeEqual expectedOutput
                process.exitValue() shouldBe ExitCode.SUCCESS.code
            }
        }

        test("help command should return list of commands") {
            runWithCommand(listOf("help")) {

                process.waitFor()

                val expectedOutput =
                    """
                    version    Display the current version of the NPL CLI
                    help       Display the description for npl-cli commands
                    check      Validate the correctness of the NPL sources (excluding test sources) in the current directory

                    """.trimIndent()

                output shouldBe expectedOutput

                process.exitValue() shouldBe ExitCode.SUCCESS.code
            }
        }

        test("unknown command error") {
            runWithCommand(listOf("notexisitingcommand")) {
                process.waitFor()

                val expectedOutput =
                    """
                    Command not supported: 'notexisitingcommand'.
                    """.trimIndent()

                output shouldBeEqual expectedOutput
                process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
            }
        }

        test("correct command should be suggested") {
            runWithCommand(listOf("versTion")) {
                process.waitFor()

                val expectedOutput =
                    """
                    Command not supported: 'verstion'. Did you mean 'version'?
                    """.trimIndent()

                output shouldBeEqual expectedOutput
                process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
            }
        }
    })
