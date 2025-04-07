package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.CommandExecutor
import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_FAILED
import com.noumenadigital.npl.cli.TestUtils.Companion.END_COMMAND_RESULT_SUCCESS
import com.noumenadigital.npl.cli.TestUtils.Companion.START_COMMAND_MESSAGE
import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.commands.registry.NplCommand
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.model.Command
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter

class NplCommandExecutorTest :
    FunSpec({
        lateinit var commandsParser: CommandsParser
        lateinit var commandExecutorOutput: CommandExecutorOutput
        lateinit var writer: OutputStreamWriter
        lateinit var commandSuccess: Command
        lateinit var nplCommandSuccess: NplCommand
        lateinit var nplCommandFailed: NplCommand
        lateinit var nplCommandEnum: NplCliCommandsEnum
        lateinit var executor: CommandExecutor

        beforeTest {
            commandsParser = mockk()
            commandExecutorOutput = mockk()
            writer = mockk(relaxed = true)
            commandSuccess = mockk()
            nplCommandEnum = mockk()
            executor = NplCommandExecutor(commandsParser)
            nplCommandSuccess = mockk()
            nplCommandFailed = mockk()
            every { nplCommandSuccess.execute(any()) } just Runs
            every { nplCommandFailed.execute(any()) } throws Exception("Failed")
        }

        test("second command shouldn't be executed if first failed") {

            val inputCommands = listOf("failed", "success")
            val successCommand = Command(nplCommandEnum)
            val failedCommand = Command(nplCommandEnum)
            every { commandsParser.parse(inputCommands) } returns listOf(failedCommand, successCommand)
            every { commandSuccess.nplCliCommandsEnum } returns nplCommandEnum
            every { nplCommandEnum.nplCommand } returns nplCommandFailed andThen nplCommandSuccess
            every { nplCommandEnum.commandName } returns COMMAND_NAME
            every { commandExecutorOutput.get() } returns writer

            executor.process(inputCommands, commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(inputCommands)
                writer.write(START_COMMAND_MESSAGE.format(COMMAND_NAME))
                writer.write(match<String> { it.contains(END_COMMAND_RESULT_FAILED.format("java.lang.Exception")) })
                writer.write("\n")
                writer.close()
            }
        }

        test("should execute parsed commands and write output") {

            every { commandsParser.parse(any()) } returns listOf(commandSuccess)
            every { commandSuccess.nplCliCommandsEnum } returns nplCommandEnum
            every { nplCommandEnum.nplCommand } returns nplCommandSuccess
            every { commandExecutorOutput.get() } returns writer
            every { nplCommandEnum.commandName } returns COMMAND_NAME

            executor.process(listOf(COMMAND_NAME), commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(listOf(COMMAND_NAME))
                writer.write(START_COMMAND_MESSAGE.format(COMMAND_NAME))
                writer.write(END_COMMAND_RESULT_SUCCESS.format(COMMAND_NAME))
                writer.write("\n")
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
                writer.write("\n")
                writer.close()
            }
        }
    }) {
    companion object {
        private const val COMMAND_NAME = "commandName"
    }
}
