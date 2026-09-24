package com.nd300.controller.network

import kotlinx.serialization.Serializable

/**
 * قالب طلب HTTP واحد فعلي كما يرسله متصفحك بالضبط للوحة إدارة المودم.
 *
 * لماذا هذا التصميم؟
 * لا يوجد توثيق رسمي موحّد وموثوق لواجهة برمجة TOTOLINK ND300 تحديداً (الإصدارات/الطرازات
 * المختلفة من نفس العائلة تستخدم واجهات ويب مختلفة: بعضها admin.cgi تقليدي بمصادقة Basic/Session،
 * وبعض طرازات TOTOLINK الأحدث تستخدم /cgi-bin/cstecgi.cgi بنمط action=xxx&token=yyy).
 * لذلك بدل افتراض endpoint غير مؤكد لجهازك بالتحديد، يقوم RouterController بتنفيذ
 * "القالب" الذي تحدده أنت بعد التقاطه فعلياً من طلبات متصفحك (راجع دليل الالتقاط في README).
 *
 * المتغيرات المتاحة للاستبدال داخل path و body و headers:
 *   {ip} {port} {username} {password} {token} {sessionCookie}
 */
@Serializable
data class RequestTemplate(
    val method: String = "POST",              // GET أو POST
    val path: String = "",                     // مثال: /cgi-bin/cstecgi.cgi?token={token}
    val contentType: String = "application/x-www-form-urlencoded; charset=UTF-8",
    val bodyTemplate: String = "",              // مثال: action=setWiFiCfg&wifiEnable=0
    val extraHeaders: Map<String, String> = emptyMap(),
    // نمط نجاح بسيط: نص متوقع وجوده في الاستجابة (أو HTTP 200 فقط إذا تُرك فارغاً)
    val successContains: String = "",
    // في حال كانت الاستجابة تحتوي على token يجب استخدامه في الطلبات اللاحقة (JSON بسيط)
    val extractTokenRegex: String = ""
)

@Serializable
data class RouterTemplateSet(
    val login: RequestTemplate = RequestTemplate(),
    val getStatus: RequestTemplate = RequestTemplate(),
    val enableInternet: RequestTemplate = RequestTemplate(),
    val disableInternet: RequestTemplate = RequestTemplate(),
    val reboot: RequestTemplate = RequestTemplate(),
    /** ما الذي يوقفه فعلياً "إيقاف الإنترنت"؟ يحدَّد بوضوح للمستخدم في واجهة الإعداد. */
    val disableInternetMeans: String = "WAN" // "WAN" أو "WIFI" حسب ما توفره الفريموير فعلياً
)
