package com.noumenadigital.npl.cli.config

import com.noumenadigital.npl.cli.exception.DeployConfigException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

class DeployConfigTest :
    FunSpec({

        lateinit var tempDir: File

        beforeTest {
            tempDir = Files.createTempDirectory("npl-cli-config-test").toFile()
        }

        afterTest {
            tempDir.deleteRecursively()
        }

        // Helper to create a config file in a specific location
        fun createTempConfigFile(
            location: File,
            content: String,
        ) {
            location.parentFile.mkdirs()
            location.writeText(content)
        }

        context("DeployConfig.load") {
            test("should load config from current directory if present") {
                val configFile = File(tempDir, ".npl/deploy.yml")
                val yamlContent =
                    """
                    targets:
                      dev:
                        type: engine
                        username: user_curr
                        password: pass_curr
                    """.trimIndent()
                createTempConfigFile(configFile, yamlContent)

                // Pass the tempDir as the current working directory
                val config = DeployConfig.load(currentWorkDir = tempDir)
                config.targets.shouldHaveSize(1)
                val target = config.targets["dev"]
                target.shouldBeInstanceOf<EngineTargetConfig>()
                target.username shouldBe "user_curr"
            }

            test("should load config from user home directory if current dir not present") {
                val mockCurrentDir = File(tempDir, "current_dir") // A dir that won't have the config
                mockCurrentDir.mkdirs()
                val userHomeDir = File(tempDir, "user_home")
                val configFile = File(userHomeDir, ".npl/deploy.yml")
                val yamlContent =
                    """
                    targets:
                      prod:
                        type: engine
                        username: user_home
                        password: pass_home
                    """.trimIndent()
                createTempConfigFile(configFile, yamlContent)

                // Pass the mock current dir and the user home dir
                val config = DeployConfig.load(currentWorkDir = mockCurrentDir, userHomeDir = userHomeDir)
                config.targets.shouldHaveSize(1)
                val target = config.targets["prod"]
                target.shouldBeInstanceOf<EngineTargetConfig>()
                target.username shouldBe "user_home"
            }

            test("should prioritize current directory over user home directory") {
                val currentDir = File(tempDir, "current_prio")
                val currentDirFile = File(currentDir, ".npl/deploy.yml")
                val userHomeDir = File(tempDir, "user_home_prio")
                val userHomeFile = File(userHomeDir, ".npl/deploy.yml")

                val currentYaml =
                    """
                    targets:
                      dev:
                        type: engine
                        username: current
                        password: pwd
                    """.trimIndent()
                createTempConfigFile(currentDirFile, currentYaml)

                val homeYaml =
                    """
                    targets:
                      dev:
                        type: engine
                        username: home
                        password: pwd
                    """.trimIndent()
                createTempConfigFile(userHomeFile, homeYaml)

                // Pass both directories
                val config = DeployConfig.load(currentWorkDir = currentDir, userHomeDir = userHomeDir)
                config.targets.shouldHaveSize(1)
                config.targets["dev"].shouldBeInstanceOf<EngineTargetConfig>().username shouldBe "current"
            }

            test("should return empty config if no file exists in specified dirs") {
                val mockCurrentDir = File(tempDir, "no_config_curr")
                val mockUserHomeDir = File(tempDir, "no_config_home")
                mockCurrentDir.mkdirs()
                mockUserHomeDir.mkdirs()

                val config = DeployConfig.load(currentWorkDir = mockCurrentDir, userHomeDir = mockUserHomeDir)
                config.targets.shouldBeEmpty()
            }

            test("should return empty config if file is malformed") {
                val configFile = File(tempDir, ".npl/deploy.yml")
                createTempConfigFile(configFile, "targets: { not: yaml")

                val config = DeployConfig.load(currentWorkDir = tempDir)
                config.targets.shouldBeEmpty()
            }

            test("should return empty config if file is empty") {
                val configFile = File(tempDir, ".npl/deploy.yml")
                createTempConfigFile(configFile, "")

                val config = DeployConfig.load(currentWorkDir = tempDir)
                config.targets.shouldBeEmpty()
            }
        }

        context("DeployConfig.validateTarget") {
            test("should return no errors for a valid target") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "valid" to EngineTargetConfig(username = "user", password = "pass"),
                            ),
                    )
                val errors = DeployConfig.validateTarget(config, "valid")
                errors shouldBe Unit // No exception should be thrown
            }

            test("should throw DeployConfigException if target not found") {
                val config = DeployConfig(targets = emptyMap())
                val exception =
                    shouldThrow<DeployConfigException> {
                        DeployConfig.validateTarget(config, "missing")
                    }
                exception.message shouldContain "Target 'missing' not found in configuration"
            }

            test("should throw DeployConfigException for missing username") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "test" to EngineTargetConfig(username = "", password = "pass"),
                            ),
                    )
                val exception =
                    shouldThrow<DeployConfigException> {
                        DeployConfig.validateTarget(config, "test")
                    }
                exception.message shouldContain "username is required"
            }

            test("should throw DeployConfigException for missing password") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "test" to EngineTargetConfig(username = "user", password = ""),
                            ),
                    )
                val exception =
                    shouldThrow<DeployConfigException> {
                        DeployConfig.validateTarget(config, "test")
                    }
                exception.message shouldContain "password is required"
            }

            test("should throw DeployConfigException for invalid schema version") {
                val config = DeployConfig(schemaVersion = "v2")
                val exception =
                    shouldThrow<DeployConfigException> {
                        DeployConfig.validateTarget(config, "anyTarget")
                    }
                exception.message shouldContain "Unsupported configuration schema version 'v2'"
            }

            test("should pass for valid config") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "test" to EngineTargetConfig(username = "user", password = "pass"),
                            ),
                    )
                DeployConfig.validateTarget(config, "test")
            }
        }
    })
