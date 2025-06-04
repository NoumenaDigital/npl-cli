package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CloudSlowDownException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import com.noumenadigital.npl.cli.util.IOUtils
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.io.path.Path

class CloudAuthManager(
    private val noumenaCloudClient: NoumenaCloudClient,
    private val jsonFilePath: Path = Path(System.getProperty("user.home"), ".noumena", "noumena.yaml"),
) {
    suspend fun login(output: ColorWriter) {
        val deviceCode = noumenaCloudClient.requestDeviceCode()
        openBrowser(deviceCode.verificationUri, output)
        output.info("Please use the following code to complete authentication in your browser: ${deviceCode.userCode}")
        val token = pollForToken(deviceCode)
        IOUtils.writeObjectToFile(jsonFilePath.toFile(), token)
    }

    private suspend fun pollForToken(deviceCode: DeviceCodeResponse): TokenResponse {
        val baseInterval = deviceCode.interval * 1_000L
        var currentInterval = baseInterval
        val maxExpiry = System.currentTimeMillis() + deviceCode.expiresIn * 1_000L

        while (true) {
            try {
                return noumenaCloudClient.requestToken(deviceCode)
            } catch (e: CloudAuthorizationPendingException) {
            } catch (e: CloudSlowDownException) {
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

    private fun openBrowser(
        url: String,
        output: ColorWriter,
    ) {
        if (shouldOpenBrowser()) {
            if (tryOpenBrowser(url)) {
                output.info("Opened browser to: $url")
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
        return try {
            when {
                os.contains("mac") -> ProcessBuilder("open", url).start()
                os.contains("win") -> ProcessBuilder("cmd", "/c", "start", url).start()
                else -> ProcessBuilder("xdg-open", url).start()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
