package com.noumenadigital.npl.cli.service.impl

import com.noumenadigital.npl.cli.service.ICommandExecutor
import com.noumenadigital.npl.cli.service.ICommandsParser
import java.io.OutputStream
import java.io.Writer

class NplCommandExecutor : ICommandExecutor {

    val commandsParser: ICommandsParser = NplCommandsParser()

    override fun process(commands: List<String>, output: Writer) {
        TODO("Not yet implemented")
    }
}