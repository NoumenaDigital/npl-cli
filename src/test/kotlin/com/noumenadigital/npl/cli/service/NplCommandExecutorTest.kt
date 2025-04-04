package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.CommandExecutor
import com.noumenadigital.npl.cli.NplCommandExecutor
import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.commands.NplCommand
import com.noumenadigital.npl.cli.model.Command
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter
import java.io.Writer

class NplCommandExecutorTest :
    FunSpec({

        lateinit var commandsParser: CommandsParser
        lateinit var commandExecutorOutput: CommandExecutorOutput
        lateinit var writer: OutputStreamWriter
        lateinit var commandSuccess: Command
        lateinit var nplCommand: NplCommand
        lateinit var nplCommandEnum: NplCliCommandsEnum
        lateinit var executor: CommandExecutor

        beforeTest {
            commandsParser = mockk()
            commandExecutorOutput = mockk()
            writer = mockk(relaxed = true)
            commandSuccess = mockk()
            nplCommandEnum = mockk()
            executor = NplCommandExecutor(commandsParser)
        }

        test("second command shouldn't be executed if first failed") {

            val inputCommands = listOf("failed", "success")
            val successCommand = Command(nplCommandEnum)
            val failedCommand = Command(nplCommandEnum)
            val nplCommandSuccess = SuccessCommand("test")
            val nplCommandFailed = ExceptionCommand("failed")
            every { commandsParser.parse(inputCommands) } returns listOf(failedCommand, successCommand)
            every { commandSuccess.nplCliCommandsEnum } returns nplCommandEnum
            every { nplCommandEnum.nplCommand } returns nplCommandFailed andThen nplCommandSuccess
            every { nplCommandEnum.commandName } returns "commandName"
            every { commandExecutorOutput.get() } returns writer

            executor.process(inputCommands, commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(inputCommands)
                writer.write("Executing command: commandName...\n")
                writer.write("Test exception")
                writer.write("\n")
                writer.close()
            }
        }

        test("should execute parsed commands and write output") {

            nplCommand = SuccessCommand("test")
            every { commandsParser.parse(any()) } returns listOf(commandSuccess)
            every { commandSuccess.nplCliCommandsEnum } returns nplCommandEnum
            every { nplCommandEnum.nplCommand } returns nplCommand
            every { commandExecutorOutput.get() } returns writer
            every { nplCommandEnum.commandName } returns "commandName"

            executor.process(listOf("version"), commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(listOf("version"))
                writer.write("Executing command: commandName...\n")
                writer.write("test command")
                writer.write("\n")
                writer.close()
            }
        }

        test("should write exception message on failure") {
            val exceptionMessage = "Parsing failed"
            every { commandExecutorOutput.get() } returns writer
            every { commandsParser.parse(any()) } throws RuntimeException(exceptionMessage)

            executor.process(listOf("fail-command"), commandExecutorOutput)

            verifySequence {
                commandExecutorOutput.get()
                commandsParser.parse(listOf("fail-command"))
                writer.write(exceptionMessage)
                writer.write("\n")
                writer.close()
            }
        }
    })

class SuccessCommand(
    override val commandDescription: String,
) : NplCommand {
    override fun execute(output: Writer) {
        output.write("test command")
    }
}

class ExceptionCommand(
    override val commandDescription: String,
) : NplCommand {
    override fun execute(output: Writer): Unit = throw Exception("Test exception")
}
