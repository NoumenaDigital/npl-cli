package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

class PumlCommandIT :
    FunSpec({
        data class PumlTestContext(
            val testResourcesPath: Path = getTestResourcesPath(),
            val sourceDir: File = File(".").resolve("puml"),
        ) {
            val absolutePath: String get() = testResourcesPath.toAbsolutePath().toString()
        }

        fun withPumlTestContext(
            testDir: List<String>,
            cleanFunction: PumlTestContext.() -> Unit = {
                val pumlDir = File(".").resolve("puml")
                if (pumlDir.exists()) {
                    pumlDir.deleteRecursively()
                }
            },
            test: PumlTestContext.() -> Unit,
        ) {
            val context = PumlTestContext(testResourcesPath = getTestResourcesPath(testDir))

            try {
                context.test()
            } finally {
                context.cleanFunction()
            }
        }

        test("Puml command: Happy path") {
            withPumlTestContext(testDir = listOf("success", "multiple_files")) {
                runCommand(commands = listOf("puml", absolutePath)) {
                    process.waitFor()

                    val pumlDir = workingDirectory.resolve("puml")
                    val expectedOutput =
                        """
                    Puml output directory: ${pumlDir.canonicalFile.path}

                    Completed compilation for 4 files in XXX ms

                    Puml diagram generated successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                    with(workingDirectory.resolve("puml")) {
                        exists() shouldBe true
                        listFiles()?.size shouldBe 3
                    }
                }
            }
        }

        test("Puml command: relative path") {
            withPumlTestContext(testDir = listOf("success", "multiple_files")) {
                val dir = Path.of("src/test/resources/npl-sources/success/multiple_files")
                runCommand(commands = listOf("puml", dir.pathString)) {
                    process.waitFor()

                    val pumlDir = workingDirectory.resolve("puml")
                    val expectedOutput =
                        """
                Puml output directory: ${pumlDir.canonicalFile.path}

                Completed compilation for 4 files in XXX ms

                Puml diagram generated successfully.
                """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                    with(pumlDir) {
                        exists() shouldBe true
                        listFiles()?.size shouldBe 3
                    }
                }
            }
        }

        test("Puml command: invalid path") {
            runCommand(commands = listOf("puml", "non-existing-path")) {
                process.waitFor()
                val expectedOutput = "Source directory does not exist or is not a directory: non-existing-path"

                output.normalize() shouldBe expectedOutput
            }
        }

        test("Puml command: directory pointing to a file") {
            val file = Path.of("src/test/resources/npl-sources/success/multiple_files/test_iou.npl")
            runCommand(commands = listOf("puml", file.pathString)) {
                process.waitFor()
                val expectedOutput = "Source directory does not exist or is not a directory: ${file.pathString}"

                output.normalize() shouldBe expectedOutput
            }
        }
    })
