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

    fun downloadBranchArchive(
        branch: String,
        archive: File,
    ) {
        val url = "$GIT_REPO_URL$repo/archive/refs/heads/$branch.zip"
        downloadArchive(url, archive)
    }

    fun downloadTestArchive(
        url: String,
        archive: File,
    ) {
        validateTestUrl(url)
        downloadArchive(url, archive)
    }

    private fun downloadArchive(
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
                error("Failed to retrieve project files. Status returned: $statusCode")
            }
        }
    }

    private fun validateTestUrl(url: String) {
        val host = URI.create(url).host

        require(host == "localhost" || host == "127.0.0.1" || host == "::1") {
            "Only localhost or equivalent allowed for --test-url (was '$host')"
        }
    }

    companion object {
        private val GIT_REPO_URL = "https://github.com/NoumenaDigital/"
    }
}
