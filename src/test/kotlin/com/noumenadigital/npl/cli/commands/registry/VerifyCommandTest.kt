package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.File
import java.io.StringWriter

class VerifyCommandTest : StringSpec({
    "verify command should fail when audit file does not exist" {
        val output = ColorWriter(StringWriter(), false)
        val command = VerifyCommand(
            audit = "/nonexistent/audit.json",
            sources = ".",
            didScheme = "https",
            didHostOverride = null,
            failFast = false,
            jsonOutput = false,
            enableReplay = false
        )

        val exitCode = command.execute(output)

        exitCode shouldBe ExitCode.GENERAL_ERROR
        output.toString() shouldContain "does not exist"
    }

    "verify command should fail when sources path does not exist" {
        val auditFile = File.createTempFile("audit", ".json")
        auditFile.writeText("{\"audit_log\": [], \"state\": {}}")
        auditFile.deleteOnExit()

        val output = ColorWriter(StringWriter(), false)
        val command = VerifyCommand(
            audit = auditFile.absolutePath,
            sources = "/nonexistent/sources",
            didScheme = "https",
            didHostOverride = null,
            failFast = false,
            jsonOutput = false,
            enableReplay = false
        )

        val exitCode = command.execute(output)

        exitCode shouldBe ExitCode.GENERAL_ERROR
        output.toString() shouldContain "does not exist"
    }

    "verify command should reject invalid did-scheme" {
        val auditFile = File.createTempFile("audit", ".json")
        auditFile.writeText("{\"audit_log\": [], \"state\": {}}")
        auditFile.deleteOnExit()

        val output = ColorWriter(StringWriter(), false)
        val command = VerifyCommand(
            audit = auditFile.absolutePath,
            sources = ".",
            didScheme = "ftp",
            didHostOverride = null,
            failFast = false,
            jsonOutput = false,
            enableReplay = false
        )

        val exitCode = command.execute(output)

        exitCode shouldBe ExitCode.GENERAL_ERROR
        output.toString() shouldContain "Invalid did-scheme"
    }

    "verify command should detect structure validation errors" {
        val auditJson = """
        {
          "audit_log": [
            {
              "id": "",
              "timestamp": "",
              "action": { "name": "", "parameters": {} },
              "notificationHashes": [],
              "stateHash": "",
              "previousHash": null,
              "proof": {
                "type": "Ed25519Signature2020",
                "created": "2025-01-01T00:00:00Z",
                "verificationMethod": "",
                "proofPurpose": "assertionMethod",
                "jws": ""
              }
            }
          ],
          "state": {}
        }
        """.trimIndent()

        val auditFile = File.createTempFile("audit", ".json")
        auditFile.writeText(auditJson)
        auditFile.deleteOnExit()

        val sourcesDir = File("src/test/resources/verify-fixtures/npl-sources")

        val output = ColorWriter(StringWriter(), false)
        val command = VerifyCommand(
            audit = auditFile.absolutePath,
            sources = sourcesDir.absolutePath,
            didScheme = "https",
            didHostOverride = null,
            failFast = false,
            jsonOutput = false,
            enableReplay = false
        )

        val exitCode = command.execute(output)

        exitCode shouldBe ExitCode.DATA_ERROR
        output.toString() shouldContain "Structure"
    }

    "verify command should output JSON when --json flag is used" {
        val auditJson = """
        {
          "audit_log": [
            {
              "id": "test-id",
              "timestamp": "2025-01-01T00:00:00Z",
              "action": { "name": "constructor", "parameters": {} },
              "notificationHashes": [],
              "stateHash": "sha256:abc",
              "previousHash": null,
              "proof": {
                "type": "Ed25519Signature2020",
                "created": "2025-01-01T00:00:00Z",
                "verificationMethod": "did:web:example.com#key-1",
                "proofPurpose": "assertionMethod",
                "jws": "eyJhbGciOiJFZERTQSJ9..sig"
              }
            }
          ],
          "state": { "value": 0 }
        }
        """.trimIndent()

        val auditFile = File.createTempFile("audit", ".json")
        auditFile.writeText(auditJson)
        auditFile.deleteOnExit()

        val sourcesDir = File("src/test/resources/verify-fixtures/npl-sources")

        val mockServer = MockWebServer()
        mockServer.start()

        try {
            val didDoc = """
            {
              "id": "did:web:example.com",
              "verificationMethod": [
                {
                  "id": "did:web:example.com#key-1",
                  "type": "Ed25519VerificationKey2020",
                  "controller": "did:web:example.com",
                  "publicKeyJwk": {
                    "kty": "OKP",
                    "crv": "Ed25519",
                    "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
                  }
                }
              ]
            }
            """.trimIndent()

            mockServer.enqueue(MockResponse().setBody(didDoc).setResponseCode(200))

            val output = ColorWriter(StringWriter(), false)
            val command = VerifyCommand(
                audit = auditFile.absolutePath,
                sources = sourcesDir.absolutePath,
                didScheme = "http",
                didHostOverride = "localhost:${mockServer.port}",
                failFast = false,
                jsonOutput = true,
                enableReplay = false
            )

            val exitCode = command.execute(output)

            exitCode shouldBe ExitCode.DATA_ERROR
            val outputStr = output.toString()
            outputStr shouldContain "\"success\""
            outputStr shouldContain "\"errors\""
        } finally {
            mockServer.shutdown()
        }
    }
})

