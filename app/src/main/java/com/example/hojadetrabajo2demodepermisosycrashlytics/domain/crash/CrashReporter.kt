package com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash

/**
 * Generic interface for reporting errors and logging diagnostic messages.
 * Implementations can delegate to Crashlytics, Logcat, or any other backend.
 * ViewModels receive this via constructor injection so the data layer never
 * depends on a concrete crash-reporting SDK.
 */
interface CrashReporter {
    /** Reports a caught exception with full stack trace. */
    fun report(throwable: Throwable)

    /** Reports a descriptive error message (no stack trace available). */
    fun report(message: String)

    /** Logs a non-critical diagnostic breadcrumb. */
    fun log(message: String)
}
