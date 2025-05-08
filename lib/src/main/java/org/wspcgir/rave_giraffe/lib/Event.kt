package org.wspcgir.rave_giraffe.lib

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val MESSAGE_PATH = "/set_event_data"
val MESSAGE_CHARSET = Charset.forName("UTF-8")

@Serializable
data class Event(
    val stageName: String,
    val artistName: String,
    @Contextual
    val startTime: LocalDateTime
)

val dtFormat = DateTimeFormatter.ofPattern("y:m:d H:M")

@Serializable
data class EventJson(
    val stageName: String,
    val artistName: String,
    val startTime: String
)

