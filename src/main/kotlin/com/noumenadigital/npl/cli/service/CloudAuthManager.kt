package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CloudSlowDownException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import com.noumenadigital.npl.cli.util.IOUtils
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

class CloudAuthManager(
    val noumenaCloudClient: NoumenaCloudClient,
    val jsonFilePath: Path = Path(System.getProperty("user.home"), ".noumena", ".npl.json"),
) {
    fun login(output: ColorWriter) {
        val deviceCode = noumenaCloudClient.requestDeviceCode()
        output.info("Please complete authentication in your browser: ${deviceCode.verificationUri}")
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(deviceCode.verificationUri))
        } else {
            output.info(("Please open the following URL in your browser: ${deviceCode.verificationUri}"))
        }
        val token = pollForToken(deviceCode, output)
        IOUtils.writeObjectToFile(jsonFilePath.toFile(), token)
    }

    fun pollForToken(
        deviceCode: DeviceCodeResponse,
        output: ColorWriter,
    ): TokenResponse {
        val intervalMillis = deviceCode.interval * 1000L
        var currentInterval = intervalMillis
        val maxExpiry = System.currentTimeMillis() + deviceCode.expiresIn * 10L

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
}
