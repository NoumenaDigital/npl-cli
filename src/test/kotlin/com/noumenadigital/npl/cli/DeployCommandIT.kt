package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.TimeUnit

class DeployCommandIT :
    FunSpec({

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
