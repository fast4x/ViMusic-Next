package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwarePaddingValues
import it.vfsfitvnm.vimusic.checkpoint
import it.vfsfitvnm.vimusic.internal
import it.vfsfitvnm.vimusic.path
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.themed.ConfirmationDialog
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.produceSaveableState
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

@ExperimentalAnimationApi
@Composable
fun DatabaseSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    var isShowingRestoreDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val queriesCount by produceSaveableState(initialValue = 0, stateSaver = autoSaver()) {
        Database.queriesCount().flowOn(Dispatchers.IO).distinctUntilChanged().collect { value = it }
    }

    val openDocumentContract = ActivityResultContracts.OpenDocument()
    val createDocumentContract = ActivityResultContracts.CreateDocument("application/vnd.sqlite3")

    val backupLauncher = rememberLauncherForActivityResult(createDocumentContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        query {
            Database.internal.checkpoint()
            context.applicationContext.contentResolver.openOutputStream(uri)
                ?.use { outputStream ->
                    FileInputStream(Database.internal.path).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(openDocumentContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        query {
            Database.internal.checkpoint()
            Database.internal.close()

            FileOutputStream(Database.internal.path).use { outputStream ->
                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
            }

            context.stopService(context.intent<PlayerService>())
            exitProcess(0)
        }
    }

    if (isShowingRestoreDialog) {
        ConfirmationDialog(
            text = "The application will automatically close itself to avoid problems after restoring the database.",
            onDismiss = {
                isShowingRestoreDialog = false
            },
            onConfirm = {
                val input = arrayOf(
                    "application/x-sqlite3",
                    "application/vnd.sqlite3",
                    "application/octet-stream"
                )

                if (openDocumentContract.createIntent(context, input)
                        .resolveActivity(context.packageManager) != null
                ) {
                    restoreLauncher.launch(input)
                } else {
                    Toast.makeText(
                        context,
                        "Can't read the database from the external storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            confirmText = "Ok"
        )
    }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LocalPlayerAwarePaddingValues.current)
    ) {
        Header(title = "Database")

        SettingsEntryGroupText(title = "SEARCH HISTORY")

        SettingsEntry(
            title = "Clear search history",
            text = if (queriesCount > 0) {
                "Delete $queriesCount search queries"
            } else {
                "History is empty"
            },
            isEnabled = queriesCount > 0,
            onClick = {
                query {
                    Database.clearQueries()
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "BACKUP")

        SettingsDescription(text = "Personal preferences (i.e. the theme mode) and the cache are excluded.")

        SettingsEntry(
            title = "Backup",
            text = "Export the database to the external storage",
            onClick = {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                val input = "vimusic_${dateFormat.format(Date())}.db"

                if (createDocumentContract.createIntent(context, input)
                        .resolveActivity(context.packageManager) != null
                ) {
                    backupLauncher.launch(input)
                } else {
                    Toast.makeText(
                        context,
                        "Can't copy the database to the external storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = "RESTORE")

        SettingsDescription(text = "Existing data will be overwritten.")

        SettingsEntry(
            title = "Restore",
            text = "Import the database from the external storage",
            onClick = { isShowingRestoreDialog = true }
        )
    }
}
