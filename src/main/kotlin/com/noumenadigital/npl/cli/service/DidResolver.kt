package com.noumenadigital.npl.cli.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.model.DidDocument
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.util.concurrent.ConcurrentHashMap

class DidResolver(
    private val didScheme: String = "https",
    private val didHostOverride: String? = null,
) {
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val cache = ConcurrentHashMap<String, DidDocument>()

    fun resolve(didUri: String): DidDocument {
        return cache.getOrPut(didUri) {
            resolveFresh(didUri)
        }
    }

    private fun resolveFresh(didUri: String): DidDocument {
        val url = didUriToHttpUrl(didUri)

        try {
            HttpClients.createDefault().use { client ->
                val request = HttpGet(url)
                client.execute(request).use { response ->
                    if (response.statusLine.statusCode != 200) {
                        throw DidResolutionException(
                            "Failed to resolve DID $didUri: HTTP ${response.statusLine.statusCode} from $url"
                        )
                    }

                    val body = EntityUtils.toString(response.entity)
                    return objectMapper.readValue(body)
                }
            }
        } catch (e: DidResolutionException) {
            throw e
        } catch (e: Exception) {
            throw DidResolutionException("Failed to resolve DID $didUri from $url: ${e.message}", e)
        }
    }

    private fun didUriToHttpUrl(didUri: String): String {
        if (!didUri.startsWith("did:web:")) {
            throw DidResolutionException("Only did:web method is supported, got: $didUri")
        }

        val didPath = didUri.removePrefix("did:web:")
        val parts = didPath.split(":")

        val host = didHostOverride ?: parts[0]

        return if (parts.size == 1) {
            // did:web:example.com -> https://example.com/.well-known/did.json
            "$didScheme://$host/.well-known/did.json"
        } else {
            // did:web:example.com:path:to:did -> https://example.com/path/to/did/did.json
            val path = parts.drop(1).joinToString("/")
            "$didScheme://$host/$path/did.json"
        }
    }
}

class DidResolutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

