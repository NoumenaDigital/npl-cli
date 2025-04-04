package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.impl.VersionCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class VersionCommandTest : FunSpec({

    test("should write version string to output") {
        // Given
        val writer = StringWriter()
        val command = VersionCommand()

        // When
        command.execute(writer)

        // Then
        writer.toString() shouldBe "I'm v1.0"
    }

    test("should have correct command description") {
        // Given
        val command = VersionCommand()

        // Then
        command.commandDescription shouldBe "Command to return current npl cli version"
    }
})
