package com.noumenadigital.npl.cli.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
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
                errors.shouldBeEmpty()
            }

            test("should return error if target not found") {
                val config = DeployConfig(targets = emptyMap())
                val errors = DeployConfig.validateTarget(config, "missing")
                errors.shouldContainExactly("Target 'missing' not found in configuration")
            }

            test("should return error if username is blank") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "no_user" to EngineTargetConfig(username = "", password = "pass"),
                            ),
                    )
                val errors = DeployConfig.validateTarget(config, "no_user")
                errors.shouldContainExactly("username is required for engine target")
            }

            test("should return error if password is blank") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "no_pass" to EngineTargetConfig(username = "user", password = " "), // Test with whitespace
                            ),
                    )
                val errors = DeployConfig.validateTarget(config, "no_pass")
                errors.shouldContainExactly("password is required for engine target")
            }

            test("should return multiple errors if both username and password are blank") {
                val config =
                    DeployConfig(
                        targets =
                            mapOf(
                                "both_blank" to EngineTargetConfig(username = "", password = ""),
                            ),
                    )
                val errors = DeployConfig.validateTarget(config, "both_blank")
                errors.shouldContainExactly(
                    "username is required for engine target",
                    "password is required for engine target",
                )
            }
        }
    })
