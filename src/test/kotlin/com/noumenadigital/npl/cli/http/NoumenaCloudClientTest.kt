package com.noumenadigital.npl.cli.http

import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.Tenant
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NoumenaCloudClientTest : FunSpec({

    test("buildNotFoundErrorMessage shows available tenants and applications") {
        // Create test data
        val app1 = Application(id = "app1-id", name = "App One", slug = "app-one")
        val app2 = Application(id = "app2-id", name = "App Two", slug = "app-two")
        val tenant1 = Tenant(name = "Tenant One", slug = "tenant-one", applications = listOf(app1))
        val tenant2 = Tenant(name = "Tenant Two", slug = "tenant-two", applications = listOf(app2))
        val tenants = listOf(tenant1, tenant2)

        // Create client with test config
        val config = NoumenaCloudConfig("http://test.com", "nonexistent-app", "nonexistent-tenant")
        val client = NoumenaCloudClient(config)

        // Use reflection to call the private method
        val method = NoumenaCloudClient::class.java.getDeclaredMethod("buildNotFoundErrorMessage", List::class.java)
        method.isAccessible = true
        val result = method.invoke(client, tenants) as String

        // Verify the error message contains the expected content
        result shouldContain "Application slug 'nonexistent-app' doesn't exist for tenant slug 'nonexistent-tenant'"
        result shouldContain "Available tenants and applications:"
        result shouldContain "Tenant: Tenant One (slug: tenant-one)"
        result shouldContain "Application: App One (slug: app-one)"
        result shouldContain "Tenant: Tenant Two (slug: tenant-two)"
        result shouldContain "Application: App Two (slug: app-two)"
    }

    test("buildNotFoundErrorMessage handles empty tenants list") {
        val tenants = emptyList<Tenant>()

        val config = NoumenaCloudConfig("http://test.com", "nonexistent-app", "nonexistent-tenant")
        val client = NoumenaCloudClient(config)

        val method = NoumenaCloudClient::class.java.getDeclaredMethod("buildNotFoundErrorMessage", List::class.java)
        method.isAccessible = true
        val result = method.invoke(client, tenants) as String

        result shouldContain "Application slug 'nonexistent-app' doesn't exist for tenant slug 'nonexistent-tenant'"
        result shouldContain "No tenants are available."
    }

    test("buildNotFoundErrorMessage handles tenant with no applications") {
        val tenant = Tenant(name = "Empty Tenant", slug = "empty-tenant", applications = emptyList())
        val tenants = listOf(tenant)

        val config = NoumenaCloudConfig("http://test.com", "nonexistent-app", "nonexistent-tenant")
        val client = NoumenaCloudClient(config)

        val method = NoumenaCloudClient::class.java.getDeclaredMethod("buildNotFoundErrorMessage", List::class.java)
        method.isAccessible = true
        val result = method.invoke(client, tenants) as String

        result shouldContain "Tenant: Empty Tenant (slug: empty-tenant)"
        result shouldContain "No applications available"
    }
})