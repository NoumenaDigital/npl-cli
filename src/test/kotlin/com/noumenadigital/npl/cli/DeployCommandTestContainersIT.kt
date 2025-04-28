package com.noumenadigital.npl.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.config.DeployTarget
import com.noumenadigital.npl.cli.config.EngineConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Testcontainers
class DeployCommandTestContainersIT :
    FunSpec({

        val network = Network.newNetwork()

        // Standard timeout for containers
        val startupTimeout = Duration.ofSeconds(30)

        // Path to the keycloak-provisioning directory
        val keycloakProvisioningPath = File("src/test/resources/keycloak-provisioning").absolutePath

        val postgresContainer =
            PostgreSQLContainer("postgres:14.17-alpine")
                .withDatabaseName("platform")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withEnv(
                    mapOf(
                        "POSTGRES_USER" to "postgres",
                        "POSTGRES_PASSWORD" to "postgres",
                        "POSTGRES_DB" to "platform",
                        "ENGINE_DB_USER" to "platform_owner",
                        "ENGINE_DB_PASSWORD" to "platform",
                    ),
                ).withInitScript("init-db-users.sql")
                .withStartupTimeout(startupTimeout)

        val keycloakContainer =
            GenericContainer("quay.io/keycloak/keycloak:23.0")
                .withNetwork(network)
                .withReuse(true)
                .withNetworkAliases("keycloak")
                .withExposedPorts(11000)
                .withEnv(
                    mapOf(
                        "KEYCLOAK_ADMIN" to "admin",
                        "KEYCLOAK_ADMIN_PASSWORD" to "ThePlatonic1",
                        "KC_DB" to "dev-file",
                        "KC_HEALTH_ENABLED" to "true",
                        "KC_HTTP_ENABLED" to "true",
                        "KC_HTTP_PORT" to "11000",
                        "KC_HOSTNAME" to "keycloak",
                    ),
                ).withCommand("start-dev")
                .waitingFor(
                    Wait
                        .forHttp("/health/ready")
                        .forPort(11000)
                        .forStatusCode(200)
                        .withStartupTimeout(startupTimeout),
                ).withStartupTimeout(startupTimeout)

        val provisioningContainer =
            GenericContainer(
                ImageFromDockerfile()
                    .withDockerfileFromBuilder { builder ->
                        builder
                            .from("hashicorp/terraform:latest")
                            .workDir("/terraform")
                            .run("mkdir -p /state")
                            .entryPoint("/terraform/docker-entrypoint.sh")
                            .build()
                    },
            ).dependsOn(keycloakContainer)
                .withNetwork(network)
                .withReuse(true)
                .withCopyFileToContainer(
                    MountableFile.forHostPath(File("$keycloakProvisioningPath/terraform.tf").toPath()),
                    "/terraform/terraform.tf",
                ).withCopyFileToContainer(
                    MountableFile.forHostPath(File("$keycloakProvisioningPath/providers.tf").toPath()),
                    "/terraform/providers.tf",
                ).withCopyFileToContainer(
                    MountableFile.forHostPath(File("$keycloakProvisioningPath/docker-entrypoint.sh").toPath()),
                    "/terraform/docker-entrypoint.sh",
                ).withCreateContainerCmdModifier { cmd ->
                    cmd
                        .withEntrypoint("/bin/sh")
                        .withCmd(
                            "-c",
                            "chmod +x /terraform/docker-entrypoint.sh && terraform init && /terraform/docker-entrypoint.sh",
                        )
                }.withEnv(
                    mapOf(
                        "KEYCLOAK_URL" to "http://keycloak:11000",
                        "KEYCLOAK_USER" to "admin",
                        "KEYCLOAK_PASSWORD" to "ThePlatonic1",
                        "TF_VAR_keycloak_url" to "http://keycloak:11000",
                        "TF_VAR_keycloak_user" to "admin",
                        "TF_VAR_keycloak_password" to "ThePlatonic1",
                    ),
                ).withStartupTimeout(startupTimeout)

        val engineContainer =
            GenericContainer(DockerImageName.parse("ghcr.io/noumenadigital/images/engine:latest"))
                .withNetwork(network)
                .withReuse(true)
                .withNetworkAliases("engine")
                .withExposedPorts(12000, 12400)
                .dependsOn(postgresContainer, keycloakContainer)
                .withEnv(
                    mapOf(
                        "ENGINE_DB_URL" to "jdbc:postgresql://postgres:5432/platform",
                        "ENGINE_DB_USER" to "platform_owner",
                        "ENGINE_DB_PASSWORD" to "platform",
                        "ENGINE_DB_SCHEMA" to "engine-it",
                        "ENGINE_MANAGEMENT_HOST" to "0.0.0.0",
                        "ENGINE_ALLOWED_ISSUERS" to "http://keycloak:11000/realms/noumena",
                        "ENGINE_ISSUER_OVERRIDE" to "http://keycloak:11000/realms/noumena",
                    ),
                ).waitingFor(
                    Wait
                        .forHttp("/actuator/health")
                        .forPort(12000)
                        .forStatusCode(200)
                        .withStartupTimeout(startupTimeout),
                ).withStartupTimeout(startupTimeout)

        beforeSpec {
            logger.info { "Starting PostgreSQL container..." }
            postgresContainer.start()

            logger.info { "Starting Keycloak container..." }
            keycloakContainer.start()

            logger.info { "Starting provisioning container..." }
            provisioningContainer.start()

            while (provisioningContainer.isRunning) {
                Thread.sleep(500)
            }

            val exitCode =
                provisioningContainer.containerInfo.state.exitCodeLong
                    ?.toInt()
            if (exitCode != 0) {
                throw IllegalStateException("Provisioning container failed with exit code $exitCode")
            }

            logger.info { "Starting Engine container..." }
            engineContainer.start()
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

            val engineConfig =
                EngineConfig(
                    targets =
                        mapOf(
                            "test-target" to
                                DeployTarget(
                                    engineManagementUrl = engineManagementUrl,
                                    authUrl = "$keycloakAuthUrl/realms/noumena",
                                    username = "user1",
                                    password = "password1",
                                    clientId = "nm-platform-service-client",
                                    clientSecret = "87ff12ca-cf29-4719-bda8-c92faa78e3c4",
                                ),
                        ),
                )

            mapper.writeValue(configFile, engineConfig)
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

                TestUtils.runCommand(commands = commands) {
                    process.waitFor(60, TimeUnit.SECONDS)
                    output = this.output
                    exitCode = process.exitValue()
                }
            }

            return Pair(output, exitCode)
        }

        test("deploy").config(timeout = 2.minutes) {
            val engineUrl = "http://${engineContainer.host}:${engineContainer.getMappedPort(12400)}"
            val keycloakUrl = "http://${keycloakContainer.host}:${keycloakContainer.getMappedPort(11000)}"

            val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
            try {
                createConfigFile(tempDir, engineUrl, keycloakUrl)

                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()

                val (output, exitCode) = executeDeployCommand(tempDir, testDirPath)

                output.normalize() shouldBe
                    """
                    Creating NPL deployment archive...
                    Deploying NPL sources and migrations to http://localhost:XXXXX...
                    Successfully deployed NPL sources and migrations to target 'test-target'.
                    """.trimIndent()
                exitCode shouldBe ExitCode.SUCCESS.code
            } finally {
                tempDir.deleteRecursively()
            }
        }

        test("deploy with clean").config(timeout = 2.minutes) {
            val engineUrl = "http://${engineContainer.host}:${engineContainer.getMappedPort(12400)}"
            val keycloakUrl = "http://${keycloakContainer.host}:${keycloakContainer.getMappedPort(11000)}"

            val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
            try {
                createConfigFile(tempDir, engineUrl, keycloakUrl)

                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("deploy-success", "main")).toAbsolutePath().toString()
                logger.info { "Deploying NPL sources from: $testDirPath with --clean flag" }

                // Execute the deploy command with clean flag
                val (output, exitCode) = executeDeployCommand(tempDir, testDirPath, withClean = true)

                output.normalize() shouldBe
                    """
                    Clearing application contents...
                    Application contents cleared
                    Creating NPL deployment archive...
                    Deploying NPL sources and migrations to http://localhost:XXXXX...
                    Successfully deployed NPL sources and migrations to target 'test-target'.
                    """.trimIndent()
                exitCode shouldBe ExitCode.SUCCESS.code
            } finally {
                tempDir.deleteRecursively()
            }
        }

        test("should report error when deploying NPL sources with errors").config(timeout = 2.minutes) {
            val engineUrl = "http://${engineContainer.host}:${engineContainer.getMappedPort(12400)}"
            val keycloakUrl = "http://${keycloakContainer.host}:${keycloakContainer.getMappedPort(11000)}"

            val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
            try {
                createConfigFile(tempDir, engineUrl, keycloakUrl)

                val testDirPath =
                    TestUtils.getTestResourcesPath(listOf("deploy-failure", "main")).toAbsolutePath().toString()
                logger.info { "Deploying NPL sources with errors from: $testDirPath" }

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
                    Deploying NPL sources and migrations to http://localhost:XXXXX...
                    Error deploying NPL sources: Failed to deploy NPL sources
                    Cause: ServerException
                    """.trimIndent()

                exitCode shouldBe ExitCode.GENERAL_ERROR.code
            } finally {
                tempDir.deleteRecursively()
            }
        }

        test("should report error when deploying only main directory without migrations").config(timeout = 2.minutes) {
            val engineUrl = "http://${engineContainer.host}:${engineContainer.getMappedPort(12400)}"
            val keycloakUrl = "http://${keycloakContainer.host}:${keycloakContainer.getMappedPort(11000)}"

            val tempDir = Files.createTempDirectory("npl-cli-test").toFile()
            try {
                createConfigFile(tempDir, engineUrl, keycloakUrl)

                val testDirPath =
                    TestUtils
                        .getTestResourcesPath(listOf("success", "both_sources", "src", "main"))
                        .toAbsolutePath()
                        .toString()
                logger.info { "Deploying only main directory without migrations from: $testDirPath" }

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
                    Deploying NPL sources and migrations to http://localhost:XXXXX...
                    Error deploying NPL sources: Failed to deploy NPL sources
                    Cause: ServerException
                    """.trimIndent()

                exitCode shouldBe ExitCode.GENERAL_ERROR.code
            } finally {
                tempDir.deleteRecursively()
            }
        }

        // Stop containers after tests run
        afterSpec {
            engineContainer.stop()
            provisioningContainer.stop()
            keycloakContainer.stop()
            postgresContainer.stop()
        }
    })
