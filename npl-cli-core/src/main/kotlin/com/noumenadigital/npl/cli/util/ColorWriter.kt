package com.noumenadigital.npl.cli.util

import org.fusesource.jansi.Ansi
import java.io.Writer

class ColorWriter(
    private val writer: Writer,
    private val useColor: Boolean = true,
) : Writer() {
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
        writer.close()
    }

    fun yellow(text: String) {
        write(formatWithColor(text, Ansi::fgYellow))
    }

    fun red(text: String) {
        write(formatWithColor(text, Ansi::fgRed))
    }

    fun green(text: String) {
        write(formatWithColor(text, Ansi::fgGreen))
    }

    fun yellowln(text: String) {
        yellow(text)
        write("\n")
    }

    fun redln(text: String) {
        red(text)
        write("\n")
    }

    fun greenln(text: String) {
        green(text)
        write("\n")
    }

    fun writeln(text: String) {
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
}
