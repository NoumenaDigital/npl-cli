package com.noumenadigital.npl.cli.intergration

import com.noumenadigital.npl.cli.CommandExecutor
import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_SUCCESS
import com.noumenadigital.npl.cli.TestUtils.Companion.START_COMMAND_MESSAGE
import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.service.NplWriterOutput
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            val commandName = "version"
            nplCommandExecutor.process(listOf(commandName), nplWriter)

            verifySequence {
                nplWriter.get()
                writer.write(START_COMMAND_MESSAGE.format(commandName))
                writer.write(any<String>())
                writer.write(END_COMMAND_RESULT_SUCCESS.format(commandName))
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

        test("All commands executed successfully") {
            nplCommandExecutor.process(NplCliCommandsEnum.entries.map { it.commandName }, nplWriter)

            verify {
                nplWriter.get()
                NplCliCommandsEnum.entries.forEach {
                    writer.write(START_COMMAND_MESSAGE.format(it.commandName))
                    writer.write(any<String>())
                    writer.write(END_COMMAND_RESULT_SUCCESS.format(it.commandName))
                    writer.write("\n")
                }
                writer.close()
            }
        }
    })
