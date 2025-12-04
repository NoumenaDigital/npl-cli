package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.File

class CloudServiceAccountDeployNplCommandIT :
    FunSpec({

        class TestContext {
            var mockOidc: MockWebServer = MockWebServer()
            var mockNC: MockWebServer = MockWebServer()

            val additionalNcPaths =
                mapOf(
                    "/api/v1/applications/$APP_ID_OK/deploy" to
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(
                                """
                                {
                                  "status": "deployed"
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
            test("cloud deploy npl via service account success") {
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
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
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
        }

        context("error") {
            test("cloud deploy npl via service account failed wrong clientSecret") {
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
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "wrong",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Command cloud deploy npl failed: Cannot get access token using client credentials 401 - Client Error.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy npl via service account failed deploy command") {
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
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud deploy npl failed: Failed to fetch tenants - Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy npl via service account no application found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "npl",
                                "--app",
                                "notappslug",
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
                        env =
                            mapOf(
                                "NPL_SERVICE_ACCOUNT_CLIENT_SECRET" to "svc-secret",
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Preparing to deploy NPL application to NOUMENA Cloud using service account..
                            Successfully authenticated with service account credentials
                            Command cloud deploy npl failed: Failed to upload application archive - Application slug notappslug doesn't exist for tenant slug tenantslug.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy npl via service account no migration found") {
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
                                "other-migration.yml",
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
                            Command cloud deploy npl failed: Migration file does not exist - other-migration.yml
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }
        }
    })
