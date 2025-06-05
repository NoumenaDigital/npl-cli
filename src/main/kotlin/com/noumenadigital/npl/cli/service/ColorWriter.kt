package com.noumenadigital.npl.cli.service

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.Writer

class ColorWriter(
    private val writer: Writer,
    private val useColor: Boolean = true,
) : AutoCloseable {
    init {
        if (useColor) {
            AnsiConsole.systemInstall()
        }
    }

    fun warning(text: String) {
        writer.write(formatWithColor(text, Ansi::fgYellow))
        writer.write("\n")
        writer.flush()
    }

    fun error(text: String) {
        writer.write(formatWithColor(text, Ansi::fgRed))
        writer.write("\n")
        writer.flush()
    }

    fun success(text: String) {
        writer.write(formatWithColor(text, Ansi::fgGreen))
        writer.write("\n")
        writer.flush()
    }

    fun info(text: String = "") {
        writer.write(text)
        writer.write("\n")
        writer.flush()
    }

    private fun formatWithColor(
        text: String,
        colorizer: (Ansi) -> Ansi,
    ): String =
        if (useColor) {
            colorizer(Ansi.ansi()).a(text).reset().toString()
        } else {
            text
        }

    override fun close() {
        writer.use {
            if (useColor) {
                AnsiConsole.systemUninstall()
            }
        }
    }

    override fun toString(): String = writer.toString()
}
