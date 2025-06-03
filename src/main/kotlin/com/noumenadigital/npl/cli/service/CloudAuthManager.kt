package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CloudSlowDownException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import com.noumenadigital.npl.cli.util.IOUtils
import java.nio.file.Path
import kotlin.io.path.Path

class CloudAuthManager(
    val noumenaCloudClient: NoumenaCloudClient,
    val jsonFilePath: Path = Path(System.getProperty("user.home"), ".noumena", "npl.json"),
) {
    fun login(output: ColorWriter) {
        val deviceCode = noumenaCloudClient.requestDeviceCode()
        openBrowser(deviceCode.verificationUri, output)
        val token = pollForToken(deviceCode, output)
        IOUtils.writeObjectToFile(jsonFilePath.toFile(), token)
    }

    fun pollForToken(
        deviceCode: DeviceCodeResponse,
        output: ColorWriter,
    ): TokenResponse {
        val intervalMillis = deviceCode.interval * 1000L
        var currentInterval = intervalMillis
        val maxExpiry = System.currentTimeMillis() + deviceCode.expiresIn * 1000L

        while (true) {
            try {
                return noumenaCloudClient.requestToken(deviceCode) // return if successful
            } catch (e: CloudAuthorizationPendingException) {
                // continue polling
            } catch (e: CloudSlowDownException) {
                currentInterval += 1000
            } catch (e: RuntimeException) {
                throw CloudCommandException(e.message, e)
            }

            if (System.currentTimeMillis() > maxExpiry) {
                throw CloudCommandException("Device flow expired, please try again")
            }

            Thread.sleep(currentInterval)
        }
    }

    private fun openBrowser(
        url: String,
        output: ColorWriter,
    ) {
        if (shouldOpenBrowser()) {
            if (tryOpenBrowser(url)) {
                output.info("Opened browser to: $url")
                return
            }
        }
        output.info("Please open the following URL in your browser: $url")
    }

    private fun shouldOpenBrowser(): Boolean =
        (
            System.getProperty("NPL_CLI_BROWSER_DISABLED")?.lowercase() ?: System
                .getenv("NPL_CLI_BROWSER_DISABLED")
                ?.lowercase()
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
