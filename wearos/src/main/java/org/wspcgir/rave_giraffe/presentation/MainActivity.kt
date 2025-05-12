/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package org.wspcgir.rave_giraffe.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.wspcgir.rave_giraffe.EVENT_DATA_FILE
import org.wspcgir.rave_giraffe.EventMessages
import org.wspcgir.rave_giraffe.lib.Event
import org.wspcgir.rave_giraffe.lib.EventJson
import org.wspcgir.rave_giraffe.lib.dtFormat
import org.wspcgir.rave_giraffe.presentation.theme.RaveGiraffeTheme
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val latestEvents by EventMessages.eventMessages.collectAsState(emptyList())
            LaunchedEffect (true) {
                try {

                    val inputStream: InputStream = applicationContext.openFileInput(EVENT_DATA_FILE)

                    val inputStreamReader = InputStreamReader(inputStream)
                    val bufferedReader = BufferedReader(inputStreamReader)
                    var receiveString: String? = ""
                    val stringBuilder = StringBuilder()
                    while ((bufferedReader.readLine().also { receiveString = it }) != null) {
                        stringBuilder.append("\n").append(receiveString)
                    }
                    inputStream.close()
                    val data = Json.decodeFromString<List<EventJson>>(stringBuilder.toString())
                    EventMessages.emit(data.map {
                        Event(it.stageName, it.artistName, LocalDateTime.parse(it.startTime, dtFormat))
                    })
                } catch (_: FileNotFoundException) {
                }
            }
            WearApp(latestEvents)
        }
    }
}

fun buildInfo(sets: List<Event>): List<StageInfo> {
    val stages = sets.map { it.stageName }.toSet().toList()
    return stages
        .map { stage ->
            val setsAtStage = sets
                .filter { set -> set.stageName == stage }
                .sortedBy { it.startTime }

            val setsWithEnd = setsAtStage
                .zip(setsAtStage.drop(1).plus(null))
                .map {
                    val name = it.first.artistName
                    val startTime = it.first.startTime
                    val endTime = it.second?.startTime
                        ?: it.first.startTime.withHour(6).withMinute(0)
                    TimeInfo(name, startTime, endTime)
                }
            StageInfo(stage, setsWithEnd)
        }
        .sortedBy { it.stageName.lowercase() }
}

/**
 * ScrollableState integration for Horizontal Pager.
 */
class PagerScrollHandler(
    private val numPages: Int,
    private val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) : ScrollableState {
    override val isScrollInProgress: Boolean
        get() = totalDelta != 0f

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    private var totalDelta = 0f

    private val scrollableState = ScrollableState { delta ->
        totalDelta += delta

        val offset = when {
            // tune to match device
            totalDelta > 80f -> {
                1
            }
            totalDelta < -80f -> {
                -1
            }
            else -> null
        }

        if (offset != null) {
            totalDelta = 0f
            val newTargetPage = pagerState.targetPage + offset
            if (newTargetPage in (0 until numPages)) {
                coroutineScope.launch {
                    Log.i("TEST", "scrolling to next set")
                    pagerState.animateScrollToPage(newTargetPage, 0f)
                }
            }
        }

        delta
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        Log.i("SCROLL", "scrolling")
        scrollableState.scroll(block = block)
    }
}

@Composable
fun WearApp(sets: List<Event>) {
    RaveGiraffeTheme {
        val stages = buildInfo(sets)
        val pageState = rememberPagerState(initialPage = 0, pageCount = { stages.size })
        VerticalPager(
            state = pageState,
            modifier = Modifier
                .fillMaxSize()
        ) { n ->
            Stage(
                info = stages[n],
                nextStageAvailable = n < stages.lastIndex,
                previousStageAvailable = n > 0
            )
       }
    }
}

data class StageInfo(
    val stageName: String,
    val setTimes: List<TimeInfo>
)

