package org.wspcgir.rave_giraffe.lib

import java.time.LocalDateTime

data class Event(
    val stageName: String,
    val artistName: String,
    val startTime: LocalDateTime
)

