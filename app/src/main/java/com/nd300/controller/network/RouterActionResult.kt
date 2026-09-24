package com.nd300.controller.network

sealed class RouterActionResult {
    data class Success(val httpCode: Int, val bodySnippet: String) : RouterActionResult()
    data class AuthFailed(val detail: String) : RouterActionResult()
    data class NetworkError(val detail: String) : RouterActionResult()
    data class UnexpectedResponse(val httpCode: Int, val bodySnippet: String) : RouterActionResult()
    data class NotConfigured(val whichTemplate: String) : RouterActionResult()

    val isSuccess: Boolean get() = this is Success

    fun failureReasonOrNull(): String? = when (this) {
        is Success -> null
        is AuthFailed -> "فشل تسجيل الدخول: $detail"
        is NetworkError -> "خطأ في الشبكة: $detail"
        is UnexpectedResponse -> "استجابة غير متوقعة (HTTP $httpCode)"
        is NotConfigured -> "لم يتم إعداد قالب الطلب لـ: $whichTemplate"
    }
}
