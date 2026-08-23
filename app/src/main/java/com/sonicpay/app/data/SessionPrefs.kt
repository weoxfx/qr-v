package com.sonicpay.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val vpa: String,
    val amountPaise: Long,
    val timestampMs: Long,
    val role: String
)

object SessionPrefs {
    private const val FILE = "sonicpay_session"
    private const val KEY_ROLE = "role"
    private const val KEY_MERCHANT_NAME = "merchant_name"
    private const val KEY_MERCHANT_VPA = "merchant_vpa"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY = 30

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    sealed class Role(val id: String) {
        object Merchant : Role("merchant")
        object Customer : Role("customer")
        companion object {
            fun fromId(id: String?): Role? = when (id) {
                "merchant" -> Merchant
                "customer" -> Customer
                else -> null
            }
        }
    }

    var savedRole: Role?
        get() = Role.fromId(prefs.getString(KEY_ROLE, null))
        private set(value) {
            prefs.edit().putString(KEY_ROLE, value?.id).apply()
        }

    fun chooseRole(role: Role) {
        savedRole = role
    }

    var merchantName: String
        get() = prefs.getString(KEY_MERCHANT_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MERCHANT_NAME, value).apply()

    var merchantVpa: String
        get() = prefs.getString(KEY_MERCHANT_VPA, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MERCHANT_VPA, value).apply()

    fun addHistory(entry: HistoryEntry) {
        val list = history.toMutableList()
        list.add(0, entry)
        while (list.size > MAX_HISTORY) list.removeAt(list.size - 1)
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("v", e.vpa)
                    .put("a", e.amountPaise)
                    .put("t", e.timestampMs)
                    .put("r", e.role)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    val history: List<HistoryEntry>
        get() {
            val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    HistoryEntry(
                        vpa = o.getString("v"),
                        amountPaise = o.getLong("a"),
                        timestampMs = o.getLong("t"),
                        role = o.optString("r", "customer")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
}
