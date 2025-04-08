package com.noumenadigital.npl.cli.intergration

import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_SUCCESS
import com.noumenadigital.npl.cli.TestUtils.Companion.START_COMMAND_MESSAGE
import com.noumenadigital.npl.cli.commands.NplCliCommands
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter

class NplCliIntegrationTest :
    FunSpec({

        lateinit var writer: OutputStreamWriter
        lateinit var nplCommandExecutor: NplCommandExecutor

        beforeTest {
            writer = mockk(relaxed = true)
            nplCommandExecutor = NplCommandExecutor()
        }

        test("Command executed successfully") {
            val commandName = "version"
            nplCommandExecutor.process(listOf(commandName), writer)

            verifySequence {
                writer.write(START_COMMAND_MESSAGE.format(commandName))
                writer.write(any<String>())
                writer.write(END_COMMAND_RESULT_SUCCESS.format(commandName))
                writer.close()
            }
        }

        test("Suggested command") {
            nplCommandExecutor.process(listOf("vers"), writer)

            verifySequence {
                writer.write("Command not supported: 'vers'. Did you mean 'version'?")
                writer.close()
            }
        }

        test("No command found") {
            nplCommandExecutor.process(listOf("foo"), writer)

            verifySequence {
                writer.write("Command not supported: 'foo'.")
                writer.close()
            }
        }

        test("Only command can be processed") {
            val commandNames = NplCliCommands.entries.map { it.commandName }
            nplCommandExecutor.process(commandNames, writer)

            verifySequence {
                writer.write("Invalid command line input. Only 1 command can be processed, but was $commandNames\n")
                writer.close()
            }
        }
    })
