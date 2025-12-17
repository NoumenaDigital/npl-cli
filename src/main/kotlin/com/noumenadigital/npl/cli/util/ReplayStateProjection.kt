package com.noumenadigital.npl.cli.service

object ReplayStateProjection {

    fun fromRestState(rest: Map<String, Any?>): Map<String, Any?> {
        val state = rest["@state"] ?: rest["state"]

        val slots = linkedMapOf<String, Any?>()

        // protocol fields (non-@ keys)
        for ((k, v) in rest) {
            if (!k.startsWith("@")) {
                slots[k] = v
            }
        }

        // parties as slots
        val parties = rest["@parties"]
        if (parties is Map<*, *>) {
            for ((k, v) in parties) {
                if (k != null) slots[k.toString()] = v
            }
        }

        // observers as slots (if present)
        val observers = rest["@observers"]
        if (observers is Map<*, *>) {
            for ((k, v) in observers) {
                if (k != null) slots[k.toString()] = v
            }
        }

        // IMPORTANT: backend frame.slots contains currentState; mirror that
        slots["currentState"] = state

        return linkedMapOf(
            "state" to state,
            "slots" to slots,
        )
    }
}
