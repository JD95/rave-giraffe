package org.wspcgir.rave_giraffe

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import org.wspcgir.rave_giraffe.lib.Event
import org.wspcgir.rave_giraffe.lib.EventJson
import org.wspcgir.rave_giraffe.lib.MESSAGE_PATH
import org.wspcgir.rave_giraffe.lib.dtFormat
import java.io.OutputStreamWriter
import java.time.LocalDateTime

object EventMessages {
    private val _eventMessages = MutableSharedFlow<List<Event>>(extraBufferCapacity = 10)
    val eventMessages: SharedFlow<List<Event>> = _eventMessages

    fun emit(msg: List<Event>) {
        _eventMessages.tryEmit(msg)
    }
}

val EVENT_DATA_FILE = "event-data.json"

class MessageListenerService : WearableListenerService() {

    override fun onCreate() {
        Log.i("MESSAGES", "Listener created")
        super.onCreate()
    }

    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)
        Log.i("MESSAGES", "Got message!")
        if (event.path == MESSAGE_PATH) {
            Log.i("MESSAGES", "Was my message!")
            val data = Json.decodeFromString<List<EventJson>>(String(event.data))
            val outputStreamWriter = OutputStreamWriter(applicationContext.openFileOutput(
                EVENT_DATA_FILE, MODE_PRIVATE))
            val stringData = String(event.data)
            Log.i("MESSAGES", stringData)
            outputStreamWriter.write(stringData)
            outputStreamWriter.close();
            EventMessages.emit(data.map {
                Event(it.stageName, it.artistName, LocalDateTime.parse(it.startTime, dtFormat))
            })
        }
    }
}