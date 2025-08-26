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
                            when (request.path) {
                                "/realms/paas/protocol/openid-connect/token" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "access_token": "mock-access-token",
                                                "refresh_token": "mock-refresh-token",
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
                                                "name": "Test Tenant",
                                                "slug": "test-tenant",
                                                "state": "active",
                                                "applications": [
                                                  {
                                                    "id": "app-123",
                                                    "name": "Test App",
                                                    "slug": "test-app",
                                                    "state": "active",
                                                    "engine_version": {
                                                      "version": "2025.1.11",
                                                      "deprecated": false
                                                    },
                                                    "deployed_at": "2025-01-01T12:00:00Z",
                                                    "namespace": "test-namespace",
                                                    "links": {
                                                      "api": "https://api.example.com"
                                                    }
                                                  }
                                                ]
                                              }
                                            ]
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
            val context = TestContext()
            try {
                context.setupMockServers()
                context.test()
            } finally {
                context.cleanupMockServers()
                File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
            }
        }

        context("success") {
            test("cloud status shows tenants and applications") {
                withTestContext {
                    // First, store a fake auth token
                    val noumenaDir = File(System.getProperty("user.home")).resolve(".noumena")
                    noumenaDir.mkdirs()
                    val tokenFile = noumenaDir.resolve("noumena.yaml")
                    tokenFile.writeText("refreshToken: mock-refresh-token")

                    runCommand(
                        commands = listOf("cloud", "status", "--url", mockNC.url("/").toString(), "--authUrl", mockOidc.url("/realms/paas/").toString()),
                    ) {
                        process.waitFor()
                        
                        val expectedOutput =
                            """
                            Cloud Status:
                            ==================================================
                            
                            Tenant: Test Tenant
                              ID: 80031abc-641b-4330-a473-16fd6d5ae305
                              Slug: test-tenant
                              State: active
                              Applications (1):
                                - Test App
                                  ID: app-123
                                  Slug: test-app
                                  State: active
                                  Engine Version: 2025.1.11
                                  Deployed At: 2025-01-01T12:00:00Z
                                  API URL: https://api.example.com
                                  Namespace: test-namespace
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud status fails when not logged in") {
                File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
                
                runCommand(
                    commands = listOf("cloud", "status"),
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
    })