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

class CloudServiceAccountClearCommandIT :
    FunSpec({

        class TestContext {
            var mockOidc: MockWebServer = MockWebServer()
            var mockNC: MockWebServer = MockWebServer()

            fun setupMockServers() {
                mockOidc.start()
                mockNC.start()

                mockOidc.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when (request.path) {
                                "/realms/paas/protocol/openid-connect/token" -> {
                                    val body = request.body.readUtf8()
                                    if (body.contains("client_secret=wrong")) {
                                        MockResponse()
                                            .setResponseCode(401)
                                            .setBody("Client Error.")
                                    } else if (body.contains("grant_type=client_credentials")) {
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setHeader("Content-Type", "application/json")
                                            .setBody(
                                                """
                                                {
                                                    "access_token": "mock-access-token",
                                                    "token_type": "bearer",
                                                    "expires_in": 3600
                                                }
                                                """.trimIndent(),
                                            )
                                    } else {
                                        MockResponse().setResponseCode(400)
                                    }
                                }
                                else -> MockResponse().setResponseCode(404)
                            }
                    }

                mockNC.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when (request.path) {
                                "/api/v1/tenants" -> {
                                    MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            [
                                              {
                                                "id": "80031abc-641b-4330-a473-16fd6d5ae305",
                                                "name": "tenantname",
                                                "slug": "tenantslug",
                                                "applications": [
                                                  {
                                                    "id": "$APP_ID_OK",
                                                    "name": "appname",
                                                    "slug": "appslug"
                                                  }
                                                ]
                                              }
                                            ]
                                            """.trimIndent(),
                                        )
                                }
                                "/api/v1/applications/$APP_ID_OK/deploy" -> {
                                    MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                              "status": "deployed"
                                            }
                                            """.trimIndent(),
                                        )
                                }
                                "/api/v1/applications/$APP_ID_OK/clear" -> {
                                    MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                              "status": "removed"
                                            }
                                            """.trimIndent(),
                                        )
                                }
                                else -> MockResponse().setResponseCode(404)
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
            test("cloud clear success") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "npl",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                        env = mapOf("NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret"),
                    ) {
                        process.waitFor()
                        val expectedDeployOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account...
                            Successfully authenticated with service account credentials
                            NPL Application successfully deployed to NOUMENA Cloud.
                            """.normalize()
                        output.normalize() shouldBe expectedDeployOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }

                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "clear",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                        env = mapOf("NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret"),
                    ) {
                        process.waitFor()
                        val expectedClearOutput =
                            """
                            Preparing to clear NPL sources to NOUMENA Cloud using service account...
                            Successfully authenticated with service account credentials
                            NPL sources successfully cleared from NOUMENA Cloud app.
                            """.normalize()
                        output.normalize() shouldBe expectedClearOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud clear with wrong client secret fails") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "clear",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                        env = mapOf("NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "wrong"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to clear NPL sources to NOUMENA Cloud using service account..
                            Command cloud clear failed: Cannot get access token using client credentials 401 - Client Error.
                            """.normalize()
                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud clear with non existing app fails") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "clear",
                                "--app",
                                "notappslug",
                                "--tenant",
                                "tenantslug",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
                            ),
                        env = mapOf("NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret"),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to clear NPL sources to NOUMENA Cloud using service account...
                            Successfully authenticated with service account credentials
                            Command cloud clear failed: Failed to remove the application -  Application slug notappslug doesn't exist for tenant slug tenantslug.
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
