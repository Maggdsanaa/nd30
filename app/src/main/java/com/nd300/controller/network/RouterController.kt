package com.nd300.controller.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * RouterController: طبقة الاتصال الفعلية بلوحة إدارة TOTOLINK ND300.
 *
 * كل عملية (login / getStatus / enableInternet / disableInternet / reboot) تُبنى من
 * RequestTemplate الذي يوفّره المستخدم في شاشة الإعداد (تم التقاطه فعلياً من طلبات
 * متصفحه أثناء استخدام لوحة الإدارة الحقيقية). لا توجد أي مسارات أو أوامر مُخترعة هنا.
 *
 * الجلسة (Cookies) تُحفظ في الذاكرة طوال عمر هذا الكائن فقط، ولا تُكتب على القرص.
 */
class RouterController(private val config: RouterConfig) {

    private val cookieStore = mutableMapOf<String, List<Cookie>>()
    private var capturedToken: String = ""

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                cookieStore[url.host] ?: emptyList()
        })
        .build()

    /** فحص بسيط لإمكانية الوصول إلى عنوان المودم قبل أي شيء آخر. */
    fun connect(): RouterActionResult {
        return try {
            val request = Request.Builder().url(config.baseUrl + "/").get().build()
            client.newCall(request).execute().use { resp ->
                RouterActionResult.Success(resp.code, "تم الوصول إلى ${config.baseUrl}")
            }
        } catch (e: IOException) {
            RouterActionResult.NetworkError(e.message ?: "تعذر الوصول للمودم")
        }
    }

    fun login(): RouterActionResult {
        val tpl = config.templates.login
        if (tpl.path.isBlank()) return RouterActionResult.NotConfigured("تسجيل الدخول")
        val result = execute(tpl)
        if (result is RouterActionResult.Success && tpl.extractTokenRegex.isNotBlank()) {
            val regex = Regex(tpl.extractTokenRegex)
            regex.find(result.bodySnippet)?.let { match ->
                capturedToken = match.groupValues.getOrElse(1) { "" }
            }
        }
        return when (result) {
            is RouterActionResult.Success -> {
                if (tpl.successContains.isNotBlank() && !result.bodySnippet.contains(tpl.successContains)) {
                    RouterActionResult.AuthFailed("لم يتم العثور على مؤشر نجاح تسجيل الدخول في الاستجابة")
                } else result
            }
            else -> result
        }
    }

    fun getStatus(): RouterActionResult {
        val tpl = config.templates.getStatus
        if (tpl.path.isBlank()) return RouterActionResult.NotConfigured("حالة الاتصال")
        return execute(tpl)
    }

    fun enableInternet(): RouterActionResult = runAuthenticated(config.templates.enableInternet, "تشغيل الإنترنت")

    fun disableInternet(): RouterActionResult = runAuthenticated(config.templates.disableInternet, "إيقاف الإنترنت")

    fun reboot(): RouterActionResult = runAuthenticated(config.templates.reboot, "إعادة التشغيل")

    fun disconnect() {
        cookieStore.clear()
        capturedToken = ""
    }

    private fun runAuthenticated(tpl: RequestTemplate, label: String): RouterActionResult {
        if (tpl.path.isBlank()) return RouterActionResult.NotConfigured(label)
        // سجّل الدخول أولاً لضمان وجود جلسة صالحة (يدعم كلاً من مصادقة الكوكيز والتوكن)
        if (config.templates.login.path.isNotBlank()) {
            val loginResult = login()
            if (loginResult is RouterActionResult.AuthFailed || loginResult is RouterActionResult.NetworkError) {
                return loginResult
            }
        }
        return execute(tpl)
    }

    private fun substitute(input: String): String {
        return input
            .replace("{ip}", config.ip)
            .replace("{port}", config.port)
            .replace("{username}", config.username)
            .replace("{password}", config.password)
            .replace("{token}", capturedToken)
    }

    private fun execute(tpl: RequestTemplate): RouterActionResult {
        return try {
            val fullPath = substitute(tpl.path)
            val url = if (fullPath.startsWith("http")) fullPath else config.baseUrl + fullPath
            val builder = Request.Builder().url(url)

            tpl.extraHeaders.forEach { (k, v) -> builder.addHeader(k, substitute(v)) }

            when (tpl.method.uppercase()) {
                "GET" -> builder.get()
                "POST" -> {
                    val body = substitute(tpl.bodyTemplate)
                        .toRequestBody(tpl.contentType.toMediaTypeOrNull())
                    builder.post(body)
                }
                else -> builder.get()
            }

            client.newCall(builder.build()).execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                val snippet = if (bodyText.length > 500) bodyText.substring(0, 500) else bodyText

                if (!resp.isSuccessful) {
                    return RouterActionResult.UnexpectedResponse(resp.code, snippet)
                }
                if (tpl.successContains.isNotBlank() && !bodyText.contains(tpl.successContains)) {
                    return RouterActionResult.UnexpectedResponse(resp.code, snippet)
                }
                RouterActionResult.Success(resp.code, snippet)
            }
        } catch (e: IOException) {
            RouterActionResult.NetworkError(e.message ?: "خطأ شبكة غير معروف")
        }
    }
}
