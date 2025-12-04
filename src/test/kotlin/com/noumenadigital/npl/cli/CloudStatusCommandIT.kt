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

class CloudStatusCommandIT :
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
                                                  },
                                                  {
                                                    "id": "18e3316d-8a70-42c5-8b97-310860151d94",
                                                    "name": "Another App",
                                                    "slug": "another-app",
                                                    "provider": "MicrosoftAzure",
                                                    "engine_version": {
                                                      "version": "2024.2.7",
                                                      "deprecated": true
                                                    },
                                                    "state": "pending"
                                                  }
                                                ],
                                                "state": "active"
                                              },
                                              {
                                                "id": "90041def-752c-5441-b584-27ge7f6bf416",
                                                "name": "Other Tenant",
                                                "slug": "other-tenant",
                                                "external_id": null,
                                                "subscription": null,
                                                "applications": [],
                                                "state": "deactivated"
                                              }
                                            ]
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
            test("cloud status lists tenants and applications") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "status",
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
                        val expectedOutput =
                            """
                            📂 My Tenant (my-tenant) [active] 🟢
                              ├── 📦 My App (my-app) [active] 🟢
                              └── 📦 Another App (another-app) [pending] 🟡
                            📂 Other Tenant (other-tenant) [deactivated] 🔴
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud status failed login required") {
                withTestContext {
                    File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "status",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud status failed: Please login again.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud status failed invalid url") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "status",
                                "--url",
                                "invalid-url",
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud status failed: Failed to fetch tenants - Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }
        }

        context("Yaml config") {
            withTestContext {
                TestUtils.withYamlConfig(
                    """
                    cloud:
                      url: ${mockNC.url("/")}
                      authUrl: "http://localhost:${mockOidc.port}/realms/paas/"
                      clientId: paas
                      clientSecret: paas
                    """.trimIndent(),
                ) {

                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "status",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            📂 My Tenant (my-tenant) [active] 🟢
                              ├── 📦 My App (my-app) [active] 🟢
                              └── 📦 Another App (another-app) [pending] 🟡
                            📂 Other Tenant (other-tenant) [deactivated] 🔴
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }
    })
