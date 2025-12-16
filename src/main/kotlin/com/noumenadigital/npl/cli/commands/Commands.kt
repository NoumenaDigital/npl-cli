package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CheckCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CloudCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.DeployCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.HelpCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.InitCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.McpCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.OpenapiCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.PumlCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.TestCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.VerifyCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.VersionCommandDescriptor

enum class Commands(
    override val commandDescriptor: () -> CommandDescriptor,
) : CommandsRegistry {
    VERSION({ VersionCommandDescriptor }),
    HELP({ HelpCommandDescriptor }),
    INIT({ InitCommandDescriptor }),
    CHECK({ CheckCommandDescriptor }),
    TEST({ TestCommandDescriptor }),
    OPENAPI({ OpenapiCommandDescriptor }),
    PUML({ PumlCommandDescriptor }),
    DEPLOY({ DeployCommandDescriptor }),
    CLOUD({ CloudCommandDescriptor }),
    MCP({ McpCommandDescriptor }),
    VERIFY({ VerifyCommandDescriptor }),
}
