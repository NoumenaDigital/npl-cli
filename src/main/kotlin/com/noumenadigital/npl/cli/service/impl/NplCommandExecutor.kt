package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.ICommandExecutor
import com.noumenadigital.npl.cli.service.ICommandExecutorOutput
import com.noumenadigital.npl.cli.service.ICommandsParser

class NplCommandExecutor(private val commandsParser: ICommandsParser = NplCommandsParser()) : ICommandExecutor {

    override fun process(commands: List<String>, output: ICommandExecutorOutput) {
        output.get().use { out ->
            try {
                val commandsList = commandsParser.parse(commands)
                commandsList.forEach { command ->
                    out.write("Executing command: ${command.nplCliCommandsEnum.commandName}...")
                    command.nplCliCommandsEnum.nplCommand?.execute(out)
                    out.write("\n")
                }
            } catch (ex: Exception) {
                ex.message?.let { out.write(it) }
                out.write("\n")
            }
        }
    }
}