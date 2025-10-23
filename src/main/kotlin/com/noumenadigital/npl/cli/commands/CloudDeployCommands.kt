package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployFrontendCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployHelpCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.deploy.CloudDeployNplCommandDescriptor

enum class CloudDeployCommands(
    override val commandDescriptor: () -> CommandDescriptor,
) : CommandsRegistry {
    CLOUD_DEPLOY_HELP({ CloudDeployHelpCommandDescriptor }),
    CLOUD_DEPLOY_NPL({ CloudDeployNplCommandDescriptor }),
    CLOUD_DEPLOY_FRONTEND({ CloudDeployFrontendCommandDescriptor }),
}
