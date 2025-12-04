package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.File

class CloudServiceAccountDeployFrontendCommandIT :
    FunSpec({

        class TestContext {
            var mockOidc: MockWebServer = MockWebServer()
            var mockNC: MockWebServer = MockWebServer()

            val additionalNcPaths =
                mapOf(
                    "/api/v1/applications/$APP_ID_OK/uploadwebsite" to
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
                            ),
                )

            fun setupMockServers() {
                mockOidc = createOidcMockServer()
                mockNC = createNcMockServer(additionalNcPaths)
                mockOidc.start()
                mockNC.start()
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
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
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
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
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
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
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
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
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
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "paas",
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
    })
