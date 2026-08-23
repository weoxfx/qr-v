package com.sonicpay.app.data

import android.content.Context
import android.util.Log

/**
 * Records uncaught exceptions so a crash can be inspected from inside the
 * app (Settings) — essential when there is no adb/logcat available.
 */
object CrashReporter {

    private const val FILE = "sonicpay_diagnostics"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY, Log.getStackTraceString(throwable))
                    .apply()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrash(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
