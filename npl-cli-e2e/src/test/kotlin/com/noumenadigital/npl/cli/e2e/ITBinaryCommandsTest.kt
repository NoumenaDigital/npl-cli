package com.noumenadigital.npl.cli.e2e

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

class ITBinaryCommandsTest :
    FunSpec({
        data class TestContext(
            val commands: String,
            val process: Process =
                ProcessBuilder(getNplExecutablePath(), commands)
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

                output shouldContain "I'm v1.0"
                process.exitValue() shouldBe 0
            }
        }

        test("help command should return list of commands") {
            runWithCommand(listOf("help")) {

                process.waitFor()

                output shouldContain "Display the current version of the NPL CLI"
                output shouldContain "Display the description for npl-cli commands"

                process.exitValue() shouldBe 0
            }
        }
    })

private fun getNplExecutablePath() =
    if (System.getProperty("os.name").contains("win", true)) {
        "./target/npl.exe"
    } else {
        "./target/npl"
    }
