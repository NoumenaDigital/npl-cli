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
    private var actualBaseUrl: String = config.baseUrl

    // Extract party-to-token mapping from npl.yml
    private val partyTokens: Map<String, String> by lazy {
        try {
            val sourcesDir = File(config.sourcesPath).canonicalFile
            val workDir = findNplYmlDirectory(sourcesDir) ?: throw IllegalStateException("npl.yml not found")

            val ymlFile = File(workDir, "npl.yml")
            val yamlFile = File(workDir, "npl.yaml")
            val configFile = when {
                ymlFile.exists() -> ymlFile
                yamlFile.exists() -> yamlFile
                else -> throw IllegalStateException("npl.yml or npl.yaml not found")
            }

            val configMap: Map<String, Any> = yamlMapper.readValue(configFile)
            val localConfig = configMap["local"] as? Map<*, *>

            // Check if we have username/password authentication
            val username = localConfig?.get("username") as? String
            val password = localConfig?.get("password") as? String
            val authUrl = localConfig?.get("authUrl") as? String

            // Support both old format (single authorization) and new format (parties map)
            val partiesMap = localConfig?.get("parties") as? Map<*, *>

            if (partiesMap != null) {
                // Check if parties map contains credentials instead of tokens
                val firstPartyValue = partiesMap.values.firstOrNull()

                if (firstPartyValue is Map<*, *> && firstPartyValue.containsKey("username")) {
                    // New format with credentials: parties: { alice: { username: "alice", password: "pwd" } }
                    log("Obtaining JWT tokens from auth server for ${partiesMap.size} parties...")
                    partiesMap.entries.associate { (party, creds) ->
                        val credsMap = creds as? Map<*, *>
                        val partyUsername = credsMap?.get("username") as? String
                        val partyPassword = credsMap?.get("password") as? String

                        if (partyUsername != null && partyPassword != null && authUrl != null) {
                            val token = obtainToken(authUrl, partyUsername, partyPassword)
                            party.toString() to token
                        } else {
                            log("  Warning: Missing credentials for party $party")
                            party.toString() to ""
                        }
                    }.filterValues { it.isNotEmpty() }.also {
                        log("Obtained ${it.size} JWT tokens from auth server")
                    }
                } else {
                    // New format with pre-existing tokens: parties: { alice: "token1", bob: "token2" }
                    partiesMap.entries.associate { (party, token) ->
                        party.toString() to token.toString()
                    }.also {
                        log("Loaded ${it.size} party tokens from npl.yml")
                    }
                }
            } else if (username != null && password != null && authUrl != null) {
                // Old format with username/password - obtain token
                log("Obtaining JWT token from auth server for user $username...")
                val token = obtainToken(authUrl, username, password)
                mapOf("default" to token).also {
                    log("Obtained JWT token from auth server")
                }
            } else {
                // Old format: single authorization token
                val authToken = localConfig?.get("authorization") as? String
                if (authToken != null) {
                    log("Using single authorization token (consider migrating to parties map)")
                    mapOf("default" to authToken)
                } else {
                    log("Warning: No authorization tokens found in npl.yml")
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            log("Warning: Could not read authorization tokens from npl.yml: ${e.message}")
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Obtain a JWT token from the auth server using username/password.
     */
    private fun obtainToken(authUrl: String, username: String, password: String): String {
        try {
            // Try OIDC token endpoint
            val tokenUrl = "$authUrl/token"

            log("  Requesting token for $username from $tokenUrl")

            val request = HttpPost(tokenUrl).apply {
                setHeader("Content-Type", "application/x-www-form-urlencoded")
                entity = StringEntity(
                    "grant_type=password&client_id=npl-cli&username=$username&password=$password",
                    "UTF-8"
                )
            }

            httpClient.execute(request).use { response ->
                val responseBody = EntityUtils.toString(response.entity)

                if (response.statusLine.statusCode == 200) {
                    val tokenResponse: Map<String, Any> = objectMapper.readValue(responseBody)
                    val accessToken = tokenResponse["access_token"] as? String

                    if (accessToken != null) {
                        log("  ✓ Successfully obtained token for $username")
                        return accessToken
                    } else {
                        throw IllegalStateException("No access_token in response")
                    }
                } else {
                    throw IllegalStateException("Failed to obtain token: HTTP ${response.statusLine.statusCode} - $responseBody")
                }
            }
        } catch (e: Exception) {
            log("  ✗ Failed to obtain token for $username: ${e.message}")
            throw e
        }
    }

    fun runReplay(auditResponse: AuditResponse): ReplayResult {
        val errors = mutableListOf<ReplayError>()

        try {
            // Step 1: Ensure actualBaseUrl is set to the running NPL node
            // Priority: 1) NPL_BASE_URL env var, 2) managementUrl from npl.yml, 3) config.baseUrl
            val envBaseUrl = System.getenv("NPL_BASE_URL")
            if (envBaseUrl != null) {
                actualBaseUrl = envBaseUrl
                log("Using NPL_BASE_URL from environment: $actualBaseUrl")
            } else {
                actualBaseUrl = config.baseUrl
            }

            // Step 2: Parse protocol identity from first entry
            if (auditResponse.auditLog.isEmpty()) {
                errors.add(ReplayError(0, "Audit log is empty, cannot replay"))
                return ReplayResult(false, errors)
            }

            val firstEntry = auditResponse.auditLog[0]
            val protocolIdentity = parseProtocolIdentity(firstEntry.id)
            log("Replaying protocol: ${protocolIdentity.protocolName} (${protocolIdentity.protocolId})")

            // Step 3: Extract protocol parties from state
            val protocolParties = extractProtocolParties(auditResponse.state)
            log("Protocol parties: $protocolParties")

            // Step 4: Replay each audit entry
            var createdProtocolId: String? = null

            auditResponse.auditLog.forEachIndexed { index, entry ->
                log("Replaying entry $index: ${entry.action.name} (${entry.action.type})")

                try {
                    when (entry.action.type) {
                        "constructor" -> {
                            createdProtocolId = replayConstructor(entry, protocolIdentity, auditResponse.state, protocolParties, errors)
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
                            replayAction(entry, protocolIdentity, protocolIdToUse, protocolParties, errors)
                        }
                        else -> {
                            errors.add(ReplayError(index, "Unknown action type: ${entry.action.type}"))
                            return@forEachIndexed
                        }
                    }

                    // Verify state hash after each action
                    val protocolIdToUse = createdProtocolId
                    if (protocolIdToUse != null) {
                        verifyStateHash(entry, protocolIdentity, protocolIdToUse, index, auditResponse.state, errors)
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
        protocolParties: Set<String>,
        errors: MutableList<ReplayError>
    ): String? {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/"
        log("  POST $url")

        // Extract parties from state
        val parties = state["@parties"] ?: emptyMap<String, Any>()

        // Build constructor body with parties
        val bodyMap = mutableMapOf<String, Any>(
            "@parties" to parties
        )

        // Add constructor parameters from the action (with type normalization)
        entry.action.parameters?.forEach { (key, value) ->
            // Skip generic arg0, arg1, etc. - these are internal representations
            if (!key.matches(Regex("arg\\d+"))) {
                bodyMap[key] = normalizeTypes(value)
            }
        }

        // Also add state fields that look like constructor parameters (not metadata, not internal)
        // This handles cases where the audit has arg0 but the actual parameter name is in state
        state.forEach { (key, value) ->
            if (!key.startsWith("@") &&
                !key.matches(Regex("arg\\d+")) &&
                !bodyMap.containsKey(key) &&
                key != "payments") { // Skip state variables like 'payments' that are initialized in constructor
                bodyMap[key] = normalizeTypes(value)
            }
        }

        val bodyJson = objectMapper.writeValueAsString(bodyMap)
        log("  Constructor body: $bodyJson")

        // Try each available token until one succeeds
        // Constructor doesn't have a specific action name, so we try protocol parties
        val tokensToTry = getAuthHeadersToTry("constructor", identity, protocolParties)

        if (tokensToTry.isEmpty()) {
            errors.add(ReplayError(0, "Constructor failed: No authentication tokens available"))
            return null
        }

        var lastError: String? = null

        for ((partyName, token) in tokensToTry) {
            try {
                log("    Trying token for party: $partyName")
                val request = HttpPost(url).apply {
                    entity = StringEntity(bodyJson, "UTF-8")
                    setHeader("Content-Type", "application/json")
                    setHeader("Authorization", "Bearer $token")
                }

                httpClient.execute(request).use { response ->
                    // Safely handle null response entity
                    val responseBody = response.entity?.let { EntityUtils.toString(it) } ?: ""
                    val statusCode = response.statusLine.statusCode

                    when {
                        statusCode in 200..299 -> {
                            log("    ✓ Success with party: $partyName")

                            // Extract @id from response
                            if (responseBody.isEmpty()) {
                                errors.add(ReplayError(0, "Constructor response is empty"))
                                return null
                            }

                            val responseMap: Map<String, Any> = objectMapper.readValue(responseBody)
                            val createdId = responseMap["@id"] as? String

                            if (createdId == null) {
                                errors.add(ReplayError(0, "Constructor response missing @id"))
                                return null
                            }

                            log("  Created protocol with @id: $createdId")
                            log("  Full response: $responseBody")

                            // Verify UUID matches expected (if runtime provides it)
                            if (createdId != identity.protocolId) {
                                log("  WARNING: Created ID ($createdId) differs from audit ID (${identity.protocolId})")
                            }

                            return createdId
                        }
                        statusCode == 403 -> {
                            // Permission denied, try next token
                            log("    ✗ Permission denied for party: $partyName")
                            lastError = "HTTP 403 - Permission denied"
                            // Continue to next token
                        }
                        else -> {
                            // Other error, record it but don't try other tokens
                            log("    ✗ Failed with party: $partyName - HTTP $statusCode")
                            errors.add(
                                ReplayError(
                                    0,
                                    "Constructor call failed: HTTP $statusCode - $responseBody"
                                )
                            )
                            return null
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                log("    ✗ Exception with party $partyName: ${e.message}")
            }
        }

        errors.add(
            ReplayError(
                0,
                "Constructor failed: All ${tokensToTry.size} party tokens were denied permission. " +
                "Last error: $lastError"
            )
        )
        return null
    }

    private fun replayAction(
        entry: AuditEntry,
        identity: ProtocolIdentity,
        protocolId: String,
        protocolParties: Set<String>,
        errors: MutableList<ReplayError>
    ) {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/$protocolId/${entry.action.name}"
        log("  POST $url")

        // Build action body from parameters and normalize types
        val rawBodyMap = entry.action.parameters ?: emptyMap()
        val bodyMap = rawBodyMap.mapValues { (_, value) -> normalizeTypes(value) }

        // Log parameter types for debugging
        bodyMap.forEach { (key, value) ->
            log("    Parameter '$key': ${value::class.simpleName} = $value")
        }

        // Ensure bodyJson is never null or empty - use "{}" for actions with no parameters
        val bodyJson = if (bodyMap.isEmpty()) {
            "{}"
        } else {
            objectMapper.writeValueAsString(bodyMap)
        }
        log("  Action body: $bodyJson")

        // Try each available token until one succeeds
        val tokensToTry = getAuthHeadersToTry(entry.action.name, identity, protocolParties)

        if (tokensToTry.isEmpty()) {
            errors.add(ReplayError(-1, "Action ${entry.action.name} failed: No authentication tokens available"))
            return
        }

        var lastError: String? = null

        for ((partyName, token) in tokensToTry) {
            try {
                log("    Trying token for party: $partyName")
                val request = HttpPost(url).apply {
                    // Ensure entity is never null by always providing a valid JSON body
                    entity = StringEntity(bodyJson, StandardCharsets.UTF_8)
                    setHeader("Content-Type", "application/json")
                    setHeader("Authorization", "Bearer $token")
                }

                httpClient.execute(request).use { response ->
                    // Safely handle null response entity (some responses may not have a body)
                    val responseBody = response.entity?.let { EntityUtils.toString(it) } ?: ""
                    val statusCode = response.statusLine.statusCode

                    when {
                        statusCode in 200..299 -> {
                            log("    ✓ Success with party: $partyName")
                            return // Success!
                        }
                        statusCode == 403 -> {
                            // Permission denied, try next token
                            log("    ✗ Permission denied for party: $partyName")
                            lastError = "HTTP 403 - Permission denied"
                            // Continue to next token
                        }
                        else -> {
                            // Other error, record it but don't try other tokens
                            log("    ✗ Failed with party: $partyName - HTTP $statusCode")
                            errors.add(
                                ReplayError(
                                    -1,
                                    "Action ${entry.action.name} failed: HTTP $statusCode - $responseBody"
                                )
                            )
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                log("    ✗ Exception with party $partyName: ${e.message}")
            }
        }

        errors.add(
            ReplayError(
                -1,
                "Action ${entry.action.name} failed: All ${tokensToTry.size} party tokens were denied permission. " +
                "Last error: $lastError"
            )
        )
    }

    private fun verifyStateHash(
        entry: AuditEntry,
        identity: ProtocolIdentity,
        protocolId: String,
        index: Int,
        auditState: Map<String, Any>,
        errors: MutableList<ReplayError>
    ) {
        val url = "$actualBaseUrl/npl/${identity.packagePath}/${identity.protocolName}/$protocolId/"
        log("  GET $url (verify state)")
        log("    Using protocolId: $protocolId")
        log("    Full URL breakdown: baseUrl=$actualBaseUrl, package=${identity.packagePath}, protocol=${identity.protocolName}, id=$protocolId")

        try {
            // Use any available party token for authentication (state is readable by all parties)
            val token = partyTokens.values.firstOrNull()

            if (token == null) {
                errors.add(ReplayError(index, "Failed to fetch state: No authentication tokens available"))
                return
            }

            val request = HttpGet(url).apply {
                setHeader("Authorization", "Bearer $token")
            }

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

                val restState: Map<String, Any?> = objectMapper.readValue(stateJson)

                // Normalize the protocol ID in the fetched state to match the audited ID
                // This allows comparison even when replay creates a new instance with a different UUID
                // IMPORTANT: This ONLY normalizes @id and @actions URLs - all other fields (like @state)
                // are left unchanged, so NPL code changes WILL be detected
                val normalizedRestState = normalizeProtocolId(restState, protocolId, identity.protocolId)

                // Apply the same projection that backend uses to compute the audit hash
                val replayState = ReplayStateProjection.fromRestState(normalizedRestState)

                val computedHash = computeStateHash(objectMapper.writeValueAsString(replayState))

                // For debugging: also log the actual state representation
                val replayStateJson = objectMapper.writeValueAsString(replayState)
                val canonicalReplayState = JsonCanonicalizer(replayStateJson).encodedString

                log("  Expected: ${entry.stateHash}")
                log("  Computed: $computedHash")
                log("  Replay state: @state=${restState["@state"]}, fields=${restState.filterKeys { !it.startsWith("@") }.keys}")

                if (index == 0) {
                    log("  [DEBUG] Audit state (@state from audit): ${auditState["@state"]}")
                    log("  [DEBUG] Replay state (@state from replay): ${restState["@state"]}")
                    log("  [DEBUG] Match: ${auditState["@state"] == restState["@state"]}")
                }

                if (entry.stateHash != computedHash) {
                    // Enhanced debugging: show what's different
                    errors.add(
                        ReplayError(
                            index,
                            "State hash mismatch at entry $index.\n" +
                                    "    Expected: ${entry.stateHash}\n" +
                                    "    Computed: $computedHash\n" +
                                    "    This likely means the NPL code has changed.\n" +
                                    "    Replay state @state: ${restState["@state"]}\n" +
                                    "    Canonical state (first 400 chars): ${canonicalReplayState.take(400)}..."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            errors.add(ReplayError(index, "Failed to verify state hash: ${e.message}"))
        }
    }

    /**
     * Normalize protocol IDs in the state to allow comparison between different instances.
     * Replaces runtime protocol ID with audited protocol ID in @id and @actions fields.
     */
    private fun normalizeProtocolId(
        state: Map<String, Any?>,
        runtimeId: String,
        auditedId: String
    ): Map<String, Any?> {
        if (runtimeId == auditedId) {
            return state
        }

        val normalized = state.toMutableMap()

        // Normalize @id field
        if (normalized.containsKey("@id")) {
            val id = normalized["@id"]
            if (id is String) {
                normalized["@id"] = id.replace(runtimeId, auditedId)
            }
        }

        // Normalize @actions URLs
        if (normalized.containsKey("@actions")) {
            val actions = normalized["@actions"]
            if (actions is Map<*, *>) {
                normalized["@actions"] = actions.mapKeys { it.key.toString() }.mapValues { (_, url) ->
                    when (url) {
                        is String -> url.replace("/$runtimeId/", "/$auditedId/")
                        else -> url
                    }
                }
            }
        }

        return normalized
    }

    private fun computeStateHash(json: String): String {
        // Canonicalize using JCS, then SHA-256
        val canonicalJson = JsonCanonicalizer(json).encodedString
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(canonicalJson.toByteArray(StandardCharsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extract party names from the protocol's @parties field in the state.
     */
    private fun extractProtocolParties(state: Map<String, Any>): Set<String> {
        val parties = state["@parties"] as? Map<*, *> ?: return emptySet()
        return parties.keys.mapNotNull { it?.toString() }.toSet()
    }

    /**
     * Parse NPL source code to extract which party can execute which action.
     * Returns a map of actionName -> partyName
     */
    private fun parseNplPermissions(protocolName: String, packagePath: String): Map<String, String> {
        val permissionMap = mutableMapOf<String, String>()

        try {
            val sourcesDir = File(config.sourcesPath).canonicalFile

            // Search for .npl files in the sources directory
            val nplFiles = sourcesDir.walk()
                .filter { it.extension == "npl" }
                .filter {
                    val content = it.readText()
                    // Check if this file contains the protocol we're looking for
                    content.contains("protocol") && content.contains(protocolName)
                }
                .toList()

            if (nplFiles.isEmpty()) {
                log("  Warning: No NPL files found for protocol $protocolName")
                return emptyMap()
            }

            for (nplFile in nplFiles) {
                val content = nplFile.readText()

                // Parse permissions and obligations
                // Pattern: permission[party] actionName(...) or permission[party1|party2] actionName(...)
                val permissionRegex = """(permission|obligation)\[([^\]]+)\]\s+(\w+)\s*\(""".toRegex()

                permissionRegex.findAll(content).forEach { match ->
                    val parties = match.groupValues[2].split("|").map { it.trim() }
                    val actionName = match.groupValues[3]

                    // For simplicity, use the first party if multiple are defined
                    // In a real scenario, we might need to try all parties
                    if (parties.isNotEmpty()) {
                        permissionMap[actionName] = parties[0]
                        log("  Found: action '$actionName' -> party '${parties[0]}'")

                        // If multiple parties, log them
                        if (parties.size > 1) {
                            log("    (also accessible by: ${parties.drop(1).joinToString(", ")})")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            log("  Warning: Failed to parse NPL permissions: ${e.message}")
        }

        return permissionMap
    }

    /**
     * Get authorization headers to try for an action.
     * Returns a list of (partyName, token) pairs to try in order.
     * Priority: 1) NPL-defined party for action, 2) protocol parties, 3) other tokens
     */
    private fun getAuthHeadersToTry(
        actionName: String,
        protocolIdentity: ProtocolIdentity,
        protocolParties: Set<String>
    ): List<Pair<String, String>> {
        val tokensToTry = mutableListOf<Pair<String, String>>()

        // Parse NPL to find which party owns this action
        val actionToPartyMap = parseNplPermissions(protocolIdentity.protocolName, protocolIdentity.packagePath)
        val actionParty = actionToPartyMap[actionName]

        // Priority 1: Try the party that owns this action according to NPL
        if (actionParty != null && partyTokens.containsKey(actionParty)) {
            log("  Action '$actionName' is owned by party '$actionParty' (from NPL)")
            tokensToTry.add(actionParty to partyTokens[actionParty]!!)
        }

        // Priority 2: Try other protocol parties
        for (party in protocolParties) {
            if (party != actionParty && partyTokens.containsKey(party)) {
                tokensToTry.add(party to partyTokens[party]!!)
            }
        }

        // Priority 3: Try remaining tokens
        for ((party, token) in partyTokens) {
            if (party != actionParty && !protocolParties.contains(party)) {
                tokensToTry.add(party to token)
            }
        }

        return tokensToTry
    }

    /**
     * Converts string values that look like numbers into actual numeric types.
     * This is needed because audit trail JSON may have numeric values as strings.
     */
    private fun normalizeTypes(value: Any): Any {
        return when (value) {
            is String -> {
                // Try to convert string to number if it looks like one
                value.toIntOrNull()
                    ?: value.toDoubleOrNull()
                    ?: value.toBooleanStrictOrNull()
                    ?: value
            }
            is Map<*, *> -> {
                value.mapValues { (_, v) -> if (v != null) normalizeTypes(v) else null }
            }
            is List<*> -> {
                value.map { if (it != null) normalizeTypes(it) else null }
            }
            else -> value
        }
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
