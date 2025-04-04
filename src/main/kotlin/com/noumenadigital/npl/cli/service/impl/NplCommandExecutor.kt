package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.service.ICommandExecutor
import com.noumenadigital.npl.cli.service.ICommandExecutorOutput
import com.noumenadigital.npl.cli.service.ICommandsParser

class NplCommandExecutor : ICommandExecutor {

    private val commandsParser: ICommandsParser = NplCommandsParser()

    override fun process(commands: List<String>, output: ICommandExecutorOutput) {
        output.get().use { out ->
            try {
                val commandsList = commandsParser.parse(commands)
                commandsList.forEach { command ->
                    command.command.nplCommand?.execute(out)
                    out.write("\n")
                }
            } catch (ex: Exception) {
                ex.message?.let { out.write(it) }
                out.write("\n")
            }
        }
    }
}