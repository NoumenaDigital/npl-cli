package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.model.Command

interface ICommandsParser {
    fun parse(command: List<String>): List<Command>
}
