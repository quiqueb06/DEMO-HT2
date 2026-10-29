package com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash

import android.util.Log

private const val TAG = "AlarmCrashReporter"

/**
 * Logcat-backed implementation of [CrashReporter].
 * In production, replace with a Crashlytics delegate:
 *   FirebaseCrashlytics.getInstance().recordException(throwable)
 */
class LogCrashReporter : CrashReporter {

    override fun report(throwable: Throwable) {
        Log.e(TAG, "Unhandled exception: ${throwable.message}", throwable)
    }

    override fun report(message: String) {
        Log.e(TAG, "Error: $message")
    }

    override fun log(message: String) {
        Log.d(TAG, message)
    }
}
