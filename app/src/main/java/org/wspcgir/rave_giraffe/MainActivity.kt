package org.wspcgir.rave_giraffe

import android.content.pm.PackageManager
import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
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
import androidx.core.net.toUri


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaveGiraffeTheme {
                val p = READ_MEDIA_VISUAL_USER_SELECTED
                var granted by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Log.i("PERMS", "Permission granted!")
                        granted = true
                    } else {
                        Log.i("PERMS", "Permission denied!")
                        granted = false
                    }
                }

                LaunchedEffect(granted) {
                    if (!granted) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                p
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.i("PERMS", "Requesting permission!")
                            launcher.launch(p)
                        } else {
                            Log.i("PERMS", "Permission already granted!")
                        }
                    }
                }

                if (granted) {
                    Page({ data -> setEventDataOnWatch(data) })
                } else {
                    Scaffold { padding ->
                        Column(modifier = Modifier.padding(padding)) {
                            Button(onClick = { launcher.launch(p)} ) {
                                Text("Grant File Reading Permissions")
                            }
                        }
                    }
                }
            }
        }
    }


    private fun setEventDataOnWatch(data: List<EventJson>) {
        val messageClient = Wearable.getMessageClient(applicationContext)
        val nodeClient = Wearable.getNodeClient(applicationContext)
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Log.i("MESSAGES", "Sent message to ${node.displayName}")
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH,
                        Json.encodeToString(data).toByteArray(MESSAGE_CHARSET)
                    )
                }
            }
    }
}

@Composable
fun PickFileButton(text: String, onPlaylistPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onPlaylistPicked(it) }
    }
    Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
        Text(text = text)
    }
}

fun parseFileUri(content: String): List<Uri> {
    return content.lines().filter { it.isNotEmpty() && it.isNotBlank() }.map { it.toUri() }
}

@Composable
@Preview
fun Page(setEventDataOnWatch: (List<EventJson>) -> Unit = {}) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            val context = LocalContext.current
            PickFileButton("import set data") { uri ->
                val events: List<EventJson>? = context.contentResolver
                    .openInputStream(uri)?.use { inStream ->
                        EventJson.eventsFromString(inStream)
                    }
                if (events != null) {
                    setEventDataOnWatch(events)
                }
            }
        }
    }
}