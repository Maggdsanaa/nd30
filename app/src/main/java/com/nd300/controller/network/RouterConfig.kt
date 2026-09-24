package com.nd300.controller.network

data class RouterConfig(
    val ip: String,
    val port: String, // فارغ = 80
    val username: String,
    val password: String,
    val templates: RouterTemplateSet
) {
    val baseUrl: String
        get() {
            val portPart = if (port.isBlank()) "" else ":$port"
            return "http://$ip$portPart"
        }
}
