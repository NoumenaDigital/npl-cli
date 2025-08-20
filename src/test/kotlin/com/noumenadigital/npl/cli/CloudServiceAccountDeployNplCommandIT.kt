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

class CloudServiceAccountDeployNplCommandIT :
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
                            return when (request.path) {
                                "/realms/paas/protocol/openid-connect/token" -> {
                                    val body = request.body.readUtf8()
                                    if (body.contains("client_id=wrong") || body.contains("client_secret=wrong")) {
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
                    }

                mockNC.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse {
                            return when (request.path) {
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

                                else -> MockResponse().setResponseCode(404)
                            }
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
            test("cloud service-deploy success") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "service-deploy",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--clientId",
                                "svc-id",
                                "--clientSecret",
                                "svc-secret",
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account...
                            Successfully authenticated with service account credentials
                            NPL Application successfully deployed to NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }

            test("cloud service-deploy success using system properties credentials") {
                withTestContext {
                    System.setProperty("NPL_SA_CLIENT_ID", "svc-id")
                    System.setProperty("NPL_SA_CLIENT_SECRET", "svc-secret")
                    try {
                        runCommand(
                            commands =
                                listOf(
                                    "cloud",
                                    "service-deploy",
                                    "--app",
                                    "appslug",
                                    "--tenant",
                                    "tenantslug",
                                    "--migration",
                                    "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                    "--url",
                                    mockNC.url("/").toString(),
                                    "--authUrl",
                                    mockOidc.url("/realms/paas/").toString(),
                                ),
                        ) {
                            process.waitFor()
                            val expectedOutput =
                                """
                                Preparing to deploy NPL application to NOUMENA Cloud using service account...
                                Successfully authenticated with service account credentials
                                NPL Application successfully deployed to NOUMENA Cloud.
                                """.normalize()

                            output.normalize() shouldBe expectedOutput
                            process.exitValue() shouldBe ExitCode.SUCCESS.code
                        }
                    } finally {
                        System.clearProperty("NPL_SA_CLIENT_ID")
                        System.clearProperty("NPL_SA_CLIENT_SECRET")
                    }
                }
            }
        }

        context("error") {
            test("cloud service-deploy failed wrong clientId") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "service-deploy",
                                "--clientId",
                                "wrong",
                                "--clientSecret",
                                "svc-secret",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Command cloud service-deploy failed: Cannot get access token using client credentials 401 - Client Error.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud service-deploy failed deploy command") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "service-deploy",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--clientId",
                                "svc-id",
                                "--clientSecret",
                                "svc-secret",
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--url",
                                "non-url",
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud service-deploy failed: Failed to fetch tenants - Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud service-deploy no application found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "service-deploy",
                                "--app",
                                "notappslug",
                                "--tenant",
                                "tenantslug",
                                "--clientId",
                                "svc-id",
                                "--clientSecret",
                                "svc-secret",
                                "--migration",
                                "src/test/resources/npl-sources/deploy-success/main/migration.yml",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud service-deploy failed: Failed to upload application archive - Application slug notappslug doesn't exist for tenant slug tenantslug.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud service-deploy no migration found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "service-deploy",
                                "--app",
                                "appslug",
                                "--tenant",
                                "tenantslug",
                                "--clientId",
                                "svc-id",
                                "--clientSecret",
                                "svc-secret",
                                "--migration",
                                "other-migration.yml",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud service-deploy failed: Migration file does not exist - other-migration.yml
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
