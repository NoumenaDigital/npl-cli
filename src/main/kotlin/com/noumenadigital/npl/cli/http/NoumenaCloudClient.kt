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

class NoumenaCloudClient(
    config: NoumenaCloudConfig,
) {
    private val clientId = config.clientId
    private val clientSecret = config.clientSecret
    private val baseUrl = config.baseUrl
    private val realm = config.realm
    private val keycloakUrl = "$baseUrl/realms/$realm/protocol/openid-connect"
    private val deviceGrantType = "urn:ietf:params:oauth:grant-type:device_code"
    private val scope = "openid offline_access"
    val objectMapper = jacksonObjectMapper()

    fun requestDeviceCode(): DeviceCodeResponse {
        try {
            val client = HttpClients.createDefault()
            val httpPost = HttpPost("$keycloakUrl/auth/device")

            val params =
                listOf(
                    BasicNameValuePair("client_id", clientId),
                    BasicNameValuePair("scope", "openid"),
                )
            httpPost.entity = UrlEncodedFormEntity(params)

            client.execute(httpPost).use { response ->
                val json = EntityUtils.toString(response.entity)
                if (response.statusLine.statusCode != 200) {
                    throw CloudRestCallException("Error: ${response.statusLine.statusCode} - $json")
                }
                return objectMapper.readValue(json)
            }
        } catch (ex: Exception) {
            throw CloudRestCallException("${ex.message ?: ex.cause?.message}")
        }
    }

    fun requestToken(deviceCode: DeviceCodeResponse): TokenResponse {
        try {
            val client = HttpClients.createDefault()
            val httpPost = HttpPost("$keycloakUrl/token")

            val params =
                listOf(
                    BasicNameValuePair("client_id", clientId),
                    BasicNameValuePair("grant_type", deviceGrantType),
                    BasicNameValuePair("device_code", deviceCode.deviceCode),
                    BasicNameValuePair("client_secret", clientSecret),
                    BasicNameValuePair("scope", scope),
                )
            httpPost.entity = UrlEncodedFormEntity(params)
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded")

            client.execute(httpPost).use { response ->
                val json = EntityUtils.toString(response.entity)

                if (response.statusLine.statusCode == 200) {
                    return objectMapper.readValue(json)
                } else {
                    val node = objectMapper.readTree(json)
                    when (node["error"]?.asText()?.lowercase()) {
                        "authorization_pending" -> throw CloudAuthorizationPendingException()
                        "slow_down" -> throw CloudSlowDownException()
                        else -> {
                            val message = node["error_description"]?.asText() ?: "Authorization failed"
                            throw CloudCommandException(message)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            throw CloudRestCallException("${ex.message ?: ex.cause?.message}")
        }
    }
}
