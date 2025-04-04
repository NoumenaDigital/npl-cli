package com.noumenadigital.npl.cli.service

import java.io.Writer

interface ICommandExecutor {

    fun process(commands: List<String>, output: Writer)
}