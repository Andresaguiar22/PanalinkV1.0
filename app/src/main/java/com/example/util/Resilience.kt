package com.example.util

import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

object Resilience {
    private const val TAG = "Resilience"

    /**
     * Executes a block with a standard retry policy.
     * Use for network calls or database operations that might fail due to transient issues.
     */
    suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 5000L,
        factor: Double = 2.0,
        retryCondition: (Throwable) -> Boolean = { it is IOException },
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (!retryCondition(e)) throw e

                // Sin conectividad no hay nada que reintentar: el backoff solo congela
                // la app en tormentas de llamadas fallidas. Fallar rápido permite a los
                // repositorios servir el caché local de inmediato.
                if (!NetworkMonitor.isOnline.value) {
                    Log.w(TAG, "Operation failed and device is offline. Failing fast to serve local cache. Error: ${e.message}")
                    throw e
                }

                Log.w(TAG, "Operation failed (attempt ${attempt + 1}), retrying in $currentDelay ms... Error: ${e.message}")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // Last attempt
    }

    /**
     * Executes a block safely, catching any exception and returning a default value or null.
     */
    inline fun <T> safe(tag: String = "Resilience", message: String = "Safe execution caught an error", block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(tag, "$message: ${e.message}", e)
            null
        }
    }

    /**
     * Standard CoroutineExceptionHandler that logs and prevents app crash in a scope.
     */
    fun globalExceptionHandler(tag: String = TAG) = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Uncaught Coroutine Exception: ${throwable.message}", throwable)
    }
}
