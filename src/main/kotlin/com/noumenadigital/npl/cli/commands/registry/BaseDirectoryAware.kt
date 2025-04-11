package com.noumenadigital.npl.cli.commands.registry

import java.nio.file.Path

interface BaseDirectoryAware {
    fun setBaseDirectory(path: Path)
}
