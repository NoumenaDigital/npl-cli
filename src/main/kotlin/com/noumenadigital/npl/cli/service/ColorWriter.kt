package com.noumenadigital.npl.cli.service

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.Writer

class ColorWriter(
    private val writer: Writer,
    private val useColor: Boolean = true,
) : Writer() {
    init {
        if (useColor) {
            AnsiConsole.systemInstall()
        }
    }

    override fun write(
        cbuf: CharArray,
        off: Int,
        len: Int,
    ) {
        writer.write(cbuf, off, len)
    }

    override fun flush() {
        writer.flush()
    }

    override fun close() {
        writer.use {
            if (useColor) {
                AnsiConsole.systemUninstall()
            }
        }
    }

    fun warning(text: String) {
        write(formatWithColor(text, Ansi::fgYellow))
        write("\n")
    }

    fun error(text: String) {
        write(formatWithColor(text, Ansi::fgRed))
        write("\n")
    }

    fun success(text: String) {
        write(formatWithColor(text, Ansi::fgGreen))
        write("\n")
    }

    fun info(text: String = "") {
        write(text)
        write("\n")
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

    override fun toString(): String = writer.toString()
}
