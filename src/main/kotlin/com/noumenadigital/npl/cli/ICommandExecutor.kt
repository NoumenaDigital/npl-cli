package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.ICommandExecutorOutput

interface ICommandExecutor {

    fun process(commands: List<String>, output: ICommandExecutorOutput)
}