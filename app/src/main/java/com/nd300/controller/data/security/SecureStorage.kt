package com.nd300.controller.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * تخزين آمن لبيانات اعتماد المودم وقوالب الطلبات باستخدام Android Keystore
 * عبر EncryptedSharedPreferences (تشفير AES256-GCM للقيم و AES256-SIV للمفاتيح).
 *
 * لا تُطبع كلمة المرور أبداً في Log.* في أي مكان بالتطبيق.
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_router_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var routerIp: String
        get() = prefs.getString(KEY_IP, "192.168.1.1") ?: "192.168.1.1"
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var routerPort: String
        get() = prefs.getString(KEY_PORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PORT, value).apply()

    var username: String
        get() = prefs.getString(KEY_USER, "admin") ?: "admin"
        set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASS, value).apply()

    /** قوالب طلبات HTTP الفعلية (JSON) التي التقطها المستخدم من واجهة إدارة المودم. */
    var requestTemplatesJson: String
        get() = prefs.getString(KEY_TEMPLATES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TEMPLATES, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_IP = "router_ip"
        private const val KEY_PORT = "router_port"
        private const val KEY_USER = "router_username"
        private const val KEY_PASS = "router_password"
        private const val KEY_TEMPLATES = "request_templates_json"
    }
}
