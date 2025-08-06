package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.util.relativeOrAbsolute
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

        test("Init command: Happy path using --path relative path") {
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
                runCommand(commands = listOf("init", "--projectDir", "npl-app", "--templateUrl", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val projectDir = workingDirectory.resolve("npl-app")
                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    Project successfully saved to ${projectDir.relativeOrAbsolute()}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    projectDir.exists() shouldBe true
                    projectDir.walk().filter { it.isDirectory }.count() shouldBe 11
                    projectDir.walk().filter { it.isFile }.count() shouldBe 11
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
            mockRepo.shutdown()
        }

        test("Init command: Happy path using --path arg") {
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
                runCommand(commands = listOf("init", "--projectDir", "npl-app", "--templateUrl", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val projectDir = workingDirectory.resolve("npl-app")
                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    Project successfully saved to ${projectDir.relativeOrAbsolute()}
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    projectDir.exists() shouldBe true
                    projectDir.walk().filter { it.isDirectory }.count() shouldBe 11
                    projectDir.walk().filter { it.isFile }.count() shouldBe 11
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
            mockRepo.shutdown()
        }

        test("Init command: Happy path with no --projectDir arg") {
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

            val commonFiles =
                listOf(
                    ".gitignore",
                    ".npl",
                    ".vscode",
                    "LICENSE.md",
                    "README.md",
                    "SECURITY.md",
                    "NOTICE.md",
                )

            val newFiles =
                listOf(
                    "docker-compose.yml",
                    ".npl/deploy.yml",
                    ".vscode/extensions.json",
                    "src/main/yaml/migration.yml",
                    "src/main/npl-1.0.0/counter/Counter.npl",
                    "src/test/npl/counter/test_counter.npl",
                )

            val backup: (File) -> Unit = { dir ->
                commonFiles.forEach {
                    dir.resolve(it).renameTo(dir.resolve("$it.save"))
                }
            }

            val revert: (File) -> Unit = { dir ->
                commonFiles.forEach {
                    dir.resolve("$it.save").renameTo(dir.resolve(it))
                }
            }

            val customClean: InitTestContext.() -> Unit = {
                revert(projectDir.parentFile)
                newFiles.forEach { projectDir.parentFile.resolve(it).delete() }
            }

            withInitTestContext(testDir = listOf("success"), cleanFunction = customClean) {
                backup(projectDir.parentFile)

                runCommand(commands = listOf("init", "--templateUrl", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    Project successfully saved to ${workingDirectory.normalize().absolutePath}
                    """.normalize()

                    (commonFiles + newFiles).forEach {
                        projectDir.parentFile.resolve(it).exists() shouldBe true
                    }

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }
            }
            mockRepo.shutdown()
        }

        test("Init command: Fail if error occurs while downloading repo") {
            var mockRepo = MockWebServer()

            mockRepo.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found"),
            )

            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--projectDir", projectDir.name, "--templateUrl", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    npl init: Failed to retrieve project template
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if archive file already exists") {
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
                runCommand(commands = listOf("init", "--templateUrl", mockRepo.url("/").toString())) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    Successfully downloaded project files
                    npl init: Failed to extract project files. File .gitignore already exists
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if unexpected arguments are given") {
            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--projectDir", projectDir.name, "--unexpected")) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    npl init: Unknown arguments found: --unexpected
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: Fail if directory with project name already exists") {
            withInitTestContext(listOf("success")) {
                projectDir.canonicalFile.mkdir()
                runCommand(commands = listOf("init", "--projectDir", projectDir.name)) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    npl init: Directory ${projectDir.relativeOrAbsolute()} already exists.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.GENERAL_ERROR.code
                }
            }
        }

        test("Init command: --bare and --templateUrl options are mutually exclusive") {
            withInitTestContext(testDir = listOf("success")) {
                runCommand(commands = listOf("init", "--projectDir", projectDir.name, "--bare", "--templateUrl", "https://example.com")) {
                    process.waitFor()

                    val expectedOutput =
                        """
                    npl init: Cannot use --bare and --templateUrl together.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.USAGE_ERROR.code
                }
            }
        }
    })
