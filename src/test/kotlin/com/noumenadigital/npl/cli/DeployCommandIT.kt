package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.TestUtils.toYamlSafePath
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.net.ServerSocket
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

        beforeTest {
            setupMockServers()
        }

        afterEach {
            cleanupMockServers()
        }

        fun executeDeployCommand(): Pair<String, Int> {
            var output = ""
            var exitCode = -1

            runCommand(commands = listOf("deploy")) {
                process.waitFor(60, TimeUnit.SECONDS)
                output = this.output
                exitCode = process.exitValue()
            }

            return Pair(output, exitCode)
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
                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: ${mockEngine.url("/")}
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {

                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        """
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                }
            }

            test("simple deploy - relative path") {
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                val engineUrl = mockEngine.url("/").toString()
                val oidcUrl = mockOidc.url("/").toString()

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toFile().relativeOrAbsolute()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: $engineUrl
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        """
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
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

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: ${mockEngine.url("/")}
                      username: user1
                      password: password1
                      authUrl: ${oidcUrl}realms/noumena
                      clear: true

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        """
                        Application contents cleared for $engineUrl
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                }
            }

            test("override parameter from yaml config with command line") {
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                val engineUrl = mockEngine.url("/").toString()
                val oidcUrl = mockOidc.url("/").toString()

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      managementUrl: ${mockEngine.url("/")}
                      username: user2
                      password: password2
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4

                    structure:
                      sourceDir: /non-existent/path
                    """.trimIndent(),
                ) {

                    var output = ""
                    var exitCode = -1

                    runCommand(commands = listOf("deploy", "--source-dir", testDirPath)) {
                        process.waitFor(60, TimeUnit.SECONDS)
                        output = this.output
                        exitCode = process.exitValue()
                    }

                    output.normalize() shouldBe
                        """
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
                }
            }

            test("deploy using default target and . path") {
                mockEngine.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{}"),
                )

                val engineUrl = mockEngine.url("/").toString()
                val oidcUrl = mockOidc.url("/").toString()

                var output = ""
                var exitCode = -1

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      managementUrl: ${mockEngine.url("/")}
                      username: user2
                      password: password2

                    structure:
                      sourceDir: .
                    """.trimIndent(),
                ) {
                    runCommand(
                        commands =
                            listOf(
                                "deploy",
                                "--client-id",
                                "paas",
                                "--client-secret",
                                "secret",
                            ),
                    ) {
                        process.waitFor(60, TimeUnit.SECONDS)
                        output = this.output
                        exitCode = process.exitValue()
                    }

                    output.normalize() shouldBe
                        """
                        Successfully deployed NPL sources and migrations to $engineUrl.
                        """.trimIndent()
                    exitCode shouldBe ExitCode.SUCCESS.code
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

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-failure", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clear: true
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: ${mockEngine.url("/")}
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {

                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        """
                        Application contents cleared for $engineUrl
                        Error deploying NPL sources: '1' source errors encountered:
                        class SourceErrorDetail {
                            code: 0001
                            description: /npl-1.0/objects/iou/iou.npl: (1, 9) E0001: Syntax error: missing {<EOF>, ';'} at 'NPL'
                        }
                        """.trimIndent()

                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
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

                val testDirPath =
                    getTestResourcesPath(listOf("success", "both_sources", "src", "main"))
                        .toAbsolutePath()
                        .toString()
                        .toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clear: true
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: ${mockEngine.url("/")}
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()
                    output.normalize() shouldBe
                        """
                        Application contents cleared for $engineUrl
                        Error deploying NPL sources: Unknown exception: 'Could not locate `migration.yml` in zip:file:/tmp/npl-deployment-${mockEngine.hostName}.zip'
                        """.trimIndent()

                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
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
                    val testDirPath =
                        getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                    TestUtils.withYamlConfig(
                        """
                        local:
                          authUrl: ${oidcUrl}realms/noumena
                          clientId: nm-platform-service-client
                          clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                          managementUrl: $engineUrl
                          username: user1
                          password: password1

                        structure:
                          sourceDir: $testDirPath
                        """.trimIndent(),
                    ) {

                        val (output, exitCode) = executeDeployCommand()

                        val expectedOutput =
                            """
                            Invalid client credentials
                            """.trimIndent()

                        output.normalize() shouldBe expectedOutput
                        exitCode shouldBe ExitCode.CONFIG_ERROR.code
                    }
                } finally {
                    tempDir.deleteRecursively()
                    cleanupMockServers()
                }
            }
        }

        context("connection errors") {
            fun getUnusedPort(): Int {
                ServerSocket(0).use { socket ->
                    return socket.localPort
                }
            }

            fun buildExpectedErrorMessage(
                authUrl: String,
                engineUrl: String,
                prefix: String = "",
            ): String {
                val baseMessage =
                    """
                    Engine or authorization service not found at `$authUrl` or `$engineUrl`. Please check that the service is running, healthy and accessible.
                    """.trimIndent()
                return if (prefix.isBlank()) {
                    baseMessage
                } else {
                    "$prefix: $baseMessage"
                }
            }

            test("engine not running - connection refused") {
                val engineUrl = "http://localhost:${getUnusedPort()}"
                val oidcUrl = mockOidc.url("/").toString()

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: $engineUrl
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        buildExpectedErrorMessage("${oidcUrl}realms/noumena", engineUrl, "Error deploying NPL sources")
                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("auth service not running - connection refused") {
                val engineUrl = mockEngine.url("/").toString()
                val oidcUrl = "http://localhost:${getUnusedPort()}"

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: $engineUrl
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        buildExpectedErrorMessage("${oidcUrl}realms/noumena", engineUrl, "Error deploying NPL sources")
                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("both services not running - connection refused") {
                val engineUrl = "http://localhost:${getUnusedPort()}"
                val oidcUrl = "http://localhost:${getUnusedPort()}"

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: $engineUrl
                      username: user1
                      password: password1

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        buildExpectedErrorMessage("${oidcUrl}realms/noumena", engineUrl, "Error deploying NPL sources")
                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                }
            }

            test("connection error during clear operation") {
                val engineUrl = "http://localhost:${getUnusedPort()}"
                val oidcUrl = mockOidc.url("/").toString()

                val testDirPath =
                    getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString().toYamlSafePath()

                TestUtils.withYamlConfig(
                    """
                    local:
                      authUrl: ${oidcUrl}realms/noumena
                      clientId: nm-platform-service-client
                      clientSecret: 87ff12ca-cf29-4719-bda8-c92faa78e3c4
                      managementUrl: $engineUrl
                      username: user1
                      password: password1
                      clear: true

                    structure:
                      sourceDir: $testDirPath
                    """.trimIndent(),
                ) {
                    val (output, exitCode) = executeDeployCommand()

                    output.normalize() shouldBe
                        buildExpectedErrorMessage("${oidcUrl}realms/noumena", engineUrl, "Failed to clear application contents")
                    exitCode shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        context("command validation errors") {
            test("missing directory parameter") {
                runCommand(
                    commands =
                        listOf(
                            "deploy",
                            "--username",
                            "user2",
                            "--password",
                            "password1",
                            "--management-url",
                            mockEngine.url("/").toString(),
                            "--client-id",
                            "nm-platform-service-client",
                            "--client-secret",
                            "87ff12ca-cf29-4719-bda8c92faa78e3c4",
                            "--auth-url",
                            mockOidc.url("/realms/noumena").toString(),
                        ),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    val expectedOutput =
                        """
                            Missing required parameter(s): source-dir

                            You can provide them in one of the following ways:

                              • As command-line arguments:
                                  --source-dir <value>

                              • In your npl.yml configuration file:

                              /structure/sourceDir

                            Usage:
                              deploy --source-dir <directory> [--clear]

                            Deploys NPL sources to a Noumena Engine instance.

                            Arguments:
                              --source-dir <directory>   Directory containing NPL sources (required).
                                                 IMPORTANT: The directory must contain a valid NPL source structure, including
                                                 migrations. E.g.:
                                                  main
                                                  ├── npl-1.0
                                                  └── migration.yml

                            Options:
                              --clear             Clear application contents before deployment.

                            Configuration is read from npl.yml (current dir)."""

                    output.normalize() shouldBe expectedOutput.normalize().trimIndent()
                    process.exitValue() shouldBe ExitCode.USAGE_ERROR.code
                }
            }

            test("invalid directory path") {
                val nonExistentDir = "/non/existent/directory"

                runCommand(
                    commands =
                        listOf(
                            "deploy",
                            "--source-dir",
                            nonExistentDir,
                            "--username",
                            "user2",
                            "--password",
                            "password1",
                            "--management-url",
                            mockEngine.url("/").toString(),
                            "--client-id",
                            "nm-platform-service-client",
                            "--client-secret",
                            "87ff12ca-cf29-4719-bda8c92faa78e3c4",
                            "--auth-url",
                            mockOidc.url("/realms/noumena").toString(),
                        ),
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
                    commands =
                        listOf(
                            "deploy",
                            "--source-dir",
                            tempFile.absolutePath,
                            "--username",
                            "user2",
                            "--password",
                            "password1",
                            "--management-url",
                            mockEngine.url("/").toString(),
                            "--client-id",
                            "nm-platform-service-client",
                            "--client-secret",
                            "87ff12ca-cf29-4719-bda8c92faa78e3c4",
                            "--auth-url",
                            mockOidc.url("/realms/noumena").toString(),
                        ),
                ) {
                    process.waitFor(5, TimeUnit.SECONDS)

                    output.normalize() shouldBe "Source path is not a directory: ${tempFile.absolutePath}".normalize()
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }
    })
