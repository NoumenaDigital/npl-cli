package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.model.TokenResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File

class CloudLogoutCommandIT :
    FunSpec({

        class TestContext {
            val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
            var mockOidc: MockWebServer = MockWebServer()

            fun readTempFile(): TokenResponse =
                objectMapper.readValue(
                    File(System.getProperty("user.home")).resolve(".noumena").resolve("noumena.yaml"),
                    TokenResponse::class.java,
                )

            fun setupMockServers(refreshTokenToVerify: String? = null) {
                mockOidc.start()

                mockOidc.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse {
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
                                                "refresh_token": "$refreshTokenToVerify",
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
            }

            fun cleanupMockServers() {
                mockOidc.shutdown()
            }
        }

        fun withTestContext(
            refreshTokenToVerify: String? = null,
            test: TestContext.() -> Unit,
        ) {
            val context =
                TestContext()
            try {
                System.setProperty(NPL_CLI_BROWSER_DISABLED, "true")
                context.setupMockServers(refreshTokenToVerify = refreshTokenToVerify)
                context.test()
            } finally {
                context.cleanupMockServers()
                File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
            }
        }

        context("success") {
            test("cloud logout") {
                val refreshTokenToVerify = "success-refresh-token"
                withTestContext(refreshTokenToVerify) {
                    runCommand(
                        commands = listOf("cloud", "login", "--url", "${mockOidc.url("/realms/paas/")}"),
                        env =
                            mapOf(
                                NPL_CLI_BROWSER_DISABLED to "true",
                            ),
                    ) {
                        process.waitFor()
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                        readTempFile().refreshToken shouldBe refreshTokenToVerify
                    }
                    runCommand(
                        commands = listOf("cloud", "logout"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Successfully logged out of NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                        readTempFile().refreshToken shouldBe null
                    }
                }
            }
            test("cloud logout before login") {
                withTestContext {
                    runCommand(
                        commands = listOf("cloud", "logout"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Successfully logged out of NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                        readTempFile().refreshToken shouldBe null
                    }
                }
            }
        }
    }) {
    companion object {
        private const val NPL_CLI_BROWSER_DISABLED = "NPL_CLI_BROWSER_DISABLED"
    }
}
