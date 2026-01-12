package com.noumenadigital.npl.cli.http

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class NoumenaGitRepoClient {
    private val client = HttpClients.createDefault()

    fun getTemplateUrl(template: Template) = "${GIT_REPO_URL}${template.repo}/archive/refs/heads/${template.branch}.zip"

    fun downloadTemplateArchive(
        url: String,
        archive: File,
    ) {
        val request = HttpGet(URI.create(url))

        client.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode == 200) {
                FileOutputStream(archive).use { output ->
                    response.entity.content.copyTo(output)
                }
            } else {
                error("Failed to retrieve project template")
            }
        }
    }

    companion object {
        enum class Template(
            val repo: String,
            val branch: String,
        ) {
            SAMPLES("npl-init", "samples"),
            NO_SAMPLES("npl-init", "no-samples"),
            FRONTEND("npl-frontend-starter", "master"),
        }

        private const val GIT_REPO_URL = "https://github.com/NoumenaDigital/"
    }
}
