package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.model.Command

interface ICommandsParser {
    fun parse(command: List<String>): List<Command>
}
