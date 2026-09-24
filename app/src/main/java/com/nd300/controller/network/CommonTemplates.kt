package com.nd300.controller.network

/**
 * قوالب مرجعية "نقطة انطلاق" فقط — غير مؤكدة لطراز ND300 تحديداً.
 *
 * هذه ليست أوامر مخترعة بلا مصدر: نمط cstecgi.cgi (action=xxx مع معامل token)
 * هو نمط موثّق فعلياً في تحليلات علنية لواجهة الويب المستخدمة في عدة طرازات TOTOLINK
 * (مثل A3300R وX5000R وX6000R)، لكن لا يوجد تأكيد بأن ND300 (وهو مودم ADSL أقدم)
 * يستخدم نفس الواجهة بالضبط. لذلك:
 *   1) هذه القوالب مُعطّلة افتراضياً (لا تُستخدم تلقائياً).
 *   2) يجب على المستخدم تحميلها يدوياً من شاشة الإعداد كنقطة بداية فقط،
 *      ثم التحقق منها عبر "اختبار الاتصال"، وتعديلها حسب الحاجة الفعلية بعد
 *      التقاط الطلب الحقيقي من متصفحه (راجع دليل الالتقاط في README).
 *   3) إذا لم تنجح هذه القوالب، فالطريقة الموثوقة الوحيدة هي الالتقاط اليدوي.
 */
object CommonTemplates {

    /** نمط شائع في بعض طرازات TOTOLINK الأحدث: cgi-bin/cstecgi.cgi + token. غير مؤكد لـ ND300. */
    val cstecgiStyle = RouterTemplateSet(
        login = RequestTemplate(
            method = "POST",
            path = "/cgi-bin/cstecgi.cgi?action=login",
            bodyTemplate = "action=login&user={username}&password={password}",
            successContains = "\"succ\"", // مثال فقط — عدّله بعد الالتقاط الفعلي
            extractTokenRegex = "\"token\"\\s*:\\s*\"([A-Za-z0-9]+)\""
        ),
        getStatus = RequestTemplate(
            method = "GET",
            path = "/cgi-bin/cstecgi.cgi?action=getWanCfg&token={token}"
        ),
        disableInternet = RequestTemplate(
            method = "POST",
            path = "/cgi-bin/cstecgi.cgi?action=setWanCfg&token={token}",
            bodyTemplate = "action=setWanCfg&wanEnable=0"
        ),
        enableInternet = RequestTemplate(
            method = "POST",
            path = "/cgi-bin/cstecgi.cgi?action=setWanCfg&token={token}",
            bodyTemplate = "action=setWanCfg&wanEnable=1"
        ),
        reboot = RequestTemplate(
            method = "POST",
            path = "/cgi-bin/cstecgi.cgi?action=reboot&token={token}",
            bodyTemplate = "action=reboot"
        ),
        disableInternetMeans = "WAN"
    )

    /** قالب فارغ تماماً — البداية الموصى بها: املأه بعد الالتقاط اليدوي فقط. */
    val blank = RouterTemplateSet()
}
