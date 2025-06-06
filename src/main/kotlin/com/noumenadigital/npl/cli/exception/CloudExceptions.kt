package com.noumenadigital.npl.cli.exception

open class CloudCommandException(
    override val message: String? = null,
    e: Exception? = null,
    val commandName: String? = null,
) : InternalException()

class CloudAuthorizationPendingException : CloudCommandException()

class CloudSlowDownException : CloudCommandException()

class CloudRestCallException(
    override val message: String?,
    val e: Exception? = null,
) : CloudCommandException()
