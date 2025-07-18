package com.noumenadigital.npl.cli.http

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.exception.CloudRestCallException
import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.Tenant
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets
import java.util.UUID

data class NoumenaCloudConfig(
    val appSlug: String = "",
    val tenantSlug: String = "",
    val url: String = "https://portal.noumena.cloud",
) {
    companion object {
        fun get(
            appSlug: String,
            tenantSlug: String,
            url: String? = null,
        ): NoumenaCloudConfig =
            NoumenaCloudConfig(
                appSlug = appSlug,
                tenantSlug = tenantSlug,
                url = url ?: "https://portal.noumena.cloud",
            )
    }
}

open class NoumenaCloudClient(
    val config: NoumenaCloudConfig,
) {
    val ncBaseUrl = "${config.url}/api/v1/applications/"
    private val tenantsUrl = "${config.url}/api/v1/tenants"
    private val client = HttpClients.createDefault()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun fetchTenants(accessToken: String): List<Tenant> {
        try {
            val httpGet = HttpGet(tenantsUrl)
            httpGet.setHeader("Accept", "application/json")
            httpGet.setHeader("Authorization", "Bearer $accessToken")

            client.execute(httpGet).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""

                if (status !in 200..299) {
                    throw CloudRestCallException("Get tenants request failed $status: $responseText")
                }

                return objectMapper.readValue(responseText, object : TypeReference<List<Tenant>>() {})
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to fetch tenants - ${e.message ?: e.cause?.message}.", e)
        }
    }

    fun uploadApplicationArchive(
        accessToken: String,
        archive: ByteArray,
        tenants: List<Tenant>,
    ) {
        try {
            val ncApp = findApplication(tenants)
            if (ncApp == null) {
                throw CloudRestCallException(buildNotFoundErrorMessage(tenants))
            }
            val deployUrl = "$ncBaseUrl/${ncApp.id}/deploy"
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

    fun clearApplication(
        token: String,
        tenants: List<Tenant>,
    ) {
        try {
            val ncApp = findApplication(tenants)
            if (ncApp == null) {
                throw CloudRestCallException(buildNotFoundErrorMessage(tenants))
            }
            val clearUrl = "$ncBaseUrl/${ncApp.id}/clear"
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

    private fun buildNotFoundErrorMessage(tenants: List<Tenant>): String {
        val errorMessage = StringBuilder()
        errorMessage.append("Application slug '${config.appSlug}' doesn't exist for tenant slug '${config.tenantSlug}'.\n\n")
        
        if (tenants.isEmpty()) {
            errorMessage.append("No tenants are available.")
        } else {
            errorMessage.append("Available tenants and applications:\n\n")
            tenants.forEach { tenant ->
                errorMessage.append("Tenant: ${tenant.name} (slug: ${tenant.slug})\n")
                if (tenant.applications.isEmpty()) {
                    errorMessage.append("  No applications available\n")
                } else {
                    tenant.applications.forEach { app ->
                        errorMessage.append("  Application: ${app.name} (slug: ${app.slug})\n")
                    }
                }
                errorMessage.append("\n")
            }
        }
        
        return errorMessage.toString().trim()
    }

    private fun findApplication(tenants: List<Tenant>): Application? =
        tenants
            .find { it.slug.equals(config.tenantSlug, ignoreCase = true) }
            ?.applications
            ?.find { it.slug.equals(config.appSlug, ignoreCase = true) }
}
