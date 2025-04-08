package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.model.CommandContext

sealed interface NplCommand {
    val commandDescription: String

    fun execute(commandContext: CommandContext)
}
