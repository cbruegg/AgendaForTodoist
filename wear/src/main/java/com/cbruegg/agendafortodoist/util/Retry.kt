package com.cbruegg.agendafortodoist.util

import kotlinx.coroutines.experimental.delay

suspend fun <T> retry(vararg handleTypes: Class<out Exception>, initialBackoffMs: Long = 500L, maxTries: Int = 3, f: suspend () -> T): T {
    require(maxTries > 0) { "You must allow at least one try." }

    var backoffMs = initialBackoffMs
    var lastException: Throwable = AssertionError("You should never see this.")
    repeat(maxTries) {
        try {
            return f()
        } catch (e: Exception) {
            if (handleTypes.any { it.isAssignableFrom(e.javaClass) }) {
                e.printStackTrace()
                lastException = e
                delay(backoffMs)
                backoffMs *= 2
            } else {
                throw e
            }
        }
    }
    throw lastException
}