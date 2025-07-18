# Tenant/Application Not Found Enhancement

## Summary

This enhancement improves the user experience when a tenant/application combination specified by the user is not found. Instead of showing a simple "not found" error, the system now provides a helpful list of available tenants and applications that the user can actually use.

## Implementation Details

### Changes Made

1. **Modified `NoumenaCloudClient.kt`**:
   - Added a new private method `buildNotFoundErrorMessage(tenants: List<Tenant>): String`
   - Updated `uploadApplicationArchive()` method to use the new error message
   - Updated `clearApplication()` method to use the new error message

### New Error Message Format

**Before:**
```
Application slug appName doesn't exist for tenant slug tenantName
```

**After:**
```
Application slug 'appName' doesn't exist for tenant slug 'tenantName'.

Available tenants and applications:

Tenant: Tenant One (slug: tenant-one)
  Application: App One (slug: app-one)
  Application: App Two (slug: app-two)

Tenant: Tenant Two (slug: tenant-two)
  Application: App Three (slug: app-three)
```

### Edge Cases Handled

1. **No tenants available**: Shows "No tenants are available."
2. **Tenant with no applications**: Shows "No applications available" under the tenant
3. **Multiple tenants with multiple applications**: Organized listing for easy scanning

### Code Changes

#### src/main/kotlin/com/noumenadigital/npl/cli/http/NoumenaCloudClient.kt

**New method added:**
```kotlin
private fun buildNotFoundErrorMessage(tenants: List<Tenant>): String {
    val errorMessage = StringBuilder()
    errorMessage.append("Application slug '${config.appSlug}' doesn't exist for tenant slug '${config.tenantSlug}'.\n\n")
    
    if (tenants.isEmpty()) {
        errorMessage.append("No tenants are available.")
    } else {
        errorMessage.append("Available tenants and applications:\n\n")
        tenants.forEach { tenant ->
            errorMessage.append("Tenant: ${tenant.name} (slug: ${tenant.slug})\n")
            if (tenant.applications.isEmpty()) {
                errorMessage.append("  No applications available\n")
            } else {
                tenant.applications.forEach { app ->
                    errorMessage.append("  Application: ${app.name} (slug: ${app.slug})\n")
                }
            }
            errorMessage.append("\n")
        }
    }
    
    return errorMessage.toString().trim()
}
```

**Updated method calls:**
- `uploadApplicationArchive()`: Line 77 - Uses `buildNotFoundErrorMessage(tenants)` instead of hardcoded message
- `clearApplication()`: Line 115 - Uses `buildNotFoundErrorMessage(tenants)` instead of hardcoded message

### Test Updates

Updated integration tests to expect the new error message format:
- `CloudDeployCommandIT.kt`: Updated expected output for "no application found" test
- `CloudClearCommandIT.kt`: Updated expected output for application not found test

### Benefits

1. **Improved User Experience**: Users can immediately see what options are available
2. **Reduced Support Load**: Self-service discovery of available tenants and applications
3. **Better Error Context**: Clear indication of what went wrong and what alternatives exist
4. **Consistent Formatting**: Professional, readable output format

### Compilation Status

✅ **Code compiles successfully** - The implementation has been verified to compile without errors using `mvn test-compile`.

The feature is ready for use and will provide users with helpful guidance when they specify tenant/application combinations that don't exist.