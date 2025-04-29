package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
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

        lateinit var mockKeycloak: MockWebServer
        lateinit var mockEngine: MockWebServer

        fun setupMockServers() {
            mockKeycloak = MockWebServer()
            mockEngine = MockWebServer()

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
            val configDir = File(tempDir, ".npl")
            configDir.mkdirs()

            val configFile = File(configDir, "deploy.yml")

            val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

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

                val localConfigDir = File(".npl")
                localConfigDir.mkdirs()
                File(tempDir, ".npl/deploy.yml").copyTo(
                    File(localConfigDir, "deploy.yml"),
                    overwrite = true,
                )

                test()
            } finally {
                File(".npl/deploy.yml").delete()
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

        context("successful deployments") {
            test("simple deploy") {
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
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

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

        context("deployment errors") {
            test("when deploying NPL sources with compilation errors") {
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

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
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

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

        context("command validation errors") {
            test("missing target parameter") {
                runCommand(
                    commands = listOf("deploy"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        """
                    Missing required parameter: target
                    Usage: deploy <target> <directory> [--clean]

                    Arguments:
                      target           Named target from deploy.yml to deploy to
                      directory        Directory containing NPL sources.
                                       IMPORTANT: The directory must contain a valid NPL source structure, including
                                       migrations. E.g.:
                                        main
                                        ├── npl-1.0
                                        │   └── processes
                                        │       └── demo.npl
                                        └── yaml
                                            └── migration.yml

                    Options:
                      --clean          Clear application contents before deployment

                    Configuration is read from .npl/deploy.yml in the current directory
                    or the user's home directory (~/.npl/deploy.yml).
                """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("missing directory parameter") {
                runCommand(
                    commands = listOf("deploy", "test-target"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        """
                    Missing required parameter: directory
                    Usage: deploy <target> <directory> [--clean]

                    Arguments:
                      target           Named target from deploy.yml to deploy to
                      directory        Directory containing NPL sources.
                                       IMPORTANT: The directory must contain a valid NPL source structure, including
                                       migrations. E.g.:
                                        main
                                        ├── npl-1.0
                                        │   └── processes
                                        │       └── demo.npl
                                        └── yaml
                                            └── migration.yml

                    Options:
                      --clean          Clear application contents before deployment

                    Configuration is read from .npl/deploy.yml in the current directory
                    or the user's home directory (~/.npl/deploy.yml).
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

                    Please create or check the configuration file at .npl/deploy.yml
                    (in the current directory or your home directory ~/.npl/deploy.yml)
                    with the following format:
                    targets:
                      nonexistent-target:
                        type: engine
                        engineManagementUrl: <URL of the Noumena Engine API>
                        authUrl: <URL of the authentication endpoint>
                        username: <username for authentication>
                        password: <password for authentication>
                        clientId: <client ID for authentication>
                        clientSecret: <client secret for authentication>
                """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
                }
            }

            test("invalid directory path") {
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
