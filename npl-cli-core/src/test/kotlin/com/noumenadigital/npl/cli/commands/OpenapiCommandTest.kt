package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.OpenapiCommand
import com.noumenadigital.npl.cli.service.ColorWriter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.io.File
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths

class OpenapiCommandTest :
    FunSpec({
        fun getTestResourcesPath(subPath: String = ""): Path {
            val rootDir = File("..").canonicalFile
            return Paths.get(rootDir.toString(), "test-resources", "npl-sources", subPath)
        }

        data class TestContext(
            val writer: ColorWriter = ColorWriter(StringWriter(), false),
            val testResourcesPath: Path = getTestResourcesPath(),
            val openapiCommand: OpenapiCommand =
                OpenapiCommand(
                    targetDir = testResourcesPath.toAbsolutePath().toString(),
                ),
        ) {
            val absolutePath: String get() = testResourcesPath.toAbsolutePath().toString()

            fun validateOpenapiSpec(expectedFileName: String): SwaggerParseResult {
                val openapiDir = testResourcesPath.resolve("openapi")
                val file = openapiDir.resolve(expectedFileName).toFile()
                return OpenAPIV3Parser().readLocation(file.path, null, null)
            }
        }

        fun withTestContext(
            testDir: String,
            clearFunction: TestContext.() -> Unit = {
                val openapiDir = getTestResourcesPath(testDir).resolve("openapi").toFile()
                if (openapiDir.exists()) {
                    openapiDir.deleteRecursively()
                }
            },
            test: TestContext.() -> Unit,
        ) {
            val context =
                TestContext(
                    testResourcesPath = getTestResourcesPath(testDir),
                    openapiCommand =
                        OpenapiCommand(
                            targetDir = getTestResourcesPath(testDir).toAbsolutePath().toString(),
                        ),
                )
            context.apply(test).apply(clearFunction)
        }

        fun normalizeOutput(output: String): String =
            output
                // Normalize line endings
                .replace("\r\n", "\n")
                // Normalize durations
                .replace(Regex("in \\d+ ms"), "in XXX ms")
                .trimIndent()

        context("success") {
            test("multiple packages") {
                withTestContext("success/multiple_packages") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Completed compilation for 2 files in XXX ms

                            Generating openapi for /objects/iou
                            Generating openapi for /objects/car
                            """,
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                    validateOpenapiSpec("objects.car-openapi.yml").messages.size shouldBe 0
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }
            test("no protocols in the package") {
                withTestContext("success/single_file") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Completed compilation for 1 file in XXX ms

                            No NPL protocols found in the target directory.
                            """,
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                    exitCode shouldBe ExitCode.NO_INPUT
                }
            }
            test("multiple files") {
                withTestContext("success/multiple_files") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            Completed compilation for 4 files in XXX ms

                            Generating openapi for /processes
                            Generating openapi for /objects/iou
                            """,
                        )

                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                    validateOpenapiSpec("processes-openapi.yml").messages.size shouldBe 0
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }
            test("test failure") {
                withTestContext("success/test_failure") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            $absolutePath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod'

                            NPL openapi failed with errors.
                            """,
                        )

                    testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }
        }

        context("failure") {
            test("multiple packages") {
                withTestContext("failure/multiple_packages") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            $absolutePath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                            $absolutePath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                            NPL openapi failed with errors.
                            """,
                        )
                    testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    exitCode shouldBe ExitCode.COMPILATION_ERROR
                }
            }
        }

        context("warning") {
            test("complied with warnings") {
                withTestContext("warnings/compilation") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            $absolutePath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                            $absolutePath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                            $absolutePath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL openapi completed with warnings.
                            Generating openapi for /processes
                            Generating openapi for /objects/car
                            Generating openapi for /objects/iou
                            """,
                        )
                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                    validateOpenapiSpec("processes-openapi.yml").messages.size shouldBe 0
                    validateOpenapiSpec("objects.car-openapi.yml").messages.size shouldBe 0
                    exitCode shouldBe ExitCode.SUCCESS
                }
            }

            test("no sources") {
                withTestContext("warnings/no_sources") {
                    val exitCode = openapiCommand.execute(writer)
                    val expectedOutput =
                        normalizeOutput(
                            """
                            No NPL source files found

                            NPL openapi completed with warnings.
                            No NPL protocols found in the target directory.
                            """,
                        )
                    normalizeOutput(writer.toString()) shouldBe expectedOutput
                    testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                    exitCode shouldBe ExitCode.NO_INPUT
                }
            }
        }
    })
