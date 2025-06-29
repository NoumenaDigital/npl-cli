package com.noumenadigital.npl.cli.http

import com.noumenadigital.npl.cli.exception.CloudRestCallException
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

data class NoumenaCloudConfig(
    val app: String = "",
    val url: String = "https://portal.noumena.cloud",
) {
    companion object {
        fun get(
            app: String,
            url: String? = null,
        ): NoumenaCloudConfig =
            NoumenaCloudConfig(
                app = app,
                url = url ?: "https://portal.noumena.cloud",
            )
    }
}

open class NoumenaCloudClient(
    config: NoumenaCloudConfig,
) {
    val ncBaseUrl =
        "${config.url}/api/v1/applications/${URLEncoder.encode(config.app, StandardCharsets.UTF_8.toString())}"
    private val deployUrl = "$ncBaseUrl/deploy"
    private val clearUrl = "$ncBaseUrl/clear"
    private val client = HttpClients.createDefault()

    fun uploadApplicationArchive(
        accessToken: String,
        archive: ByteArray,
    ) {
        try {
            val boundary = "----NoumenaBoundary" + UUID.randomUUID().toString().replace("-", "")

            val newline = "\r\n"

            val preamble =
                (
                    "--$boundary$newline" +
                        "Content-Disposition: form-data; name=\"npl_archive\"; filename=\"archive.zip\"$newline" +
                        "Content-Type: application/zip$newline$newline"
                ).toByteArray(StandardCharsets.UTF_8)

            val epilogue = "$newline--$boundary--$newline".toByteArray(StandardCharsets.UTF_8)

            val body = preamble + archive + epilogue

            val httpPost = HttpPost(deployUrl)
            httpPost.setHeader("Authorization", "Bearer $accessToken")
            httpPost.setHeader("Content-Type", "multipart/form-data; boundary=$boundary")
            httpPost.entity = ByteArrayEntity(body)

            client.execute(httpPost).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""
                if (status !in 200..299) {
                    throw CloudRestCallException("Deploy failed with status $status: $responseText")
                }
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to upload application archive - ${e.message ?: e.cause?.message}.", e)
        }
    }

    fun clearApplication(token: String) {
        try {
            val httpDelete = HttpDelete(clearUrl)
            httpDelete.setHeader("Accept", "application/json")
            httpDelete.setHeader("Authorization", "Bearer $token")

            client.execute(httpDelete).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""
                if (status !in 200..299) {
                    throw CloudRestCallException("Clear application failed with status $status: $responseText")
                }
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to remove the application -  ${e.message ?: e.cause?.message}.", e)
        }
    }
}
