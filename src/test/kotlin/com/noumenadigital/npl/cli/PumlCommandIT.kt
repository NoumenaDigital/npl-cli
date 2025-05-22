package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
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

        fun File.listAllFilesNames() = walk().filter { it.isFile }.map { it.name }.toList()

        fun File.validateContents(contents: String) = contents shouldBe readText()

        fun File.findFile(
            fileName: String,
            action: (File) -> Unit,
        ) = walk().filter { it.isFile && it.name == fileName }.firstOrNull()?.let { action(it) }

        test("Puml command: Happy path") {
            withPumlTestContext(testDir = listOf("success", "multiple_files")) {
                runCommand(commands = listOf("puml", absolutePath)) {
                    process.waitFor()

                    val pumlDir = workingDirectory.resolve("puml")
                    val expectedOutput =
                        """
                    Completed compilation for 4 files in XXX ms

                    Writing Puml files to ${pumlDir.canonicalFile.path}

                    Puml diagram generated successfully.
                    """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                    with(workingDirectory.resolve("puml")) {
                        exists() shouldBe true
                        listAllFilesNames() shouldContainAll listOf("settle.puml", "iou.puml", "includes.puml")
                        findFile("settle.puml") { file -> file.validateContents(SETTLE_PUML_CONTENTS) }
                        findFile("iou.puml") { file -> file.validateContents(IOU_PUML_CONTENTS) }
                        findFile("includes.puml") { file -> file.validateContents(INCLUDES_PUML_CONTENTS) }
                    }
                }
            }
        }

        test("Puml command: relative path") {
            withPumlTestContext(testDir = listOf("success", "multiple_files")) {
                val dir = Path.of("src", "test", "resources", "npl-sources", "success", "multiple_files")
                runCommand(commands = listOf("puml", dir.pathString)) {
                    process.waitFor()

                    val pumlDir = workingDirectory.resolve("puml")
                    val expectedOutput =
                        """
                        Completed compilation for 4 files in XXX ms

                        Writing Puml files to ${pumlDir.canonicalFile.path}

                        Puml diagram generated successfully.
                        """.normalize()

                    output.normalize() shouldBe expectedOutput
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                    with(pumlDir) {
                        exists() shouldBe true
                        listAllFilesNames() shouldContainAll listOf("settle.puml", "iou.puml", "includes.puml")
                        findFile("settle.puml") { file -> file.validateContents(SETTLE_PUML_CONTENTS) }
                        findFile("iou.puml") { file -> file.validateContents(IOU_PUML_CONTENTS) }
                        findFile("includes.puml") { file -> file.validateContents(INCLUDES_PUML_CONTENTS) }
                    }
                }
            }
        }

        test("Puml command: overwrite existing files") {
            withPumlTestContext(testDir = listOf("success", "multiple_files")) {
                runCommand(commands = listOf("puml", absolutePath)) {
                    process.waitFor()

                    workingDirectory.resolve("puml")
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
                }

                runCommand(commands = listOf("puml", absolutePath)) {
                    process.waitFor()

                    workingDirectory.resolve("puml")
                    process.exitValue() shouldBe ExitCode.SUCCESS.code
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
            val file = Path.of("src", "test", "resources", "npl-sources", "success", "multiple_files", "test_iou.npl")
            runCommand(commands = listOf("puml", file.pathString)) {
                process.waitFor()
                val expectedOutput = "Source directory does not exist or is not a directory: ${file.pathString}"

                output.normalize() shouldBe expectedOutput
            }
        }
    }) {
    companion object {
        val IOU_PUML_CONTENTS: String =
            """@startuml
                |hide empty members
                |namespace npl.objects.iou {
                |    class TimestampedAmount << (s,green) >> {
                |        {field} +amount: Number
                |        {field} +timestamp: DateTime
                |    }
                |    class Iou << (p,orchid) >> {
                |        {field} +issuer: Party
                |        {field} +payee: Party
                |        {field} +payments: List<TimestampedAmount>
                |        {field} +forAmount: Number
                |        {field} +observers: Map<Text, Party>
                |        {method} +[issuer] pay(amount: Number)
                |        {method} +[payee] forgive()
                |        {method} +[issuer | payee] getAmountOwed(): Number
                |    }
                |    npl.objects.iou.Iou --> "*" npl.objects.iou.TimestampedAmount : payments
                |}
                |@enduml
            """.trimMargin()

        val INCLUDES_PUML_CONTENTS =
            """
            @startuml
            hide empty members
            !include objects/iou/iou.puml
            !include processes/settle.puml
            @enduml
            """.trimIndent()

        val SETTLE_PUML_CONTENTS =
            """
            @startuml
            hide empty members
            namespace npl.processes {
                class Settle << (p,orchid) >> {
                    {field} +iouOwner: Party
                    {field} +carOwner: Party
                    {field} +iou: Iou
                    {field} +observers: Map<Text, Party>
                    {method} +[iouOwner | carOwner] swap()
                }
                npl.processes.Settle --> "1" npl.objects.iou.Iou : iou
            }
            @enduml
            """.trimIndent()
    }
}
