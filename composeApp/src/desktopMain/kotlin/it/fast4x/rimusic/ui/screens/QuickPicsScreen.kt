package it.fast4x.rimusic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import player.PlayerInput
import player.PlayerSource
import player.component.ComponentPlayer
import player.frame.FramePlayer
import vlcj.VlcjComponentController
import vlcj.VlcjFrameController

@Composable
fun QuickPicsScreen(
    modifier: Modifier = Modifier
) {
    var url = remember { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }


            val componentController = remember(url) { VlcjComponentController() }
            val frameController = remember(url) { VlcjFrameController() }

            BoxWithConstraints(
                modifier = Modifier
                    .padding(top = 16.dp, start = 200.dp, end = 16.dp)
                    .fillMaxSize().border(BorderStroke(1.dp, Color.Blue))
            ) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp).border(BorderStroke(1.dp, Color.Red)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    /*
                    ComponentPlayer(
                        Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color.Yellow)),
                        url,
                        componentController.component,
                        componentController
                    )
                     */

                    FramePlayer(
                        Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color.Yellow)),
                        url,
                        frameController.size.collectAsState(null).value?.run {
                            IntSize(first, second)
                        } ?: IntSize.Zero,
                        frameController.bytes.collectAsState(null).value,
                        frameController
                    )

                    PlayerInput {
                        url = it
                    }

                }
            }


    /*
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Quick Pics")
        }

    }
     */

}