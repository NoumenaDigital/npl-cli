package com.noumenadigital.npl.cli.http

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class NoumenaGitRepoClient(
    val repo: String = "npl-init",
) {
    private val client = HttpClients.createDefault()

    fun getBranchUrl(branch: SupportedBranches) = "$GIT_REPO_URL$repo/archive/refs/heads/${branch.branchName}.zip"

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
        enum class SupportedBranches(
            val branchName: String,
        ) {
            SAMPLES("samples"),
            NO_SAMPLES("no-samples"),
        }

        private const val GIT_REPO_URL = "https://github.com/NoumenaDigital/"
    }
}
