package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.model.CommandContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class VersionCommandTest :
    FunSpec({

        test("should write version string to output") {
            val writer = StringWriter()
            val command = VersionCommand

            command.execute(CommandContext(writer))

            writer.toString() shouldBe "I'm v1.0"
        }

        test("should have correct command description") {
            val command = VersionCommand

            command.commandDescription shouldBe "Command to return current npl cli version"
        }
    })
