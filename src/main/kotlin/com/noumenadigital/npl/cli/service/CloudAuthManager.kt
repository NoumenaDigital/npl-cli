package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CloudSlowDownException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import com.noumenadigital.npl.cli.util.IOUtils
import com.noumenadigital.npl.cli.util.IOUtils.readObjectFromFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path

class CloudAuthManager(
    private val noumenaCloudAuthClient: NoumenaCloudAuthClient = NoumenaCloudAuthClient(),
    private val noumenaConfigFilePath: Path = Path(System.getProperty("user.home"), ".noumena", "noumena.yaml"),
) {
    suspend fun login(output: ColorWriter) {
        val deviceCode = noumenaCloudAuthClient.requestDeviceCode()
        openBrowser(deviceCode.verificationUriComplete, output)
        val token = pollForToken(deviceCode)
        IOUtils.writeObjectToFile(noumenaConfigFilePath.toFile(), token)
    }

    fun loginBlocking(output: ColorWriter) {
        runBlocking { login(output) }
    }

    fun logout() {
        val emptyToken = TokenResponse(null)
        IOUtils.writeObjectToFile(noumenaConfigFilePath.toFile(), emptyToken)
    }

    private suspend fun pollForToken(deviceCode: DeviceCodeResponse): TokenResponse {
        val baseInterval = deviceCode.interval * 1_000L
        var currentInterval = baseInterval
        val maxExpiry = System.currentTimeMillis() + deviceCode.expiresIn * 1_000L

        while (true) {
            try {
                return noumenaCloudAuthClient.requestToken(deviceCode)
            } catch (_: CloudAuthorizationPendingException) {
            } catch (_: CloudSlowDownException) {
                currentInterval += 1000
            } catch (e: RuntimeException) {
                throw CloudCommandException(e.message, e)
            }

            if (System.currentTimeMillis() > maxExpiry) {
                throw CloudCommandException("Device flow expired, please try again.")
            }

            delay(currentInterval)
        }
    }

    fun getToken(): TokenResponse {
        if (!noumenaConfigFilePath.toFile().exists()) {
            throw AuthorizationFailedException("Please login again.")
        }
        val storedToken = readObjectFromFile<TokenResponse>(noumenaConfigFilePath.toFile())
        if (storedToken.refreshToken == null) {
            throw CloudCommandException("No refresh token found in the stored configuration. Please login again.")
        }
        val updatedToken = noumenaCloudAuthClient.getAccessTokenByRefreshToken(storedToken.refreshToken)
        IOUtils.writeObjectToFile(noumenaConfigFilePath.toFile(), updatedToken)
        return updatedToken
    }

    fun getServiceAccountAccessToken(
        clientId: String,
        clientSecret: String,
    ): String {
        val token =
            noumenaCloudAuthClient.getAccessTokenByClientCredentials(
                serviceClientId = clientId,
                serviceClientSecret = clientSecret,
            )
        val accessToken = token.accessToken
        if (accessToken.isNullOrBlank()) {
            throw CloudCommandException("Access token is not available for the provided service account credentials.")
        }
        return accessToken
    }

    private fun openBrowser(
        url: String,
        output: ColorWriter,
    ) {
        if (shouldOpenBrowser()) {
            output.info("Attempting to open $url in your browser...")
            if (tryOpenBrowser(url)) {
                output.info("Browser opened successfully.")
            } else {
                output.warning("Failed to open browser automatically. Please open the following URL manually: $url")
            }
        } else {
            output.info("Please open the following URL in your browser: $url")
        }
    }

    private fun shouldOpenBrowser(): Boolean =
        (
            System.getProperty("NPL_CLI_BROWSER_DISABLED")?.lowercase()
                ?: System.getenv("NPL_CLI_BROWSER_DISABLED")?.lowercase()
        ) != "true"

    private fun tryOpenBrowser(url: String): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> executeCommand("open", url)
            os.contains("win") -> executeCommand("cmd", "/c", "start", url)
            else -> executeCommand("xdg-open", url) || executeBrowserFallback(url)
        }
    }

    private fun executeCommand(vararg command: String): Boolean =
        try {
            val process = ProcessBuilder(*command).start()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }

    private fun executeBrowserFallback(url: String): Boolean {
        val browser = System.getenv("BROWSER") ?: return false
        return executeCommand(browser, url)
    }
}
