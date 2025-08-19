package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.CloudAuthorizationPendingException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.model.DeviceCodeResponse
import com.noumenadigital.npl.cli.model.TokenResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class FakeNoumenaCloudAuthClient(
    var tokenResponses: List<Any> = listOf(),
    var deviceCodeResponse: DeviceCodeResponse? = null,
) : NoumenaCloudAuthClient(
        config =
            NoumenaCloudAuthConfig(
                clientId = "test-client",
                clientSecret = "test-client-secret",
                url = "http://localhost:8080",
            ),
    ) {
    private var tokenCallCount = 0

    override fun requestDeviceCode(): DeviceCodeResponse = deviceCodeResponse ?: error("No device code response set")

    override fun requestToken(deviceCode: DeviceCodeResponse): TokenResponse {
        val resp = tokenResponses.getOrNull(tokenCallCount++)
        return when (resp) {
            is TokenResponse -> resp
            is Exception -> throw resp
            else -> error("No token response set for call $tokenCallCount")
        }
    }
}

class CloudAuthManagerTest :
    FunSpec({
        System.setProperty("NPL_CLI_BROWSER_DISABLED", "true")

        data class TestContext(
            val tokenResponses: List<Any> = listOf(),
            val deviceCodeResponse: DeviceCodeResponse =
                DeviceCodeResponse(
                    deviceCode = "dev-code",
                    userCode = "user-code",
                    verificationUri = "http://verify",
                    verificationUriComplete = "http://verify/complete",
                    interval = 1,
                    expiresIn = 2,
                ),
            val writer: StringWriter = StringWriter(),
            val colorWriter: ColorWriter = ColorWriter(writer, useColor = false),
            val fakeClient: FakeNoumenaCloudAuthClient = FakeNoumenaCloudAuthClient(tokenResponses, deviceCodeResponse),
            val manager: CloudAuthManager = CloudAuthManager(fakeClient),
        )

        fun withTestContext(test: TestContext.() -> Unit) {
            TestContext().apply(test)
        }

        test("login should write user code and save token on first try") {
            withTestContext {
                fakeClient.tokenResponses =
                    listOf(
                        TokenResponse(refreshToken = "refresh-token", accessToken = "access-token"),
                    )
                runBlocking { manager.login(colorWriter) }
                writer.toString().contains("http://verify/complete") shouldBe true
            }
        }

        test("pollForToken retries on CloudAuthorizationPendingException and then succeeds") {
            withTestContext {
                fakeClient.tokenResponses =
                    listOf(
                        CloudAuthorizationPendingException(),
                        TokenResponse(refreshToken = "refresh-token", accessToken = "access-token"),
                    )
                runBlocking { manager.login(colorWriter) }
                writer.toString().contains("http://verify/complete") shouldBe true
            }
        }

        test("pollForToken throws on CloudCommandException") {
            withTestContext {
                fakeClient.tokenResponses = listOf(RuntimeException("fail"))
                shouldThrow<CloudCommandException> {
                    runBlocking { manager.login(colorWriter) }
                }
            }
        }

        test("getServiceAccountAccessToken returns access token from client credentials without storing") {
            val stubClient = object : NoumenaCloudAuthClient(NoumenaCloudAuthConfig("id","secret","http://localhost:8080")) {
                override fun getAccessTokenByClientCredentials(serviceClientId: String, serviceClientSecret: String): TokenResponse {
                    serviceClientId shouldBe "svc-id"
                    serviceClientSecret shouldBe "svc-secret"
                    return TokenResponse(refreshToken = null, accessToken = "svc-access")
                }
            }
            val manager = CloudAuthManager(stubClient)
            val token = manager.getServiceAccountAccessToken("svc-id", "svc-secret")
            token shouldBe "svc-access"
        }

        test("getServiceAccountAccessToken throws when access token is missing") {
            val stubClient = object : NoumenaCloudAuthClient(NoumenaCloudAuthConfig("id","secret","http://localhost:8080")) {
                override fun getAccessTokenByClientCredentials(serviceClientId: String, serviceClientSecret: String): TokenResponse {
                    return TokenResponse(refreshToken = null, accessToken = null)
                }
            }
            val manager = CloudAuthManager(stubClient)
            shouldThrow<CloudCommandException> {
                manager.getServiceAccountAccessToken("svc-id", "svc-secret")
            }
        }
    })
