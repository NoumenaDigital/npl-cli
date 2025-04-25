package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.config.DeployTarget
import com.noumenadigital.npl.cli.config.EngineConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DeployCommandIT :
    FunSpec({

        // Test helper to verify multipart form data content
        fun RecordedRequest.containsFileWithName(name: String): Boolean {
            val body = body.readUtf8()
            return body.contains("name=\"archive\"") &&
                body.contains("filename=\"$name\"") &&
                body.contains("Content-Type: application/zip")
        }

        // Helper function to create a test config file
        fun createConfigFile(
            tempDir: File,
            serverPort: Int,
        ): File {
            // Create .noumena directory
            val configDir = File(tempDir, ".noumena")
            configDir.mkdirs()

            // Create config file
            val configFile = File(configDir, "config.json")

            val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

            val engineConfig =
                EngineConfig(
                    targets =
                        mapOf(
                            "test-target" to
                                DeployTarget(
                                    engineManagementUrl = "http://localhost:$serverPort",
                                    authUrl = "http://localhost:$serverPort/auth",
                                    username = "test-user",
                                    password = "test-password",
                                    clientId = "test-client",
                                    clientSecret = "test-secret",
                                ),
                        ),
                )

            mapper.writeValue(configFile, engineConfig)
            return configFile
        }

        // Set the system property to use a temporary config directory
        fun withConfigDir(
            tempDir: File,
            test: () -> Unit,
        ) {
            val originalUserHome = System.getProperty("user.home")
            try {
                System.setProperty("user.home", tempDir.absolutePath)
                test()
            } finally {
                System.setProperty("user.home", originalUserHome)
            }
        }

        context("deploy sources") {
            test("successful deployment") {
                // Create temporary directory for config
                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    // Create a new server instance for this test
                    val server = MockWebServer()
                    server.start()

                    // Set up dispatcher for all endpoints
                    server.dispatcher =
                        object : Dispatcher() {
                            override fun dispatch(request: RecordedRequest): MockResponse {
                                println("Mock server received request: ${request.method} ${request.path}")

                                return when {
                                    request.path == "/auth/.well-known/openid-configuration" ||
                                        request.path == "/.well-known/openid-configuration" -> {
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                """
                                                {
                                                    "issuer": "http://localhost:${server.port}/auth",
                                                    "authorization_endpoint": "http://localhost:${server.port}/auth/oauth/authorize",
                                                    "token_endpoint": "http://localhost:${server.port}/auth/oauth/token",
                                                    "userinfo_endpoint": "http://localhost:${server.port}/auth/oauth/userinfo",
                                                    "jwks_uri": "http://localhost:${server.port}/auth/oauth/jwks",
                                                    "response_types_supported": ["code", "token"],
                                                    "subject_types_supported": ["public"],
                                                    "id_token_signing_alg_values_supported": ["RS256"]
                                                }
                                                """.trimIndent(),
                                            )
                                    }

                                    request.path == "/auth/oauth/token" -> {
                                        // Token endpoint
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                """
                                                {
                                                    "access_token": "mock-access-token",
                                                    "token_type": "bearer",
                                                    "expires_in": 3600,
                                                    "refresh_token": "mock-refresh-token"
                                                }
                                                """.trimIndent(),
                                            )
                                    }

                                    // Handle all other endpoints with success
                                    else -> {
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setBody("""{"message": "Success"}""")
                                    }
                                }
                            }
                        }

                    // Create config file with test target pointing to the server
                    createConfigFile(tempDir, server.port)

                    // Get test directory with NPL sources
                    val testDirPath = getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()

                    // Run the test with the temporary config
                    withConfigDir(tempDir) {
                        runCommand(
                            commands = listOf("deploy", "test-target", testDirPath),
                        ) {
                            process.waitFor(5, TimeUnit.SECONDS)

                            val expectedOutput =
                                """
                            Creating NPL deployment archive...
                            Deploying NPL sources and migrations to http://localhost:${server.port}...
                            Successfully deployed NPL sources and migrations to target 'test-target'.
                        """.normalize()

                            output.normalize() shouldBe expectedOutput
                            process.exitValue() shouldBe ExitCode.SUCCESS.code
                        }
                    }

                    // Clean up
                    server.shutdown()
                } finally {
                    tempDir.deleteRecursively()
                }
            }

            test("server returns error") {
                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    val server = MockWebServer()
                    server.start()

                    server.dispatcher =
                        object : Dispatcher() {
                            override fun dispatch(request: RecordedRequest): MockResponse =
                                when {
                                    request.path == "/auth/.well-known/openid-configuration" ||
                                        request.path == "/.well-known/openid-configuration" -> {
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                """
                                                {
                                                    "issuer": "http://localhost:${server.port}/auth",
                                                    "authorization_endpoint": "http://localhost:${server.port}/auth/oauth/authorize",
                                                    "token_endpoint": "http://localhost:${server.port}/auth/oauth/token",
                                                    "userinfo_endpoint": "http://localhost:${server.port}/auth/oauth/userinfo",
                                                    "jwks_uri": "http://localhost:${server.port}/auth/oauth/jwks",
                                                    "response_types_supported": ["code", "token"],
                                                    "subject_types_supported": ["public"],
                                                    "id_token_signing_alg_values_supported": ["RS256"]
                                                }
                                                """.trimIndent(),
                                            )
                                    }

                                    request.path == "/auth/oauth/token" -> {
                                        // Token endpoint
                                        MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                """
                                                {
                                                    "access_token": "mock-access-token",
                                                    "token_type": "bearer",
                                                    "expires_in": 3600,
                                                    "refresh_token": "mock-refresh-token"
                                                }
                                                """.trimIndent(),
                                            )
                                    }

                                    request.path?.contains("/api/deployment") == true -> {
                                        // Deployment endpoint - return an error
                                        MockResponse()
                                            .setResponseCode(500)
                                            .setBody("""{"error": "Internal server error"}""")
                                    }

                                    else -> {
                                        MockResponse().setResponseCode(404)
                                    }
                                }
                        }

                    createConfigFile(tempDir, server.port)

                    val testDirPath = getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()

                    withConfigDir(tempDir) {
                        runCommand(
                            commands = listOf("deploy", "test-target", testDirPath),
                        ) {
                            process.waitFor(5, TimeUnit.SECONDS)

                            // Check for error message in output
                            output.normalize() shouldContain "Error deploying NPL sources"
                            process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                        }
                    }

                    server.shutdown()
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        context("deploy error handling") {
            test("missing target") {
                runCommand(
                    commands = listOf("deploy"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        """
                    Missing required parameter: target
                    Usage: deploy <target> [directory] [--clean]

                    Arguments:
                      target           Named target from config.json to deploy to
                      directory        Directory containing NPL sources (defaults to current directory)

                    Options:
                      --clean          Clear application contents before deployment

                    Configuration is read from .noumena/config.json in the current directory
                    or the user's home directory.
                """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("target not found in config") {
                val testDirPath = getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()

                runCommand(
                    commands = listOf("deploy", "nonexistent-target", testDirPath),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        """
                    Configuration errors:
                      Target 'nonexistent-target' not found in configuration

                    Please create a configuration file at .noumena/config.json
                    with the following format:
                    {
                      "targets": {
                        "nonexistent-target": {
                          "engineManagementUrl": "<URL of the Noumena Engine API>",
                          "authUrl": "<URL of the authentication endpoint>",
                          "username": "<username for authentication>",
                          "password": "<password for authentication>",
                          "clientId": "<client ID for authentication>",
                          "clientSecret": "<client secret for authentication>"
                        }
                      }
                    }
                """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
                }
            }

            test("invalid directory path") {
                // Use a non-existent directory path
                val nonExistentDir = "/non/existent/directory"

                runCommand(
                    commands = listOf("deploy", "test-target", nonExistentDir),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    output.normalize().contains("Target directory does not exist") shouldBe true
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("not a directory") {
                // Create a temporary file
                val tempFile = File.createTempFile("test", ".txt")
                tempFile.deleteOnExit()

                runCommand(
                    commands = listOf("deploy", "test-target", tempFile.absolutePath),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    output.normalize().contains("Target path is not a directory") shouldBe true
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }
    })
