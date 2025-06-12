package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File

class CloudDeployCommandIT :
    FunSpec({

        class TestContext {
            var mockOidc: MockWebServer = MockWebServer()
            var mockNC: MockWebServer = MockWebServer()

            fun setupMockServers() {
                mockOidc.start()
                mockNC.start()

                mockOidc.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse {
                            if (request.body.toString().contains("wrong")) {
                                return MockResponse().setResponseCode(401)
                            }
                            when (request.path) {
                                "/realms/paas/protocol/openid-connect/auth/device" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                              "device_code": "mock-device-code",
                                              "user_code": "mock-user-code",
                                              "verification_uri": "https://verification-uri.com",
                                              "verification_uri_complete": "verification-uri-complete",
                                              "expires_in": 600,
                                              "interval": 5
                                            }
                                            """.trimIndent(),
                                        )
                                }

                                "/realms/paas/protocol/openid-connect/token" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "access_token": "mock-access-token",
                                                "refresh_token": "refresh-token-ok",
                                                "token_type": "bearer",
                                                "expires_in": 3600
                                            }
                                            """.trimIndent(),
                                        )
                                }
                            }
                            return MockResponse().setResponseCode(404)
                        }
                    }

                mockNC.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse {
                            when (request.path) {
                                "/api/v1/applications/$APP_ID_OK/deploy" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                              "status": "deployed",
                                            }
                                            """.trimIndent(),
                                        )
                                }
                            }
                            return MockResponse().setResponseCode(404)
                        }
                    }
            }

            fun cleanupMockServers() {
                mockOidc.shutdown()
                mockNC.shutdown()
            }
        }

        fun withTestContext(test: TestContext.() -> Unit) {
            val context =
                TestContext()
            try {
                System.setProperty("NPL_CLI_BROWSER_DISABLED", "true")
                context.setupMockServers()
                runCommand(
                    commands = listOf("cloud", "login", "--url", context.mockOidc.url("/realms/paas/").toString()),
                    env = mapOf("NPL_CLI_BROWSER_DISABLED" to "true"),
                ) {
                    process.waitFor()
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
                context.test()
            } finally {
                context.cleanupMockServers()
                File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
                System.setProperty("NPL_CLI_BROWSER_DISABLED", "false")
            }
        }

        context("success") {
            test("cloud deploy success") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "--tenant",
                                "Training",
                                "--appId",
                                APP_ID_OK,
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--ncUrl",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            NPL Application successfully deployed to NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud deploy failed wrong clientId") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "--clientId",
                                "wrong",
                                "--tenant",
                                "Training",
                                "--appId",
                                APP_ID_OK,
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--ncUrl",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy failed: Cannot get access token 401 - Client Error.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy failed deploy command") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "--tenant",
                                "Training",
                                "--appId",
                                APP_ID_OK,
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--ncUrl",
                                "non-url",
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy failed: Failed to upload application archive: Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy failed login required") {
                withTestContext {
                    File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "--tenant",
                                "Training",
                                "--appId",
                                APP_ID_OK,
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--ncUrl",
                                "non-url",
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy failed: Please login again.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy no sources found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "--tenant",
                                "Training",
                                "--appId",
                                APP_ID_OK,
                                "--migration",
                                "notexists",
                                "--ncUrl",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy failed: Migration file does not exist: notexists
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }
        }
    }) {
    companion object {
        private const val APP_ID_OK = "1a978a70-1709-40c1-82d7-30114edfc46b"
    }
}
