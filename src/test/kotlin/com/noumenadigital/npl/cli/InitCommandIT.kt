package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class InitCommandIT :
    FunSpec({
        data class InitTestContext(
            val testResourcesPath: Path = getTestResourcesPath(),
            val projectName: String = "npl-app",
        ) {
            val projectDir: File = File(".").resolve(projectName)
        }

        fun withInitTestContext(
            testDir: List<String>,
            cleanFunction: InitTestContext.() -> Unit = {
                if (projectDir.exists()) {
                    projectDir.deleteRecursively()
                }
            },
            test: InitTestContext.() -> Unit,
        ) {
            val context =
                InitTestContext(
                    testResourcesPath = getTestResourcesPath(testDir),
                )

            try {
                context.test()
            } finally {
                context.cleanFunction()
            }
        }

        test("Init command: Happy path") {
            var mockRepo = MockWebServer()
            val resourceDir = Paths.get("src/test/resources/test-files").toAbsolutePath().normalize()
            val repoArchive = resourceDir.resolve("samples.zip").toFile()
            val buffer = Buffer().write(repoArchive.readBytes())

            mockRepo.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/zip")
                    .setBody(buffer),
            )

            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--project", "npl-app", "--test-url", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val projectDir = workingDirectory.resolve("npl-app")
                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    Project successfully saved to ${projectDir.canonicalFile.path}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    projectDir.exists() shouldBe true
                    projectDir.walk().filter { it.isFile }.count() shouldBe 11
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
            mockRepo.shutdown()
        }

        test("Init command: Happy path --bare option") {
            var mockRepo = MockWebServer()
            val resourceDir = Paths.get("src/test/resources/test-files").toAbsolutePath().normalize()
            val repoArchive = resourceDir.resolve("no-samples.zip").toFile()
            val buffer = Buffer().write(repoArchive.readBytes())

            mockRepo.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/zip")
                    .setBody(buffer),
            )

            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--project", "npl-app", "--test-url", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val projectDir = workingDirectory.resolve("npl-app")
                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    Project successfully saved to ${projectDir.canonicalFile.path}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    projectDir.exists() shouldBe true
                    projectDir.walk().filter { it.isFile }.count() shouldBe 0
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
            mockRepo.shutdown()
        }

        test("Init command: test-url is not localhost") {
            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--project", "npl-app", "--test-url", "https://github.com/some/api")) {
                    process.destroy()
                    process.waitFor()

                    val expectedOutput =
                        """
                    Only localhost or equivalent allowed for --test-url (was 'github.com')
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if error occurs while downloading repo") {
            var mockRepo = MockWebServer()

            mockRepo.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found"),
            )

            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--project", projectDir.name, "--test-url", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Error occurred while downloading project files: Failed to retrieve project files. Status returned: 404
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if directory with project name already exists") {
            withInitTestContext(listOf("success")) {
                projectDir.canonicalFile.mkdir()
                runCommand(commands = listOf("init", "--project", projectDir.name)) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Directory ${projectDir.canonicalFile.path} already exists.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if process cannot create directory") {
            withInitTestContext(testDir = listOf("success")) {
                projectDir.parentFile.setWritable(false)
                runCommand(commands = listOf("init", "--project", projectDir.name)) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Failed to create directory ${projectDir.canonicalFile.path}.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code

                    projectDir.parentFile.setWritable(true)
                }
            }
        }
    })
