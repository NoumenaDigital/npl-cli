package com.noumenadigital.npl.cli.settings

import com.noumenadigital.npl.cli.commands.NamedParameter

interface SettingsProvider {
    val cloud: CloudSettings
    val local: LocalSettings
    val structure: StructureSettings
    val other: OtherSettings
}

class DefaultSettingsProvider(
    private val args: List<String>,
    private val parameters: List<NamedParameter>,
) : SettingsProvider {
    private val parsed by lazy { SettingsResolver.parseArgsOrThrow(args, parameters) }
    private val yaml by lazy { SettingsResolver.loadYamlConfig() }

    override val cloud: CloudSettings by lazy { SettingsResolver.resolveCloud(parsed, yaml) }
    override val local: LocalSettings by lazy { SettingsResolver.resolveLocal(parsed, yaml) }
    override val structure: StructureSettings by lazy { SettingsResolver.resolveStructure(parsed, yaml) }
    override val other: OtherSettings by lazy { SettingsResolver.resolveOther(parsed) }
}
