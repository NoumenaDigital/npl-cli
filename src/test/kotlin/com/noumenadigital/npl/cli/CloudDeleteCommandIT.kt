package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File

class CloudDeleteCommandIT :
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
                                "/api/v1/tenants" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            [
                                              {
                                                "id": "80031abc-641b-4330-a473-16fd6d5ae305",
                                                "name": "My Tenant",
                                                "slug": "my-tenant",
                                                "external_id": null,
                                                "subscription": null,
                                                "applications": [
                                                  {
                                                    "id": "1a978a70-1709-40c1-82d7-30114edfc46b",
                                                    "name": "My App",
                                                    "slug": "my-app",
                                                    "provider": "MicrosoftAzure",
                                                    "engine_version": {
                                                      "version": "2025.1.2",
                                                      "deprecated": false
                                                    },
                                                    "state": "active"
                                                  }
                                                ],
                                                "state": "active"
                                              }
                                            ]
                                            """.trimIndent(),
                                        )
                                }

                                "/api/v1/applications/1a978a70-1709-40c1-82d7-30114edfc46b/delete" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody("")
                                }
                            }
                            return MockResponse().setResponseCode(404).setBody("Not found")
                        }
                    }
            }

            fun cleanupMockServers() {
                mockOidc.shutdown()
                mockNC.shutdown()
            }
        }

        fun withTestContext(test: TestContext.() -> Unit) {
            val context = TestContext()
            try {
                System.setProperty("NPL_CLI_BROWSER_DISABLED", "true")
                context.setupMockServers()
                runCommand(
                    commands =
                        listOf(
                            "cloud",
                            "login",
                            "--auth-url",
                            context.mockOidc.url("/realms/paas/").toString(),
                            "--client-id",
                            "client-id-ok",
                            "--client-secret",
                            "client-secret-ok",
                        ),
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
            test("cloud delete deletes an application") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "delete",
                                "--tenant",
                                "my-tenant",
                                "--app",
                                "my-app",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "client-id-ok",
                                "--client-secret",
                                "client-secret-ok",
                            ),
                    ) {
                        process.waitFor()
                        output shouldContain "Application 'my-app' deleted successfully from tenant 'my-tenant'!"
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud delete fails without required parameters") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "delete",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "client-id-ok",
                                "--client-secret",
                                "client-secret-ok",
                            ),
                    ) {
                        process.waitFor()
                        output shouldContain "Missing required parameter(s): tenant, app"
                        process.exitValue() shouldBe ExitCode.USAGE_ERROR.code
                    }
                }
            }

            test("cloud delete fails with non-existent tenant") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "delete",
                                "--tenant",
                                "non-existent-tenant",
                                "--app",
                                "my-app",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "client-id-ok",
                                "--client-secret",
                                "client-secret-ok",
                            ),
                    ) {
                        process.waitFor()
                        output shouldContain "Tenant 'non-existent-tenant' not found"
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud delete fails with non-existent application") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "delete",
                                "--tenant",
                                "my-tenant",
                                "--app",
                                "non-existent-app",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "client-id-ok",
                                "--client-secret",
                                "client-secret-ok",
                            ),
                    ) {
                        process.waitFor()
                        output shouldContain "Application 'non-existent-app' not found in tenant 'my-tenant'"
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }
        }
    })
