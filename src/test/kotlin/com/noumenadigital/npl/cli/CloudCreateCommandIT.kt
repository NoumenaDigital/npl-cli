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

class CloudCreateCommandIT :
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

                                "/api/v1/engine/versions" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            ["2025.2.0", "2025.1.2", "2024.2.7"]
                                            """.trimIndent(),
                                        )
                                }

                                "/api/v1/utils/slug?slug=New+App" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody("\"new-app\"")
                                }

                                "/api/v1/tenants/80031abc-641b-4330-a473-16fd6d5ae305/createApplication" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                              "id": "new-app-id-123",
                                              "name": "New App",
                                              "slug": "new-app",
                                              "state": "pending"
                                            }
                                            """.trimIndent(),
                                        )
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
            test("cloud create creates a new application") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "create",
                                "--tenant",
                                "my-tenant",
                                "--app-name",
                                "New App",
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
                        output shouldContain "Application created successfully!"
                        output shouldContain "Name: New App"
                        output shouldContain "Slug: new-app"
                        output shouldContain "ID: new-app-id-123"
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }

            test("cloud create with custom engine version") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "create",
                                "--tenant",
                                "my-tenant",
                                "--app-name",
                                "New App",
                                "--engine-version",
                                "2024.2.7",
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
                        output shouldContain "Application created successfully!"
                        output shouldContain "Engine Version: 2024.2.7"
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud create fails without required parameters") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "create",
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
                        output shouldContain "Missing required parameter(s): tenant, app-name"
                        process.exitValue() shouldBe ExitCode.USAGE_ERROR.code
                    }
                }
            }

            test("cloud create fails with non-existent tenant") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "create",
                                "--tenant",
                                "non-existent-tenant",
                                "--app-name",
                                "New App",
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
        }
    })
