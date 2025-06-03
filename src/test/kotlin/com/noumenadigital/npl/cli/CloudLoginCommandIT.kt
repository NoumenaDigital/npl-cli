package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockWebServer

class CloudLoginCommandIT :
    FunSpec({

        class TestContext {
            lateinit var mockKeyCloakServer: MockWebServer
        }

        fun withTestContext(test: TestContext.() -> Unit) {
            val context =
                TestContext()
            try {
                System.setProperty(IS_BROWSER_CAN_BE_OPENED_FROM_CLI, "true")
                context.test()
            } finally {
                System.setProperty(IS_BROWSER_CAN_BE_OPENED_FROM_CLI, "false")
            }
        }

        context("success") {
            test("cloud login") {
                withTestContext {
                    runCommand(
                        commands = listOf("cloud", "login"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                                Completed compilation for 1 file in XXX ms

                                No NPL protocols found in the target directory.
                                """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }
    }) {
    companion object {
        const val IS_BROWSER_CAN_BE_OPENED_FROM_CLI = "java.awt.headless"
    }
}
