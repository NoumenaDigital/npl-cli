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

class CloudDeployFrontendCommandIT :
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
                                                "name": "Default_tenant",
                                                "slug": "training",
                                                "external_id": null,
                                                "subscription": null,
                                                "applications": [
                                                  {
                                                    "id": "$APP_ID_OK",
                                                    "name": "existingName",
                                                    "slug": "nplintegrations",
                                                    "provider": "MicrosoftAzure",
                                                    "engine_version": {
                                                      "version": "2025.1.2",
                                                      "deprecated": false
                                                    },
                                                    "owner_id": "a3893a2a-d75b-46ea-9c84-9109ab03c891",
                                                    "trusted_issuers": [
                                                      "https://keycloak-training-nplintegrations.noumena.cloud/realms/noumena",
                                                      "https://keycloak-training-nplintegrations.noumena.cloud/realms/nplintegrations",
                                                      "http://noumenadigital.com"
                                                    ],
                                                    "state": "active",
                                                    "deployed_at": "2025-04-07T06:21:53.739573Z",
                                                    "backup_records": [],
                                                    "namespace": "training",
                                                    "configuration_id": "0662db78-fb2a-4115-ab90-ab020343c30b",
                                                    "deleted_at": null,
                                                    "links": {
                                                      "api": "https://engine-training-nplintegrations.noumena.cloud",
                                                      "graphql": "https://engine-training-nplintegrations.noumena.cloud/graphql",
                                                      "swagger": "https://engine-training-nplintegrations.noumena.cloud/swagger-ui/index.html",
                                                      "inspector": "https://inspector-training-nplintegrations.noumena.cloud",
                                                      "keycloak": "https://keycloak-training-nplintegrations.noumena.cloud/admin/master/console",
                                                      "trusted_issuers": [
                                                        "https://keycloak-training-nplintegrations.noumena.cloud/realms/noumena",
                                                        "https://keycloak-training-nplintegrations.noumena.cloud/realms/nplintegrations",
                                                        "http://noumenadigital.com"
                                                      ]
                                                    },
                                                    "add_ons": [],
                                                    "website_deployed_at": null,
                                                    "website_file_name": null,
                                                    "website_url": null
                                                  },
                                                  {
                                                    "id": "18e3316d-8a70-42c5-8b97-310860151d94",
                                                    "name": "test_old_version",
                                                    "slug": "testoldversion",
                                                    "provider": "MicrosoftAzure",
                                                    "engine_version": {
                                                      "version": "2024.2.7",
                                                      "deprecated": true
                                                    },
                                                    "owner_id": "92309f7b-f9e1-42c4-a35f-ab97241d2d6c",
                                                    "trusted_issuers": [
                                                      "https://keycloak-training-testoldversion.noumena.cloud/realms/noumena",
                                                      "https://keycloak-training-testoldversion.noumena.cloud/realms/testoldversion"
                                                    ],
                                                    "state": "active",
                                                    "deployed_at": null,
                                                    "backup_records": [],
                                                    "namespace": "training",
                                                    "configuration_id": "98ff3aed-bfac-433b-9960-a0a30b407052",
                                                    "deleted_at": null,
                                                    "links": {
                                                      "api": "https://engine-training-testoldversion.noumena.cloud",
                                                      "graphql": "https://engine-training-testoldversion.noumena.cloud/graphql",
                                                      "swagger": "https://engine-training-testoldversion.noumena.cloud/swagger-ui/index.html",
                                                      "inspector": "https://inspector-training-testoldversion.noumena.cloud",
                                                      "keycloak": "https://keycloak-training-testoldversion.noumena.cloud/admin/master/console",
                                                      "trusted_issuers": [
                                                        "https://keycloak-training-testoldversion.noumena.cloud/realms/noumena",
                                                        "https://keycloak-training-testoldversion.noumena.cloud/realms/testoldversion"
                                                      ]
                                                    },
                                                    "add_ons": [],
                                                    "website_deployed_at": null,
                                                    "website_file_name": null,
                                                    "website_url": null
                                                  }
                                                ],
                                                "state": "active"
                                              }
                                            ]
                                            """.trimIndent(),
                                        )
                                }

                                "/api/v1/applications/$APP_ID_OK/uploadwebsite" -> {
                                    return MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "id": "$APP_ID_OK",
                                                "name": "existingName",
                                                "slug": "nplintegrations",
                                                "provider": "MicrosoftAzure",
                                                "engine_version": {
                                                  "version": "2025.1.2",
                                                  "deprecated": false
                                                },
                                                "owner_id": "a3893a2a-d75b-46ea-9c84-9109ab03c891",
                                                "trusted_issuers": [
                                                  "https://keycloak-training-nplintegrations.noumena.cloud/realms/noumena",
                                                  "https://keycloak-training-nplintegrations.noumena.cloud/realms/nplintegrations",
                                                  "http://noumenadigital.com"
                                                ],
                                                "state": "active",
                                                "deployed_at": "2025-04-07T06:21:53.739573Z",
                                                "backup_records": [],
                                                "namespace": "training",
                                                "configuration_id": "0662db78-fb2a-4115-ab90-ab020343c30b",
                                                "deleted_at": null,
                                                "links": {
                                                  "api": "https://engine-training-nplintegrations.noumena.cloud",
                                                  "graphql": "https://engine-training-nplintegrations.noumena.cloud/graphql",
                                                  "swagger": "https://engine-training-nplintegrations.noumena.cloud/swagger-ui/index.html",
                                                  "inspector": "https://inspector-training-nplintegrations.noumena.cloud",
                                                  "keycloak": "https://keycloak-training-nplintegrations.noumena.cloud/admin/master/console",
                                                  "trusted_issuers": [
                                                    "https://keycloak-training-nplintegrations.noumena.cloud/realms/noumena",
                                                    "https://keycloak-training-nplintegrations.noumena.cloud/realms/nplintegrations",
                                                    "http://noumenadigital.com"
                                                  ]
                                                },
                                                "add_ons": [],
                                                "website_deployed_at": "2025-07-11T08:23:27.339077571Z",
                                                "website_file_name": "training_nplintegrations_20250711_082327.zip",
                                                "website_url": "https://training-nplintegrations.noumena.cloud",
                                                "tenant_id": "80031abc-641b-4330-a473-16fd6d5ae305"
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
                mockNC.shutdown()
            }
        }

        fun withTestContext(test: TestContext.() -> Unit) {
            val context =
                TestContext()
            try {
                System.setProperty("NPL_CLI_BROWSER_DISABLED", "true")
                context.setupMockServers()
                runCommand(
                    commands = listOf("cloud", "login", "--url", context.mockOidc.url("/realms/paas/").toString()),
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
            test("cloud deploy success") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "existingName",
                                "--tenant",
                                "default_tenant",
                                "--buildDir",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Frontend successfully deployed to NOUMENA Cloud.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }
        }

        context("error") {
            test("cloud deploy failed wrong clientId") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--clientId",
                                "wrong",
                                "--app",
                                "existingName",
                                "--tenant",
                                "default_tenant",
                                "--buildDir",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy frontend failed: Cannot get access token 401 - Client Error.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy failed deploy command") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "existingName",
                                "--tenant",
                                "default_tenant",
                                "--buildDir",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                "non-url",
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy frontend failed: Failed to fetch tenants - Target host is not specified.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy failed login required") {
                withTestContext {
                    File(System.getProperty("user.home")).resolve(".noumena").deleteRecursively()
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "existingName",
                                "--tenant",
                                "default_tenant",
                                "--buildDir",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                "non-url",
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy frontend failed: Please login again.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                    }
                }
            }

            test("cloud deploy no application found") {
                withTestContext {
                    runCommand(
                        commands =
                            listOf(
                                "cloud",
                                "deploy",
                                "frontend",
                                "--app",
                                "notExistingName",
                                "--tenant",
                                "default_tenant",
                                "--buildDir",
                                "src/test/resources/frontend-sources/deploy-success/build",
                                "--url",
                                mockNC.url("/").toString(),
                                "--authUrl",
                                mockOidc.url("/realms/paas/").toString(),
                            ),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                            Command cloud deploy frontend failed: Failed to upload application archive - Application name notExistingName doesn't exist for tenant default_tenant.
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
