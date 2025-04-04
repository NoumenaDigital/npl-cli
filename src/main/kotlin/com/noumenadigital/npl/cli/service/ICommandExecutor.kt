package com.noumenadigital.npl.cli.service

interface ICommandExecutor {

    fun process(commands: List<String>, output: ICommandExecutorOutput)
}