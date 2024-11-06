package it.fast4x.rimusic.utils

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import it.fast4x.rimusic.ui.screens.settings.SettingsDescription

@OptIn(UnstableApi::class)
@Composable
fun RestartPlayerService(
    restartService: Boolean = false,
    onRestart: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    AnimatedVisibility(visible = restartService) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsDescription(
                text = stringResource(R.string.minimum_silence_length_warning),
                important = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_service),
                onClick = {
                    binder?.restartForegroundOrStop().let { onRestart() }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            )
        }
    }
}