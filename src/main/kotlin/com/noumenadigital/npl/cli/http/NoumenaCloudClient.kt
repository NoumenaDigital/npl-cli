package com.noumenadigital.npl.cli.http

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.exception.CloudRestCallException
import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.CreateApplicationRequest
import com.noumenadigital.npl.cli.model.Tenant
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
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
    private val ncBaseUrl = "${config.url}/api/v1/applications/"
    private val tenantsUrl = "${config.url}/api/v1/tenants"
    private val engineVersionsUrl = "${config.url}/api/v1/engine/versions"
    private val slugUtilUrl = "${config.url}/api/v1/utils/slug"
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
            val ncApp =
                findApplication(tenants)
                    ?: throw CloudRestCallException("Application slug ${config.appSlug} doesn't exist for tenant slug ${config.tenantSlug}")
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
            val ncApp =
                findApplication(tenants)
                    ?: throw CloudRestCallException("Application slug ${config.appSlug} doesn't exist for tenant slug ${config.tenantSlug}")
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

    fun uploadFrontendArchive(
        accessToken: String,
        archive: ByteArray,
        tenants: List<Tenant>,
    ) {
        try {
            val ncApp =
                findApplication(tenants)
                    ?: throw CloudRestCallException("Application slug ${config.appSlug} doesn't exist for tenant ${config.tenantSlug}")
            val deployUrl = "$ncBaseUrl/${ncApp.id}/uploadwebsite"
            val boundary = "----NoumenaBoundary" + UUID.randomUUID().toString().replace("-", "")

            val newline = "\r\n"

            val preamble =
                (
                    "--$boundary$newline" +
                        "Content-Disposition: form-data; name=\"website_zip\"; filename=\"archive.zip\"$newline" +
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

    fun fetchEngineVersions(accessToken: String): List<String> {
        try {
            val httpGet = HttpGet(engineVersionsUrl)
            httpGet.setHeader("Accept", "application/json")
            httpGet.setHeader("Authorization", "Bearer $accessToken")

            client.execute(httpGet).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""

                if (status !in 200..299) {
                    throw CloudRestCallException("Get engine versions request failed $status: $responseText")
                }

                // API returns array of strings, not objects
                return objectMapper.readValue(responseText, object : TypeReference<List<String>>() {})
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to fetch engine versions - ${e.message ?: e.cause?.message}.", e)
        }
    }

    fun generateSlug(
        accessToken: String,
        text: String,
    ): String {
        try {
            val uri =
                URIBuilder(slugUtilUrl)
                    .addParameter("slug", text)
                    .build()
            val httpGet = HttpGet(uri)
            httpGet.setHeader("Accept", "application/json")
            httpGet.setHeader("Authorization", "Bearer $accessToken")

            client.execute(httpGet).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""

                if (status !in 200..299) {
                    throw CloudRestCallException("Generate slug request failed $status: $responseText")
                }

                // API returns a plain JSON string, not an object
                return objectMapper.readValue(responseText, String::class.java)
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to generate slug - ${e.message ?: e.cause?.message}.", e)
        }
    }

    fun createApplication(
        accessToken: String,
        tenantId: String,
        request: CreateApplicationRequest,
    ): Application {
        try {
            val createUrl = "$tenantsUrl/$tenantId/createApplication"
            val httpPost = HttpPost(createUrl)
            httpPost.setHeader("Accept", "application/json")
            httpPost.setHeader("Content-Type", "application/json")
            httpPost.setHeader("Authorization", "Bearer $accessToken")

            val requestBody = objectMapper.writeValueAsString(request)
            httpPost.entity = ByteArrayEntity(requestBody.toByteArray(StandardCharsets.UTF_8))

            client.execute(httpPost).use { response ->
                val status = response.statusLine.statusCode
                val responseText = response.entity?.let { EntityUtils.toString(it) } ?: ""

                if (status !in 200..299) {
                    throw CloudRestCallException("Create application request failed $status: $responseText")
                }

                return objectMapper.readValue(responseText, Application::class.java)
            }
        } catch (e: Exception) {
            throw CloudRestCallException("Failed to create application - ${e.message ?: e.cause?.message}.", e)
        }
    }

    private fun findApplication(tenants: List<Tenant>): Application? =
        tenants
            .find { it.slug.equals(config.tenantSlug, ignoreCase = true) }
            ?.applications
            ?.find { it.slug.equals(config.appSlug, ignoreCase = true) }
}
