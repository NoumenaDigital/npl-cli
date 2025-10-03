package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import com.noumenadigital.npl.cli.commands.registry.TestCommand.Companion.COVERAGE_FILE
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.io.path.pathString

class TestCommandIT :
    FunSpec(
        {
            val coverageFile = File(".").resolve(COVERAGE_FILE)

            beforeEach {
                if (coverageFile.exists()) {
                    coverageFile.delete()
                }
            }

            afterEach {
                if (coverageFile.exists()) {
                    coverageFile.delete()
                }
            }

            context("success") {
                test("both main and test sources") {
                    coverageFile.exists() shouldBe false

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms

                        NPL test completed successfully in XXX ms.
                        """.normalize()

                        coverageFile.exists() shouldBe false
                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }

                test("both main and test sources - with coverage using relative path") {
                    coverageFile.exists() shouldBe false

                    val testPath = getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath()
                    val relativePath = File(".").canonicalFile.toPath().relativize(testPath)

                    runCommand(
                        commands = listOf("test", "--test-source-dir", relativePath.pathString, "--coverage"),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms
                        Line coverage summary
                        --------------------------------------
                        src/main/npl/objects/iou/iou.npl   XXX%
                        src/test/npl/objects/test_iou.npl XXX%
                        --------------------------------------
                        Overall                            XXX%


                        NPL test completed successfully in XXX ms.
                        """.normalize()

                        coverageFile.exists() shouldBe true
                        coverageFile.readText().normalize() shouldBe
                            """
                            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                            <coverage version="1">
                                <file path="$testPath/src/main/npl/objects/iou/iou.npl">
                                    <lineToCover covered="true" lineNumber="8"/>
                                    <lineToCover covered="true" lineNumber="12"/>
                                    <lineToCover covered="true" lineNumber="18"/>
                                    <lineToCover covered="true" lineNumber="20"/>
                                    <lineToCover covered="true" lineNumber="24"/>
                                    <lineToCover covered="true" lineNumber="25"/>
                                    <lineToCover covered="true" lineNumber="27"/>
                                    <lineToCover covered="true" lineNumber="29"/>
                                    <lineToCover covered="true" lineNumber="31"/>
                                    <lineToCover covered="false" lineNumber="38"/>
                                    <lineToCover covered="true" lineNumber="43"/>
                                </file>
                                <file path="$testPath/src/test/npl/objects/test_iou.npl">
                                    <lineToCover covered="true" lineNumber="10"/>
                                    <lineToCover covered="true" lineNumber="12"/>
                                    <lineToCover covered="true" lineNumber="17"/>
                                    <lineToCover covered="true" lineNumber="18"/>
                                    <lineToCover covered="true" lineNumber="20"/>
                                </file>
                            </coverage>

                            """.trimIndent().normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }

                test("both main and test sources - with coverage using absolute path") {
                    coverageFile.exists() shouldBe false

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath, "--coverage"),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms
                        Line coverage summary
                        --------------------------------------
                        src/main/npl/objects/iou/iou.npl   XXX%
                        src/test/npl/objects/test_iou.npl XXX%
                        --------------------------------------
                        Overall                            XXX%


                        NPL test completed successfully in XXX ms.
                        """.normalize()

                        coverageFile.exists() shouldBe true
                        coverageFile.readText().normalize() shouldBe
                            """
                            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                            <coverage version="1">
                                <file path="$testDirPath/src/main/npl/objects/iou/iou.npl">
                                    <lineToCover covered="true" lineNumber="8"/>
                                    <lineToCover covered="true" lineNumber="12"/>
                                    <lineToCover covered="true" lineNumber="18"/>
                                    <lineToCover covered="true" lineNumber="20"/>
                                    <lineToCover covered="true" lineNumber="24"/>
                                    <lineToCover covered="true" lineNumber="25"/>
                                    <lineToCover covered="true" lineNumber="27"/>
                                    <lineToCover covered="true" lineNumber="29"/>
                                    <lineToCover covered="true" lineNumber="31"/>
                                    <lineToCover covered="false" lineNumber="38"/>
                                    <lineToCover covered="true" lineNumber="43"/>
                                </file>
                                <file path="$testDirPath/src/test/npl/objects/test_iou.npl">
                                    <lineToCover covered="true" lineNumber="10"/>
                                    <lineToCover covered="true" lineNumber="12"/>
                                    <lineToCover covered="true" lineNumber="17"/>
                                    <lineToCover covered="true" lineNumber="18"/>
                                    <lineToCover covered="true" lineNumber="20"/>
                                </file>
                            </coverage>

                            """.trimIndent().normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }

                test("test compilation failure") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "test_failure")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        'compilation' ............... FAIL           0    tests in XXX ms ($testDirPath/src/test/npl/objects/test_iou_error.npl: (12, 5) E0003: Unknown member 'undefinedMethod')

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
            }

            context("failure") {
                test("one test failed") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "test_assertion_failed")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou_failed.npl' . FAIL(1)        2    tests in XXX ms
                        'Amount owed should reflect payment ==> Expect '112300', got '50'.' .......................................................................... FAIL

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("all test failed") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "test_failed")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou_failed.npl' ... FAIL(2)        2    tests in XXX ms
                        'Amount owed should equal initial value ==> Expect '999', got '100'.' ................................................................ FAIL
                        'Amount owed should reflect payment ==> Expect '777', got '50'.' ..................................................................... FAIL
                        '$testDirPath/src/test/npl/objects/test_iou_failed_2.npl' . FAIL(2)        2    tests in XXX ms
                        'Amount owed should equal initial value ==> Expect '999', got '100'.' ................................................................ FAIL
                        'Amount owed should reflect payment ==> Expect '777', got '50'.' ..................................................................... FAIL

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("no test sources found") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "empty_tests_folder")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        No NPL tests found.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
                test("sources compilation failure") {
                    val testDirPath =
                        getTestResourcesPath(listOf("failure", "single_file")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        'compilation' ............... FAIL           0    tests in XXX ms ($testDirPath/src/main/npl/objects/car/car.npl: (7, 1) E0001: Syntax error: rule statement failed predicate: {quirksMode}?)

                        NPL test failed with errors.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.DATA_ERROR.code
                    }
                }
            }

            context("warnings") {
                test("source code compilation warning") {
                    val testDirPath =
                        getTestResourcesPath(listOf("warnings", "compilation")).toAbsolutePath().toString()
                    runCommand(
                        commands = listOf("test", "--test-source-dir", testDirPath),
                    ) {
                        process.waitFor()

                        val expectedOutput =
                            """
                        '$testDirPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms

                        NPL test completed successfully in XXX ms.
                    """.normalize()

                        output.normalize() shouldBe expectedOutput
                        process.exitValue() shouldBe ExitCode.SUCCESS.code
                    }
                }
            }

            context("Yaml config") {
                fun happyPath(
                    @Language("yaml") yamlConfig: String,
                    params: List<String>,
                    testWithCoverage: Boolean = false,
                ) {
                    coverageFile.exists() shouldBe false

                    val testPath = getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath()

                    TestUtils.withYamlConfig(yamlConfig) {
                        runCommand(
                            commands = listOf("test", *params.toTypedArray()),
                        ) {
                            process.waitFor()

                            val expectedOutput =
                                """
                        '$testPath/src/test/npl/objects/test_iou.npl' .. PASS           2    tests in XXX ms
                        Line coverage summary
                        --------------------------------------
                        src/main/npl/objects/iou/iou.npl   XXX%
                        src/test/npl/objects/test_iou.npl XXX%
                        --------------------------------------
                        Overall                            XXX%


                        NPL test completed successfully in XXX ms.
                        """.normalize()

                            coverageFile.exists() shouldBe testWithCoverage
                            if (testWithCoverage) {
                                coverageFile.readText().normalize() shouldBe
                                    """
                                    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                                    <coverage version="1">
                                        <file path="$testPath/src/main/npl/objects/iou/iou.npl">
                                            <lineToCover covered="true" lineNumber="8"/>
                                            <lineToCover covered="true" lineNumber="12"/>
                                            <lineToCover covered="true" lineNumber="18"/>
                                            <lineToCover covered="true" lineNumber="20"/>
                                            <lineToCover covered="true" lineNumber="24"/>
                                            <lineToCover covered="true" lineNumber="25"/>
                                            <lineToCover covered="true" lineNumber="27"/>
                                            <lineToCover covered="true" lineNumber="29"/>
                                            <lineToCover covered="true" lineNumber="31"/>
                                            <lineToCover covered="false" lineNumber="38"/>
                                            <lineToCover covered="true" lineNumber="43"/>
                                        </file>
                                        <file path="$testPath/src/test/npl/objects/test_iou.npl">
                                            <lineToCover covered="true" lineNumber="10"/>
                                            <lineToCover covered="true" lineNumber="12"/>
                                            <lineToCover covered="true" lineNumber="17"/>
                                            <lineToCover covered="true" lineNumber="18"/>
                                            <lineToCover covered="true" lineNumber="20"/>
                                        </file>
                                    </coverage>

                                    """.trimIndent().normalize()

                                output.normalize() shouldBe expectedOutput
                            }
                            process.exitValue() shouldBe ExitCode.SUCCESS.code
                        }
                    }
                }

                test("Yaml config happy path - coverage") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    happyPath(
                        params = emptyList(),
                        testWithCoverage = true,
                        yamlConfig =
                            """
                            structure:
                              testSourceDir: "$testDirPath"
                              coverage: true
                            """.trimIndent(),
                    )
                }

                test("Yaml config happy path - no coverage - implicit") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    happyPath(
                        params = emptyList(),
                        testWithCoverage = false,
                        yamlConfig =
                            """
                            structure:
                              testSourceDir: "$testDirPath"
                            """.trimIndent(),
                    )
                }

                test("Yaml config happy path - no coverage - explicit") {
                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    happyPath(
                        params = emptyList(),
                        testWithCoverage = false,
                        yamlConfig =
                            """
                            structure:
                              testSourceDir: "$testDirPath"
                              coverage: false
                            """.trimIndent(),
                    )
                }
            }
        },
    )
