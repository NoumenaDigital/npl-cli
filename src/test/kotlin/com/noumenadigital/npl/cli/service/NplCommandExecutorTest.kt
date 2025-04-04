package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.ICommandExecutor
import com.noumenadigital.npl.cli.commands.INplCommand
import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.model.Command
import com.noumenadigital.npl.cli.service.impl.NplCommandExecutor
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.OutputStreamWriter
import java.io.Writer


class TestCommand(override val commandDescription: String) : INplCommand {

    override fun execute(output: Writer) {
        output.write("test command")
    }
}

class NplCommandExecutorTest : FunSpec({

    lateinit var commandsParser: ICommandsParser
    lateinit var commandExecutorOutput: ICommandExecutorOutput
    lateinit var writer: OutputStreamWriter
    lateinit var command: Command
    lateinit var nplCommand: INplCommand
    lateinit var nplCommandEnum: NplCliCommandsEnum
    lateinit var executor: ICommandExecutor

    beforeTest {
        commandsParser = mockk()
        commandExecutorOutput = mockk()
        writer = mockk(relaxed = true)
        command = mockk()
        nplCommandEnum = mockk()
        executor = NplCommandExecutor(commandsParser)
    }

    test("should execute parsed commands and write output") {

        nplCommand = TestCommand("test")
        every { commandsParser.parse(any()) } returns listOf(command)
        every { command.nplCliCommandsEnum } returns nplCommandEnum
        every { nplCommandEnum.nplCommand } returns nplCommand
        every { commandExecutorOutput.get() } returns writer

        executor.process(listOf("version"), commandExecutorOutput)

        verifySequence {
            commandExecutorOutput.get()
            commandsParser.parse(listOf("version"))
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
