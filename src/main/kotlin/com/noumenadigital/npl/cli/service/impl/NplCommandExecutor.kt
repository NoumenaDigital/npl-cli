package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.service.ICommandExecutor
import com.noumenadigital.npl.cli.service.ICommandsParser
import java.io.Writer

class NplCommandExecutor : ICommandExecutor {

    private val commandsParser: ICommandsParser = NplCommandsParser()

    override fun process(commands: List<String>, output: Writer) {
        output.use { out ->
            try {
                val commandsList = commandsParser.parse(commands)
                commandsList.forEach { command ->
                    command.command.nplCommand?.execute(out)
                    out.write("\n")
                }
            } catch (ex: Exception) {
                ex.message?.let { output.write(it) }
                out.write("\n")
            }
        }
    }
}