package org.taxiapp.matching.dto.driver

data class EmailSendRequest(
    val recipient: String,
    val subject: String,
    val body: String,
)