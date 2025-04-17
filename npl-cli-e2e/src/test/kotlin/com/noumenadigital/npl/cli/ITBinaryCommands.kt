package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class ITBinaryCommands :
    FunSpec({
        test("version command should return correct value") {
            runCommand(listOf("version")) {
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
            runCommand(listOf("help")) {
                process.waitFor()

                val expectedOutput =
                    """
                    version    Display the current version of the NPL CLI
                    help       Display the description for npl-cli commands
                    check      Validate the correctness of NPL sources
                                 <directory>  Target directory containing NPL source files to check (defaults to current directory)
                    """.trimIndent()

                output shouldBe expectedOutput
                process.exitValue() shouldBe ExitCode.SUCCESS.code
            }
        }

        test("unknown command error") {
            runCommand(listOf("notexisitingcommand")) {
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
            runCommand(listOf("versTion")) {
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
