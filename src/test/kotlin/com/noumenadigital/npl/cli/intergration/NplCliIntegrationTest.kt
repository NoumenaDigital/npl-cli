package com.noumenadigital.npl.cli.intergration

import com.noumenadigital.npl.cli.CommandExecutor
import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.service.NplWriterOutput
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter

class NplCliIntegrationTest :
    FunSpec({

        lateinit var writer: OutputStreamWriter
        lateinit var nplWriter: NplWriterOutput
        lateinit var nplCommandExecutor: CommandExecutor

        beforeTest {
            writer = mockk(relaxed = true)
            nplWriter = mockk(relaxed = true)
            every { nplWriter.get() } returns writer
            nplCommandExecutor = NplCommandExecutor()
        }

        test("Command executed successfully") {
            nplCommandExecutor.process(listOf("version"), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write("Executing command: version...\n")
                writer.write(any<String>())
                writer.write("\n")
                writer.close()
            }
        }

        test("Suggested command") {
            nplCommandExecutor.process(listOf("vers"), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write("Command not supported: 'vers'. Did you mean 'version'?")
                writer.write("\n")
                writer.close()
            }
        }

        test("No command found") {
            nplCommandExecutor.process(listOf("foo"), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write("Command not supported: 'foo'.")
                writer.write("\n")
                writer.close()
            }
        }
    })
