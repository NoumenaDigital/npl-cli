package com.noumenadigital.npl.cli.intergration

import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_SUCCESS
import com.noumenadigital.npl.cli.TestUtils.Companion.START_COMMAND_MESSAGE
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
        lateinit var nplCommandExecutor: NplCommandExecutor

        beforeTest {
            writer = mockk(relaxed = true)
            nplWriter = mockk(relaxed = true)
            every { nplWriter.get() } returns writer
            nplCommandExecutor = NplCommandExecutor()
        }

        test("Command executed successfully") {
            val commandName = "version"
            nplCommandExecutor.process(listOf(commandName), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write(START_COMMAND_MESSAGE.format(commandName))
                writer.write(any<String>())
                writer.write(END_COMMAND_RESULT_SUCCESS.format(commandName))
                writer.close()
            }
        }

        test("Suggested command") {
            nplCommandExecutor.process(listOf("vers"), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write("Command not supported: 'vers'. Did you mean 'version'?")
                writer.close()
            }
        }

        test("No command found") {
            nplCommandExecutor.process(listOf("foo"), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write("Command not supported: 'foo'.")
                writer.close()
            }
        }
    })
