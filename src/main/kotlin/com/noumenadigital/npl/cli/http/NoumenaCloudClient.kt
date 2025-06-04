package com.noumenadigital.npl.cli.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CloudRestCallException
import com.noumenadigital.npl.cli.exception.CloudSlowDownException
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

data class NoumenaCloudConfig(
    val clientId: String,
    val clientSecret: String,
    val baseUrl: String,
    val realm: String,
)

open class NoumenaCloudClient(
    config: NoumenaCloudConfig,
) {
    private val clientId = config.clientId
    private val clientSecret = config.clientSecret
    private val baseUrl = config.baseUrl
    private val realm = config.realm
    private val keycloakUrl = "$baseUrl/realms/$realm/protocol/openid-connect"
    private val deviceGrantType = "urn:ietf:params:oauth:grant-type:device_code"
    private val scope = "openid offline_access"
    private val objectMapper = jacksonObjectMapper()
    private val client = HttpClients.createDefault() // ✅ Reuse this

    open fun requestDeviceCode(): DeviceCodeResponse {
        try {
            val httpPost = HttpPost("$keycloakUrl/auth/device")
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded")
            httpPost.entity =
                UrlEncodedFormEntity(
                    listOf(
                        BasicNameValuePair("client_id", clientId),
                        BasicNameValuePair("scope", "openid"),
                    ),
                )

            client.execute(httpPost).use { response ->
                val entity = response.entity ?: throw CloudRestCallException("Empty response entity")
                val json = EntityUtils.toString(entity)
                if (response.statusLine.statusCode != 200) {
                    throw CloudRestCallException("Error: ${response.statusLine.statusCode} - $json")
                }
                return objectMapper.readValue(json)
            }
        } catch (ex: Exception) {
            throw CloudRestCallException(ex.message ?: ex.cause?.message ?: "Unknown error")
        }
    }

    open fun requestToken(deviceCode: DeviceCodeResponse): TokenResponse {
        val httpPost = HttpPost("$keycloakUrl/token")
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded")
        httpPost.entity =
            UrlEncodedFormEntity(
                listOf(
                    BasicNameValuePair("client_id", clientId),
                    BasicNameValuePair("grant_type", deviceGrantType),
                    BasicNameValuePair("device_code", deviceCode.deviceCode),
                    BasicNameValuePair("client_secret", clientSecret),
                    BasicNameValuePair("scope", scope),
                ),
            )

        client.execute(httpPost).use { response ->
            val entity = response.entity ?: throw CloudRestCallException("Empty response entity")
            val json = EntityUtils.toString(entity)

            return when (response.statusLine.statusCode) {
                200 -> objectMapper.readValue(json)
                400 -> {
                    val node = objectMapper.readTree(json)
                    when (node["error"]?.asText()?.lowercase()) {
                        "authorization_pending" -> throw CloudAuthorizationPendingException()
                        "slow_down" -> throw CloudSlowDownException()
                        else -> throw CloudCommandException(
                            node["error_description"]?.asText() ?: "Authorization failed",
                        )
                    }
                }

                else -> throw CloudRestCallException("Error: ${response.statusLine.statusCode} - $json")
            }
        }
    }
}
