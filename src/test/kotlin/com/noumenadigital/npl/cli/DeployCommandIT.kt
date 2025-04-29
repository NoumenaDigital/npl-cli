package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.config.DeployConfig
import com.noumenadigital.npl.cli.config.EngineTargetConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DeployCommandIT :
    FunSpec({

        // Set up the mock web server for each test
        lateinit var mockKeycloak: MockWebServer
        lateinit var mockEngine: MockWebServer

        fun setupMockServers() {
            mockKeycloak = MockWebServer()
            mockEngine = MockWebServer()

            // Start them on random ports
            mockKeycloak.start()
            mockEngine.start()

            mockKeycloak.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        when (request.path) {
                            "/realms/noumena/.well-known/openid-configuration" -> {
                                return MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "application/json")
                                    .setBody(
                                        """
                                        {
                                            "token_endpoint": "${mockKeycloak.url("/realms/noumena/protocol/openid-connect/token")}"
                                        }
                                        """.trimIndent(),
                                    )
                            }

                            "/realms/noumena/protocol/openid-connect/token" -> {
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
        }

        fun cleanupMockServers() {
            mockKeycloak.shutdown()
            mockEngine.shutdown()
        }

        fun createConfigFile(
            tempDir: File,
            engineManagementUrl: String,
            keycloakAuthUrl: String,
        ): File {
            val configDir = File(tempDir, ".noumena")
            configDir.mkdirs()

            val configFile = File(configDir, "config.json")

            val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

            val deployConfig =
                DeployConfig(
                    targets =
                        mapOf(
                            "test-target" to
                                EngineTargetConfig(
                                    engineManagementUrl = engineManagementUrl,
                                    authUrl = "${keycloakAuthUrl}realms/noumena",
                                    username = "user1",
                                    password = "password1",
                                    clientId = "nm-platform-service-client",
                                    clientSecret = "87ff12ca-cf29-4719-bda8-c92faa78e3c4",
                                ),
                        ),
                )

            mapper.writeValue(configFile, deployConfig)
            return configFile
        }

        fun withConfigDir(
            tempDir: File,
            test: () -> Unit,
        ) {
            val originalUserHome = System.getProperty("user.home")
            try {
                System.setProperty("user.home", tempDir.absolutePath)

                // Also create a config file in the current directory for JAR mode
                val localConfigDir = File(".noumena")
                localConfigDir.mkdirs()
                File(tempDir, ".noumena/config.json").copyTo(
                    File(localConfigDir, "config.json"),
                    overwrite = true,
                )

                test()
            } finally {
                // Clean up the local config file
                File(".noumena/config.json").delete()
                System.setProperty("user.home", originalUserHome)
            }
        }

        fun executeDeployCommand(
            tempDir: File,
            testDirPath: String,
            withClean: Boolean = false,
        ): Pair<String, Int> {
            var output = ""
            var exitCode = -1

            // Configure the user.home system property to point to our temp directory
            withConfigDir(tempDir) {
                val commands =
                    if (withClean) {
                        listOf("deploy", "test-target", testDirPath, "--clean")
                    } else {
                        listOf("deploy", "test-target", testDirPath)
                    }

                runCommand(commands = commands) {
                    process.waitFor(60, TimeUnit.SECONDS)
                    output = this.output
                    exitCode = process.exitValue()
                }
            }

            return Pair(output, exitCode)
        }

        beforeTest {
            setupMockServers()
        }

        afterTest {
            cleanupMockServers()
        }

        // Main use cases first
        context("successful deployments") {
            test("simple deploy") {
                // Configure the mock engine to respond to deployment requests
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                val engineUrl = mockEngine.url("/").toString()
                val keycloakUrl = mockKeycloak.url("/").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, keycloakUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    val (output, exitCode) = executeDeployCommand(tempDir, testDirPath)

                    output.normalize() shouldBe
                        """
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Successfully deployed NPL sources and migrations to target 'test-target'.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }

            test("deploy with clean flag") {
                // Configure the mock engine to respond to clear application request
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                // Configure the mock engine to respond to deployment request
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                val engineUrl = mockEngine.url("").toString()
                val keycloakUrl = mockKeycloak.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, keycloakUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    // Execute the deploy command with clean flag
                    val (output, exitCode) = executeDeployCommand(tempDir, testDirPath, withClean = true)

                    output.normalize() shouldBe
                        """
                        Clearing application contents...
                        Application contents cleared
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Successfully deployed NPL sources and migrations to target 'test-target'.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        // Error cases during deployment
        context("deployment errors") {
            test("when deploying NPL sources with compilation errors") {
                // Configure the mock engine to respond to clear application request
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                // Configure the mock engine to respond with an error for deploying invalid sources
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(400)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                                "errorType": "sourceError",
                                "message": "'1' source errors encountered:\nclass SourceErrorDetail {\n    code: 0001\n    description: /npl-1.0/objects/iou/iou.npl: (1, 9) E0001: Syntax error: missing {<EOF>, ';'} at 'NPL'\n}",
                                "errors": [
                                    {
                                        "code": "0001",
                                        "description": "/npl-1.0/objects/iou/iou.npl: (1, 9) E0001: Syntax error: missing {<EOF>, ';'} at 'NPL'"
                                    }
                                ]
                            }
                            """.trimIndent(),
                        ),
                )

                val engineUrl = mockEngine.url("").toString()
                val keycloakUrl = mockKeycloak.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, keycloakUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-failure", "main")).toAbsolutePath().toString()

                    val (output, exitCode) =
                        executeDeployCommand(
                            tempDir = tempDir,
                            testDirPath = testDirPath,
                            withClean = true,
                        )

                    output.normalize() shouldBe
                        """
                        Clearing application contents...
                        Application contents cleared
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Error deploying NPL sources: '1' source errors encountered:
                        class SourceErrorDetail {
                            code: 0001
                            description: /npl-1.0/objects/iou/iou.npl: (1, 9) E0001: Syntax error: missing {<EOF>, ';'} at 'NPL'
                        }
                        """.trimIndent()

                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }

            test("when deploying without required migration file") {
                // Configure the mock engine to respond to clear application request
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                // Configure the mock engine to respond with migration error
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                                "errorType": "server",
                                "message": "Unknown exception: 'Could not locate `migration.yml` in zip:file:/tmp/npl-deployment-${mockEngine.hostName}.zip'"
                            }
                            """.trimIndent(),
                        ),
                )

                val engineUrl = mockEngine.url("").toString()
                val keycloakUrl = mockKeycloak.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, keycloakUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources", "src", "main"))
                            .toAbsolutePath()
                            .toString()

                    // Execute the deploy command
                    val (output, exitCode) =
                        executeDeployCommand(
                            tempDir = tempDir,
                            testDirPath = testDirPath,
                            withClean = true,
                        )

                    output.normalize() shouldBe
                        """
                        Clearing application contents...
                        Application contents cleared
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Error deploying NPL sources: Unknown exception: 'Could not locate `migration.yml` in zip:file:/tmp/npl-deployment-${mockEngine.hostName}.zip'
                        """.trimIndent()

                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        // Edge cases and validation errors
        context("command validation errors") {
            test("missing target parameter") {
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

            test("target not found in configuration") {
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
                          "type": "engine",
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

            test("file provided instead of directory") {
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
