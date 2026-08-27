package com.daaass142.smsstrongalert

data class AlertPayload(
    val body: String,
    val sender: String,
    val plate: String?,
    val receivedAtMillis: Long,
)
