package com.noumenadigital.npl.cli.util

import java.io.File

fun String.normalizeWindowsPath(): String {
    if (File.separatorChar != '\\') {
        return this // Only normalize on Windows
    }

    // Convert /D:/path to D:\path format
    return replace(Regex("/([A-Za-z]):/")) { match ->
        "${match.groupValues[1]}:\\"
    }.replace('/', '\\')
}
