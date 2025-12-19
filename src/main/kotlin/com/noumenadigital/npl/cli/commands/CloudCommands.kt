package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudClearNplCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudCreateCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudDeleteCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudDeployCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudHelpCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudListCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLoginCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudLogoutCommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.cloud.CloudStatusCommandDescriptor

enum class CloudCommands(
    override val commandDescriptor: () -> CommandDescriptor,
) : CommandsRegistry {
    CLOUD_LOGIN({ CloudLoginCommandDescriptor }),
    CLOUD_LOGOUT({ CloudLogoutCommandDescriptor }),
    CLOUD_HELP({ CloudHelpCommandDescriptor }),
    CLOUD_CLEAR_NPL({ CloudClearNplCommandDescriptor }),
    CLOUD_DEPLOY({ CloudDeployCommandDescriptor }),
    CLOUD_STATUS({ CloudStatusCommandDescriptor }),
    CLOUD_LIST({ CloudListCommandDescriptor }),
    CLOUD_CREATE({ CloudCreateCommandDescriptor }),
    CLOUD_DELETE({ CloudDeleteCommandDescriptor }),
}
