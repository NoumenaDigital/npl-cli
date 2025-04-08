package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_SUCCESS
import com.noumenadigital.npl.cli.TestUtils.Companion.START_COMMAND_MESSAGE
import com.noumenadigital.npl.cli.commands.registry.NplCommand
import com.noumenadigital.npl.cli.exception.CommandParsingException
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter

class NplCommandExecutorTest :
    FunSpec({
        lateinit var commandsParser: NplCommandsParser
        lateinit var commandExecutorOutput: CommandExecutorOutput
        lateinit var writer: OutputStreamWriter
        lateinit var commandSuccess: NplCommand
        lateinit var nplCommandSuccess: NplCommand
        lateinit var executor: NplCommandExecutor

        beforeTest {
            commandSuccess = mockk()

            commandsParser = mockk()
            commandExecutorOutput = mockk()
            writer = mockk(relaxed = true)
            executor = NplCommandExecutor(commandsParser)
            nplCommandSuccess = mockk()
            every { nplCommandSuccess.execute(any()) } just Runs
        }

        test("should execute parsed commands and write output") {

            every { commandsParser.parse(any()) } returns commandSuccess
            every { commandSuccess.commandName } returns COMMAND_NAME
            every { commandSuccess.execute(any()) } just Runs
            every { commandExecutorOutput.get() } returns writer

            executor.process(listOf(COMMAND_NAME), commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(listOf(COMMAND_NAME))
                writer.write(START_COMMAND_MESSAGE.format(COMMAND_NAME))
                writer.write(END_COMMAND_RESULT_SUCCESS.format(COMMAND_NAME))
                writer.close()
            }
        }

        test("should write exception message on failure") {
            val exceptionMessage = "Parsing failed"
            every { commandExecutorOutput.get() } returns writer
            every { commandsParser.parse(any()) } throws CommandParsingException(exceptionMessage)

            executor.process(listOf("fail-command"), commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(listOf("fail-command"))
                writer.write(exceptionMessage)
                writer.close()
            }
        }
    }) {
    companion object {
        private const val COMMAND_NAME = "commandName"
    }
}