@Composable
fun Stage(
    info: StageInfo,
    nextStageAvailable: Boolean,
    previousStageAvailable: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pageState = rememberPagerState (0 , pageCount = { info.setTimes.size })
        val scope = rememberCoroutineScope()
        val pagerScrollHandler = remember {
            PagerScrollHandler(info.setTimes.size, pageState, scope)
        }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        HorizontalPager(
            state = pageState,
            modifier = Modifier
                .onRotaryScrollEvent {
                    Log.i("SCROLL", "onRotaryScrollEvent")
                    scope.launch {
                        pagerScrollHandler.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
        ) { n ->
            Time(
                stageName = info.stageName,
                info = info.setTimes[n],
                nextSetAvailable = n < info.setTimes.lastIndex,
                previousSetAvailable = n > 0,
                nextStageAvailable = nextStageAvailable,
                previousStageAvailable = previousStageAvailable
            ) {
                Log.i("TEST", "setOver")
                scope.launch {
                    pageState.animateScrollToPage(n + 1)
                }
            }
        }
    }
}

data class TimeInfo(
    val artistName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)

@Composable
fun Time(
    stageName: String,
    info: TimeInfo,
    nextSetAvailable: Boolean,
    previousSetAvailable: Boolean,
    nextStageAvailable: Boolean,
    previousStageAvailable: Boolean,
    onSetOver: () -> Unit
) {
    val totalSeconds = Duration.between(info.startTime, info.endTime).seconds.toFloat()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var setInProgress by remember { mutableStateOf(true) }
    val hasStarted by remember { mutableStateOf(info.startTime.isBefore(now)) }
    val hasEnded by remember { mutableStateOf(info.endTime.isBefore(now)) }

    LaunchedEffect(now, setInProgress) {
        if (setInProgress) {
            if (info.endTime.isBefore(now)) {
                setInProgress = false
                onSetOver()
            } else {
                now = LocalDateTime.now()
                delay(1000)
            }
        }
    }

    Box {

        NavigationArrows(
            previousStageAvailable,
            nextStageAvailable,
            nextSetAvailable,
            previousSetAvailable
        )

        val currentSetStatus = if (!hasStarted) {
            SetStatus.Coming
        } else if (!hasEnded) {
            SetStatus.Progressing
        } else {
            SetStatus.Done
        }
        SetDisplay(stageName, info.artistName, info.startTime, currentSetStatus)

        if (hasStarted && !hasEnded) {
            val secondsSoFar = Duration.between(info.startTime, now).seconds.toFloat()
            val delta = secondsSoFar.div(totalSeconds)
            CircularProgressIndicator(
                progress = 1.0f - delta,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scaleX = -1f, scaleY = 1f),
            )
        }
    }
}

@Composable
fun NavigationArrows(
    displayUp: Boolean,
    displayDown: Boolean,
    displayForward: Boolean,
    displayBack: Boolean,
) {
    val padding = 5.dp
    val mod = Modifier
        .fillMaxSize()
        .padding(padding)
    val arrowMod = Modifier
        .scale(0.5f)
        .alpha(0.5f)

    if (displayUp) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = mod
        ) {
            Icon(
                modifier = arrowMod,
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Up"
            )
        }
    }

    if (displayDown) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = mod
        ) {
            Icon(
                modifier = arrowMod,
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Down"
            )
        }
    }

    if (displayForward) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = mod
        ) {
            Icon(
                modifier = arrowMod,
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward"
            )
        }
    }

    if (displayBack) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = mod
        ) {
            Icon(
                modifier = arrowMod,
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}

sealed class SetStatus {
    data object Coming : SetStatus()
    data object Progressing : SetStatus()
    data object Done : SetStatus()
}

@Composable
fun SetDisplay(
    stageName: String,
    artistName: String,
    startTime: LocalDateTime,
    status: SetStatus
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = when (status) {
            is SetStatus.Coming -> Icons.Filled.BrowseGallery
            is SetStatus.Progressing -> Icons.Filled.Audiotrack
            is SetStatus.Done -> Icons.Filled.Check
        }
        Icon(icon, contentDescription = "Current set status")
        Text(stageName)
        Text(artistName)
        Text(startTime.toLocalTime().toString())
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        listOf(
            Event(
                "Cosmic Meadow",
                "DJ Ron",
                LocalDateTime.of(2025, 4, 21, 21, 0, 0, 0)
            ),
            Event(
                "Cosmic Meadow",
                "Tiesto",
                LocalDateTime.of(2025, 4, 21, 22, 30, 0, 0)
            )
        )
    )
}