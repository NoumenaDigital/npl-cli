package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.commands.registry.DeployCommand
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

        lateinit var mockOidc: MockWebServer
        lateinit var mockEngine: MockWebServer

        fun setupMockServers() {
            mockOidc = MockWebServer()
            mockEngine = MockWebServer()

            mockOidc.start()
            mockEngine.start()

            mockOidc.dispatcher =
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
                                            "token_endpoint": "${mockOidc.url("/realms/noumena/protocol/openid-connect/token")}"
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
            mockOidc.shutdown()
            mockEngine.shutdown()
        }

        fun createConfigFile(
            tempDir: File,
            engineManagementUrl: String,
            oidcAuthUrl: String,
            schemaVersion: String = "v1",
        ): File {
            val configDir = File(tempDir, ".npl")
            configDir.mkdirs()

            val configFile = File(configDir, "deploy.yml")

            val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

            val deployConfig =
                DeployConfig(
                    schemaVersion = schemaVersion,
                    targets =
                        mapOf(
                            "test-target" to
                                EngineTargetConfig(
                                    engineManagementUrl = engineManagementUrl,
                                    authUrl = "${oidcAuthUrl}realms/noumena",
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
                File(".npl").deleteRecursively()
                System.setProperty("user.home", originalUserHome)
            }
        }

        fun executeDeployCommand(
            tempDir: File,
            testDirPath: String,
            withClear: Boolean = false,
        ): Pair<String, Int> {
            var output = ""
            var exitCode = -1

            withConfigDir(tempDir) {
                val commands =
                    if (withClear) {
                        listOf("deploy", "--target=test-target", "--sourceDir=$testDirPath", "--clear")
                    } else {
                        listOf("deploy", "--target=test-target", "--sourceDir=$testDirPath")
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
                val oidcUrl = mockOidc.url("/").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, oidcUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    val (output, exitCode) = executeDeployCommand(tempDir, testDirPath)

                    output.normalize(withPadding = false) shouldBe
                        """
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }

            test("deploy with clear flag") {
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
                val oidcUrl = mockOidc.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, oidcUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    val (output, exitCode) = executeDeployCommand(tempDir, testDirPath, withClear = true)

                    output.normalize(withPadding = false) shouldBe
                        """
                        Clearing application contents for $engineUrl...
                        Application contents cleared for $engineUrl
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Successfully deployed NPL sources and migrations to $engineUrl.
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
                val oidcUrl = mockOidc.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, oidcUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-failure", "main")).toAbsolutePath().toString()

                    val (output, exitCode) =
                        executeDeployCommand(
                            tempDir = tempDir,
                            testDirPath = testDirPath,
                            withClear = true,
                        )

                    output.normalize(withPadding = false) shouldBe
                        """
                        Clearing application contents for $engineUrl...
                        Application contents cleared for $engineUrl
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
                val oidcUrl = mockOidc.url("").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    createConfigFile(tempDir, engineUrl, oidcUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources", "src", "main"))
                            .toAbsolutePath()
                            .toString()

                    val (output, exitCode) =
                        executeDeployCommand(
                            tempDir = tempDir,
                            testDirPath = testDirPath,
                            withClear = true,
                        )

                    output.normalize(withPadding = false) shouldBe
                        """
                        Clearing application contents for $engineUrl...
                        Application contents cleared for $engineUrl
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

        context("authentication errors") {
            test("invalid client credentials") {
                mockOidc.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when (request.path) {
                                "/realms/noumena/.well-known/openid-configuration" -> {
                                    MockResponse()
                                        .setResponseCode(200)
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "token_endpoint": "${mockOidc.url("/realms/noumena/protocol/openid-connect/token")}"
                                            }
                                            """.trimIndent(),
                                        )
                                }
                                "/realms/noumena/protocol/openid-connect/token" -> {
                                    MockResponse()
                                        .setResponseCode(401) // Unauthorized
                                        .setHeader("Content-Type", "application/json")
                                        .setBody(
                                            """
                                            {
                                                "error": "invalid_client",
                                                "error_description": "Invalid client credentials"
                                            }
                                            """.trimIndent(),
                                        )
                                }
                                else -> MockResponse().setResponseCode(404)
                            }
                    }

                val engineUrl = mockEngine.url("/").toString() // Engine URL doesn't matter much here
                val oidcUrl = mockOidc.url("/").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test-auth-fail").toFile()
                try {
                    // Config file uses standard credentials, but the mock server forces failure
                    createConfigFile(tempDir, engineUrl, oidcUrl)

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    val (output, exitCode) = executeDeployCommand(tempDir, testDirPath)

                    val expectedOutput =
                        """
                        Creating NPL deployment archive...
                        Deploying NPL sources and migrations to $engineUrl...
                        Authorization exception: Invalid client credentials
                        """.trimIndent()

                    output.normalize(withPadding = false) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.CONFIG_ERROR.code
                } finally {
                    tempDir.deleteRecursively()
                    setupMockServers() // Re-setup default dispatchers
                }
            }
        }

        context("configuration errors") {
            test("unsupported schema version") {
                val engineUrl = mockEngine.url("/").toString()
                val oidcUrl = mockOidc.url("/").toString()

                val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
                try {
                    // Create config with unsupported version
                    createConfigFile(tempDir, engineUrl, oidcUrl, schemaVersion = "2")

                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                    var output = ""
                    var exitCode = -1

                    withConfigDir(tempDir) {
                        runCommand(commands = listOf("deploy", "--target=test-target", "--sourceDir=$testDirPath")) {
                            process.waitFor(60, TimeUnit.SECONDS)
                            output = this.output
                            exitCode = process.exitValue()
                        }
                    }

                    val expectedOutput =
                        """
                        Configuration error: Unsupported configuration schema version '2'. Supported version is 'v1'.
                        """.normalize()

                    output.normalize() shouldBe expectedOutput
                    exitCode shouldBe ExitCode.CONFIG_ERROR.code
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        context("command validation errors") {
            test("missing target parameter") {
                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                runCommand(
                    commands = listOf("deploy", "--sourceDir=$testDirPath"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        "Missing required parameter: --target=<name> (or use --dev for local defaults)\n" +
                            DeployCommand.USAGE_STRING

                    output.normalize() shouldBe expectedOutput.normalize()
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("missing directory parameter") {
                runCommand(
                    commands = listOf("deploy", "--target=test-target"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput = "Missing required parameter: --sourceDir=<directory>\n${DeployCommand.USAGE_STRING}"

                    output.normalize() shouldBe expectedOutput.normalize()
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("target not found in configuration") {
                val testDirPath = getTestResourcesPath(listOf("success", "multiple_files")).toAbsolutePath().toString()

                runCommand(
                    commands = listOf("deploy", "--target=nonexistent-target", "--sourceDir=$testDirPath"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput = "Configuration error: Target 'nonexistent-target' not found in configuration"

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
                }
            }

            test("invalid directory path") {
                val nonExistentDir = "/non/existent/directory"

                runCommand(
                    commands = listOf("deploy", "--target=test-target", "--sourceDir=$nonExistentDir"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    output.normalize() shouldBe "Source directory does not exist: /non/existent/directory".normalize()
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("file provided instead of directory") {
                val tempFile = File.createTempFile("test", ".txt")
                tempFile.deleteOnExit()

                runCommand(
                    commands = listOf("deploy", "--target=test-target", "--sourceDir=${tempFile.absolutePath}"),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    output.normalize() shouldBe "Source path is not a directory: ${tempFile.absolutePath}".normalize()
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }
    })
