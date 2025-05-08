package org.wspcgir.rave_giraffe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.wspcgir.rave_giraffe.lib.Event
import org.wspcgir.rave_giraffe.lib.EventJson
import org.wspcgir.rave_giraffe.lib.MESSAGE_CHARSET
import org.wspcgir.rave_giraffe.lib.MESSAGE_PATH
import org.wspcgir.rave_giraffe.lib.dtFormat
import org.wspcgir.rave_giraffe.ui.theme.RaveGiraffeTheme
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            RaveGiraffeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Button(onClick = { scope.launch { setEventDataOnWatch() } }) {
                            Text("Send Test Data")
                        }
                    }
                }
            }
        }
    }


    private fun setEventDataOnWatch() {
        val testData = listOf(
            Event("Kinetic Field", "Bob", LocalDateTime.of(2025, 4, 21, 17, 0, 0))
        ).map { it -> EventJson(it.stageName, it.artistName, it.startTime.format(dtFormat)) }
        val messageClient = Wearable.getMessageClient(applicationContext)
        val nodeClient = Wearable.getNodeClient(applicationContext)
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Log.i("MESSAGES", "Sent message to ${node.displayName}")
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH,
                        Json.encodeToString(testData).toByteArray(MESSAGE_CHARSET)
                    )
                }
            }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RaveGiraffeTheme {
        Greeting("Android")
    }
}