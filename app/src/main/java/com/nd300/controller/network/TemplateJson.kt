package com.nd300.controller.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TemplateJson {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun encode(set: RouterTemplateSet): String = json.encodeToString(set)

    fun decode(raw: String): RouterTemplateSet =
        if (raw.isBlank()) RouterTemplateSet()
        else try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            RouterTemplateSet()
        }
}
