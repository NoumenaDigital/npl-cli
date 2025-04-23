package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.nio.file.Path

class OpenapiCommandIT :
    FunSpec({
        val openApiParser = OpenAPIV3Parser()

        data class OpenapiTestContext(
            val testResourcesPath: Path = getTestResourcesPath(),
        ) {
            val absolutePath: String get() = testResourcesPath.toAbsolutePath().toString()

            fun validateOpenapiSpec(expectedFileName: String): SwaggerParseResult {
                val openapiDir = testResourcesPath.resolve("openapi")
                val file = openapiDir.resolve(expectedFileName).toFile()
                return openApiParser.readLocation(file.path, null, null)
            }
        }

        fun withOpenapiTestContext(
            testDir: List<String>,
            clearFunction: OpenapiTestContext.() -> Unit = {
                val openapiDir = getTestResourcesPath(testDir).resolve("openapi").toFile()
                if (openapiDir.exists()) {
                    openapiDir.deleteRecursively()
                }
            },
            test: OpenapiTestContext.() -> Unit,
        ) {
            val context =
                OpenapiTestContext(
                    testResourcesPath = getTestResourcesPath(testDir),
                )
            context.apply(test).apply(clearFunction)
        }

        context("success") {
            test("no protocols found") {
                withOpenapiTestContext(testDir = listOf("success", "single_file")) {
                    runCommand(
                        commands = listOf("openapi", absolutePath),
                    ) {
                        process.waitFor()
                        val expectedOutput =
                            """
                                Completed compilation for 1 file in XXX ms

                                No NPL protocols found in the target directory.
                                """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                        testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                    }
                }
            }

            test("multiple files") {
                withOpenapiTestContext(testDir = listOf("success", "multiple_files")) {
                    runCommand(
                        commands = listOf("openapi", absolutePath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        Completed compilation for 4 files in XXX ms

                        Generating openapi for /processes
                        Generating openapi for /objects/iou
                        NPL openapi completed successfully.
                        """.normalize()

                        output.normalize() shouldBe expectedOutput
                        validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                        validateOpenapiSpec("processes-openapi.yml").messages.size shouldBe 0
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }

            test("multiple packages") {
                withOpenapiTestContext(testDir = listOf("success", "multiple_packages")) {
                    runCommand(
                        commands = listOf("openapi", absolutePath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                            Completed compilation for 2 files in XXX ms

                            Generating openapi for /objects/iou
                            Generating openapi for /objects/car
                            NPL openapi completed successfully.
                            """.normalize()

                        output.normalize() shouldBe expectedOutput
                        validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                        validateOpenapiSpec("objects.car-openapi.yml").messages.size shouldBe 0
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }

            test("test failure") {
                withOpenapiTestContext(testDir = listOf("success", "test_failure")) {
                    runCommand(
                        commands = listOf("openapi", absolutePath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                    $absolutePath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod'

                    NPL openapi failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                        process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                    }
                }
            }

            context("failures") {
                test("multiple packages") {
                    withOpenapiTestContext(testDir = listOf("failure", "multiple_packages")) {
                        runCommand(
                            commands = listOf("openapi", absolutePath),
                        ) {
                            process.waitFor()

                            val expectedOutput =
                                """
                        $absolutePath/src/main/npl/objects/car/car.npl: (10, 69) E0002: Unknown 'Vehicle'
                        $absolutePath/src/main/npl/objects/iou/iou.npl: (7, 12) E0002: Unknown 'Color'
                        $absolutePath/src/main/npl/objects/iou/iou.npl: (18, 47) E0002: Unknown 'calculateValue'

                        NPL openapi failed with errors.
                        """.normalize()

                            output.normalize() shouldBe expectedOutput
                            testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                            process.exitValue() shouldBe ExitCode.COMPILATION_ERROR.code
                        }
                    }
                }
            }

            context("warnings") {
                test("warnings during compilation") {
                    withOpenapiTestContext(testDir = listOf("warnings", "compilation")) {
                        runCommand(
                            commands = listOf("openapi", absolutePath),
                        ) {
                            process.waitFor()

                            val expectedOutput =
                                """
                            $absolutePath/src/main/npl/objects.iou/iou.npl: (18, 5) W0019: Public property `payments` should be explicitly typed.
                            $absolutePath/src/main/npl/processes/demo.npl: (15, 5) W0016: Declared variable `car` unused
                            $absolutePath/src/main/npl/processes/demo.npl: (16, 5) W0016: Declared variable `iou` unused
                            Completed compilation for 4 files with 3 warnings in XXX ms

                            NPL openapi has compilation warnings
                            Generating openapi for /objects/car
                            Generating openapi for /objects/iou
                            Generating openapi for /processes
                            NPL openapi completed successfully.
                            """.normalize()

                            output.normalize() shouldBe expectedOutput
                            validateOpenapiSpec("objects.iou-openapi.yml").messages.size shouldBe 0
                            validateOpenapiSpec("processes-openapi.yml").messages.size shouldBe 0
                            validateOpenapiSpec("objects.car-openapi.yml").messages.size shouldBe 0
                            process.exitValue() shouldBe ExitCode.SUCCESS.code
                        }
                    }
                }

                test("no NPL sources") {
                    withOpenapiTestContext(testDir = listOf("warnings", "no_sources")) {
                        runCommand(
                            commands = listOf("openapi", absolutePath),
                        ) {
                            process.waitFor()

                            val expectedOutput =
                                """
                                 No NPL source files found

                                 NPL openapi has compilation warnings
                                 No NPL protocols found in the target directory.
                                 """.normalize()

                            output.normalize() shouldBe expectedOutput
                            testResourcesPath.resolve("openapi").toFile().exists() shouldBe false
                            process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                        }
                    }
                }
            }
        }
    })
