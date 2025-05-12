package org.wspcgir.rave_giraffe.lib

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.io.InputStream
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

val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Serializable
data class EventJson(
    val stageName: String,
    val artistName: String,
    val startTime: String,
) {
    companion object {
        fun eventsFromString(input: InputStream): List<EventJson> {
            return csvReader().readAllWithHeader(input).mapNotNull { row ->
                val stageName = row["stageName"]
                val artistName = row["artistName"]
                val timeDay = row["timeDay"]
                val timeStart = row["timeStart"]
                if (stageName != null && artistName != null && timeDay != null && timeStart != null) {
                    EventJson(stageName, artistName, "$timeDay $timeStart")
                } else {
                    null
                }
            }
        }
    }
}

