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

class CloudServiceAccountDeployFrontendCommandIT :
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

                                "/api/v1/applications/$APP_ID_OK/uploadwebsite" -> {
                                    MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "id": "$APP_ID_OK",
                                                "name": "appname",
                                                "slug": "appslug",
                                                "website_deployed_at": "2025-07-11T08:23:27.339077571Z",
                                                "website_file_name": "tenantslug_appslug_20250711_082327.zip",
                                                "website_url": "https://tenantslug-appslug.noumena.cloud"
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
            test("cloud service-deploy-frontend success") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--frontend",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy frontend to NOUMENA Cloud using service account...
                            Successfully authenticated with service account credentials
                            Frontend successfully deployed to NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud deploy frontend via service account failed wrong clientSecret") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--frontend",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "wrong",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy frontend to NOUMENA Cloud using service account..
                            Command cloud deploy frontend failed: Cannot get access token using client credentials 401 - Client Error.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy frontend via service account failed deploy command") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--frontend",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                "non-url",
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy frontend to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud deploy frontend failed: Failed to fetch tenants - Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy frontend via service account no application found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "notappslug",
                                "--tenant",
                                "tenantslug",
                                "--frontend",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy frontend to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud deploy frontend failed: Failed to upload application archive - Application slug notappslug doesn't exist for tenant tenantslug.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy frontend via service account no build dir found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--frontend",
                                "other-build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--auth-url",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy frontend failed: Build directory does not exist or is not a directory - other-build
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
