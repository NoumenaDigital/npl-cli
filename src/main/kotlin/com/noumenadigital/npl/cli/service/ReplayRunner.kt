package com.noumenadigital.npl.cli.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.model.AuditEntry
import com.noumenadigital.npl.cli.model.AuditResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.erdtman.jcs.JsonCanonicalizer
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Configuration for replay verification
 */
data class ReplayConfig(
    val sourcesPath: String,
    val baseUrl: String = "http://localhost:12000",
    val dockerComposeCmd: String = "docker compose up -d --wait",
    val deployCmd: String = "npl deploy",
    val skipDocker: Boolean = false,
    val cleanup: Boolean = false,
    val verbose: Boolean = false,
)

/**
 * Handles replay verification by executing audit actions against a live NPL runtime
 */
class ReplayRunner(
    private val config: ReplayConfig,
) {
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    private val httpClient = HttpClients.createDefault()

    // Actual base URL to use (may be overridden from npl.yml)
    private var actualBaseUrl: String = config.baseUrl

    fun runReplay(auditResponse: AuditResponse): ReplayResult {
        val errors = mutableListOf<ReplayError>()

        try {
            // Step 1: Ensure actualBaseUrl is set to the running NPL node
            if (System.getenv("NPL_BASE_URL") == null) {
                val detectedUrl = detectManagementUrl()
                if (detectedUrl != null) {
                    actualBaseUrl = detectedUrl
                    log("Using managementUrl from npl.yml: $actualBaseUrl")
                } else {
                    log("Using default base URL: $actualBaseUrl")
                }
            } else {
                log("Using NPL_BASE_URL from environment: $actualBaseUrl")
            }

            // Default actualBaseUrl to localhost:12000 if not set in the YAML or command-line flags
            actualBaseUrl = config.baseUrl // Start with the default from ReplayConfig

            // Step 2: Parse protocol identity from first entry
            if (auditResponse.auditLog.isEmpty()) {
                errors.add(ReplayError(0, "Audit log is empty, cannot replay"))
                return ReplayResult(false, errors)
            }

            val firstEntry = auditResponse.auditLog[0]
            val protocolIdentity = parseProtocolIdentity(firstEntry.id)
            log("Replaying protocol: ${protocolIdentity.protocolName} (${protocolIdentity.protocolId})")

            // Step 3: Replay each audit entry
            var createdProtocolId: String? = null

            auditResponse.auditLog.forEachIndexed { index, entry ->
                log("Replaying entry $index: ${entry.action.name} (${entry.action.type})")

                try {
                    when (entry.action.type) {
                        "constructor" -> {
                            createdProtocolId = replayConstructor(entry, protocolIdentity, auditResponse.state, errors)
                            if (createdProtocolId == null) {
                                return@forEachIndexed
                            }
                        }
                        "permission", "obligation" -> {
                            val protocolIdToUse = createdProtocolId
                            if (protocolIdToUse == null) {
                                errors.add(ReplayError(index, "Cannot replay action before constructor"))
                                return@forEachIndexed
                            }
                            replayAction(entry, protocolIdentity, protocolIdToUse, errors)
                        }
                        else -> {
                            errors.add(ReplayError(index, "Unknown action type: ${entry.action.type}"))
                            return@forEachIndexed
                        }
                    }

                    // Verify state hash after each action
                    val protocolIdToUse = createdProtocolId
                    if (protocolIdToUse != null) {
                        verifyStateHash(entry, protocolIdentity, protocolIdToUse, index, errors)
                    }

                } catch (e: Exception) {
                    errors.add(ReplayError(index, "Replay failed: ${e.message}"))
                    e.printStackTrace()
                }
            }

            return ReplayResult(errors.isEmpty(), errors)

        } finally {
            // No cleanup needed as we are not starting Docker runtime
        }
    }

    private fun findNplYmlDirectory(startDir: File): File? {
        var currentDir = if (startDir.isDirectory) startDir else startDir.parentFile

        // Search up to 5 levels up
        for (i in 0..5) {
            if (currentDir == null) break

            val ymlFile = File(currentDir, "npl.yml")
            val yamlFile = File(currentDir, "npl.yaml")

            if (ymlFile.exists() || yamlFile.exists()) {
                return currentDir
            }

            currentDir = currentDir.parentFile
        }

        return null
    }

    private fun detectManagementUrl(): String? {
        try {
            val sourcesDir = File(config.sourcesPath).canonicalFile
            val workDir = findNplYmlDirectory(sourcesDir) ?: return null

            val ymlFile = File(workDir, "npl.yml")
            val yamlFile = File(workDir, "npl.yaml")
            val configFile = when {
                ymlFile.exists() -> ymlFile
                yamlFile.exists() -> yamlFile
                else -> return null
            }

            // Parse YAML and extract local.managementUrl
            val configMap: Map<String, Any> = yamlMapper.readValue(configFile)
            val localConfig = configMap["local"] as? Map<*, *>
            val managementUrl = localConfig?.get("managementUrl") as? String

            return managementUrl
        } catch (e: Exception) {
            log("Warning: Could not read managementUrl from npl.yml: ${e.message}")
            return null
        }
    }

    private fun parseProtocolIdentity(entryId: String): ProtocolIdentity {
        // Format: urn:npl:{host}/npl/{packagePath}/{protocolName}/{uuid}#{index}
        if (!entryId.startsWith("urn:npl:")) {
            throw IllegalArgumentException("Invalid entry ID format: $entryId")
        }

        val withoutUrn = entryId.removePrefix("urn:npl:")
        val parts = withoutUrn.split("/npl/")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid entry ID format (missing /npl/): $entryId")
        }

        val host = parts[0]
        val pathParts = parts[1].split("/")
        if (pathParts.size < 3) {
            throw IllegalArgumentException("Invalid entry ID format (insufficient path parts): $entryId")
        }

        val packagePath = pathParts[0]
        val protocolName = pathParts[1]
        val uuidWithIndex = pathParts[2]
        val uuid = if (uuidWithIndex.contains("#")) {
            uuidWithIndex.substringBefore("#")
        } else {
            uuidWithIndex
        }

        return ProtocolIdentity(host, packagePath, protocolName, uuid)
    }

    private fun replayConstructor(
        entry: AuditEntry,
        identity: ProtocolIdentity,
        state: Map<String, Any>,
        errors: MutableList<ReplayError>
    ): String? {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/"
        log("  POST $url")

        // Extract parties from state
        val parties = state["@parties"] ?: emptyMap<String, Any>()

        // Build constructor body with parties
        val bodyMap = mutableMapOf(
            "@parties" to parties
        )

        // Add constructor parameters if present
        entry.action.parameters?.forEach { (key, value) ->
            bodyMap[key] = value
        }

        val bodyJson = objectMapper.writeValueAsString(bodyMap)

        try {
            val request = HttpPost(url).apply {
                entity = StringEntity(bodyJson, "UTF-8")
                setHeader("Content-Type", "application/json")
            }

            httpClient.execute(request).use { response ->
                val responseBody = EntityUtils.toString(response.entity)

                if (response.statusLine.statusCode !in 200..299) {
                    errors.add(
                        ReplayError(
                            0,
                            "Constructor call failed: HTTP ${response.statusLine.statusCode} - $responseBody"
                        )
                    )
                    return null
                }

                // Extract @id from response
                val responseMap: Map<String, Any> = objectMapper.readValue(responseBody)
                val createdId = responseMap["@id"] as? String

                if (createdId == null) {
                    errors.add(ReplayError(0, "Constructor response missing @id"))
                    return null
                }

                log("  Created protocol with @id: $createdId")

                // Verify UUID matches expected (if runtime provides it)
                if (createdId != identity.protocolId) {
                    log("  WARNING: Created ID ($createdId) differs from audit ID (${identity.protocolId})")
                }

                return createdId
            }
        } catch (e: Exception) {
            errors.add(ReplayError(0, "Constructor HTTP request failed: ${e.message}"))
            return null
        }
    }

    private fun replayAction(
        entry: AuditEntry,
        identity: ProtocolIdentity,
        protocolId: String,
        errors: MutableList<ReplayError>
    ) {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/$protocolId/${entry.action.name}"
        log("  POST $url")

        // Build action body from parameters
        val bodyMap = entry.action.parameters ?: emptyMap()
        val bodyJson = objectMapper.writeValueAsString(bodyMap)

        try {
            val request = HttpPost(url).apply {
                entity = StringEntity(bodyJson, "UTF-8")
                setHeader("Content-Type", "application/json")
            }

            httpClient.execute(request).use { response ->
                val responseBody = EntityUtils.toString(response.entity)

                if (response.statusLine.statusCode !in 200..299) {
                    errors.add(
                        ReplayError(
                            -1,
                            "Action ${entry.action.name} failed: HTTP ${response.statusLine.statusCode} - $responseBody"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            errors.add(ReplayError(-1, "Action ${entry.action.name} HTTP request failed: ${e.message}"))
        }
    }

    private fun verifyStateHash(
        entry: AuditEntry,
        identity: ProtocolIdentity,
        protocolId: String,
        index: Int,
        errors: MutableList<ReplayError>
    ) {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/$protocolId"
        log("  GET $url (verify state)")

        try {
            val request = HttpGet(url)
            httpClient.execute(request).use { response ->
                if (response.statusLine.statusCode != 200) {
                    errors.add(
                        ReplayError(
                            index,
                            "Failed to fetch state: HTTP ${response.statusLine.statusCode}"
                        )
                    )
                    return
                }

                val stateJson = EntityUtils.toString(response.entity)

                // Compute hash of the pretty state directly
                val computedHash = computeStateHash(stateJson)

                log("  Expected: ${entry.stateHash}")
                log("  Computed: $computedHash")

                if (entry.stateHash != computedHash) {
                    errors.add(
                        ReplayError(
                            index,
                            "State hash mismatch at entry $index.\n" +
                                    "    Expected: ${entry.stateHash}\n" +
                                    "    Computed: $computedHash\n" +
                                    "    State JSON: ${stateJson.take(200)}..."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            errors.add(ReplayError(index, "Failed to verify state hash: ${e.message}"))
        }
    }

    private fun computeStateHash(json: String): String {
        // Canonicalize using JCS, then SHA-256
        val canonicalJson = JsonCanonicalizer(json).encodedString
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(canonicalJson.toByteArray(StandardCharsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun log(message: String) {
        if (config.verbose) {
            println(message)
        }
    }
}

data class ProtocolIdentity(
    val host: String,
    val packagePath: String,
    val protocolName: String,
    val protocolId: String,
)

data class ReplayResult(
    val success: Boolean,
    val errors: List<ReplayError>,
)

data class ReplayError(
    val entryIndex: Int,
    val message: String,
)
