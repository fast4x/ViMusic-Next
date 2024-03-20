package it.vfsfitvnm.vimusic.ui.components.themed


import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.PlaylistSortBy
import it.vfsfitvnm.vimusic.enums.SortOrder
import it.vfsfitvnm.vimusic.models.Artist
import it.vfsfitvnm.vimusic.models.Info
import it.vfsfitvnm.vimusic.models.Playlist
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongPlaylistMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.isLocal
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.items.ArtistItem
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.screens.albumRoute
import it.vfsfitvnm.vimusic.ui.screens.artistRoute
import it.vfsfitvnm.vimusic.ui.screens.home.PINNED_PREFIX
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.favoritesIcon
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.addNext
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.downloadedStateMedia
import it.vfsfitvnm.vimusic.utils.durationToMillis
import it.vfsfitvnm.vimusic.utils.enqueue
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.formatAsDuration
import it.vfsfitvnm.vimusic.utils.formatAsTime
import it.vfsfitvnm.vimusic.utils.getDownloadState
import it.vfsfitvnm.vimusic.utils.manageDownload
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.playlistSortByKey
import it.vfsfitvnm.vimusic.utils.playlistSortOrderKey
import it.vfsfitvnm.vimusic.utils.positionAndDurationState
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime.now
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@ExperimentalTextApi
@ExperimentalAnimationApi
@androidx.media3.common.util.UnstableApi
@Composable
fun InHistoryMediaItemMenu(
    onDismiss: () -> Unit,
    song: Song,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current

    var isHiding by remember {
        mutableStateOf(false)
    }

    if (isHiding) {
        ConfirmationDialog(
            text = stringResource(R.string.hidesong),
            onDismiss = { isHiding = false },
            onConfirm = {
                onDismiss()
                query {
                    // Not sure we can to this here
                    binder?.cache?.removeResource(song.id)
                    Database.incrementTotalPlayTimeMs(song.id, -song.totalPlayTimeMs)
                }
            }
        )
    }

    NonQueuedMediaItemMenu(
        mediaItem = song.asMediaItem,
        onDismiss = onDismiss,
        onHideFromDatabase = { isHiding = true },
        modifier = modifier
    )
}

@ExperimentalTextApi
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun InPlaylistMediaItemMenu(
    onDismiss: () -> Unit,
    playlistId: Long,
    positionInPlaylist: Int,
    song: Song,
    modifier: Modifier = Modifier
) {
    NonQueuedMediaItemMenu(
        mediaItem = song.asMediaItem,
        onDismiss = onDismiss,
        onRemoveFromPlaylist = {
            transaction {
                Database.move(playlistId, positionInPlaylist, Int.MAX_VALUE)
                Database.delete(SongPlaylistMap(song.id, playlistId, Int.MAX_VALUE))
            }
        },
        modifier = modifier
    )
}

@ExperimentalTextApi
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun NonQueuedMediaItemMenuLibrary(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
) {
    val binder = LocalPlayerServiceBinder.current

    var isHiding by remember {
        mutableStateOf(false)
    }

    if (isHiding) {
        ConfirmationDialog(
            text = stringResource(R.string.hidesong),
            onDismiss = { isHiding = false },
            onConfirm = {
                onDismiss()
                query {
                    if (binder != null) {
                            binder.cache.removeResource(mediaItem.mediaId)
                            Database.resetTotalPlayTimeMs(mediaItem.mediaId)
                    }
                }
            }
        )
    }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onStartRadio = {
            binder?.stopRadio()
            binder?.player?.forcePlay(mediaItem)
            binder?.setupRadio(
                NavigationEndpoint.Endpoint.Watch(
                    videoId = mediaItem.mediaId,
                    playlistId = mediaItem.mediaMetadata.extras?.getString("playlistId")
                )
            )
        },
        onPlayNext = { binder?.player?.addNext(mediaItem) },
        onEnqueue = { binder?.player?.enqueue(mediaItem) },
        onDownload = onDownload,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onHideFromDatabase = { isHiding = true },
        onRemoveFromQuickPicks = onRemoveFromQuickPicks,
        modifier = modifier
    )
}

@ExperimentalTextApi
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun NonQueuedMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
) {
    val binder = LocalPlayerServiceBinder.current

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onStartRadio = {
            binder?.stopRadio()
            binder?.player?.forcePlay(mediaItem)
            binder?.setupRadio(
                NavigationEndpoint.Endpoint.Watch(
                    videoId = mediaItem.mediaId,
                    playlistId = mediaItem.mediaMetadata.extras?.getString("playlistId")
                )
            )
        },
        onPlayNext = { binder?.player?.addNext(mediaItem) },
        onEnqueue = { binder?.player?.enqueue(mediaItem) },
        onDownload = onDownload,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onHideFromDatabase = onHideFromDatabase,
        onRemoveFromQuickPicks = onRemoveFromQuickPicks,
        modifier = modifier
    )
}

@ExperimentalTextApi
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun QueuedMediaItemMenu(
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?,
    mediaItem: MediaItem,
    indexInQueue: Int?,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onDownload = onDownload,
        onRemoveFromQueue = if (indexInQueue != null) ({
            binder?.player?.removeMediaItem(indexInQueue)
        }) else null,
        onPlayNext = { binder?.player?.addNext(mediaItem) },
        modifier = modifier
    )
}

@ExperimentalTextApi
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun BaseMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    MediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onGoToEqualizer = onGoToEqualizer,
        onShowSleepTimer = onShowSleepTimer,
        onStartRadio = onStartRadio,
        onPlayNext = onPlayNext,
        onEnqueue = onEnqueue,
        onDownload = onDownload,
        onAddToPlaylist = { playlist, position ->
            transaction {
                Database.insert(mediaItem)
                Database.insert(
                    SongPlaylistMap(
                        songId = mediaItem.mediaId,
                        playlistId = Database.insert(playlist).takeIf { it != -1L } ?: playlist.id,
                        position = position
                    )
                )
            }
        },
        onHideFromDatabase = onHideFromDatabase,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onRemoveFromQueue = onRemoveFromQueue,
        onGoToAlbum = albumRoute::global,
        onGoToArtist = artistRoute::global,
        onShare = {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "https://music.youtube.com/watch?v=${mediaItem.mediaId}"
                )
            }

            context.startActivity(Intent.createChooser(sendIntent, null))
        },
        onRemoveFromQuickPicks = onRemoveFromQuickPicks,
        modifier = modifier
    )
}

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalAnimationApi
@Composable
fun MediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onAddToPlaylist: ((Playlist, Int) -> Unit)? = null,
    onGoToAlbum: ((String) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
    onShare: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val density = LocalDensity.current

    val binder = LocalPlayerServiceBinder.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val isLocal by remember { derivedStateOf { mediaItem.isLocal } }

    var isViewingPlaylists by remember {
        mutableStateOf(false)
    }

    var showSelectDialogListenOn by remember {
        mutableStateOf(false)
    }

    var height by remember {
        mutableStateOf(0.dp)
    }

    var albumInfo by remember {
        mutableStateOf(mediaItem.mediaMetadata.extras?.getString("albumId")?.let { albumId ->
            Info(albumId, null)
        })
    }

    var artistsInfo by remember {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { artistNames ->
                mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artistIds ->
                    artistNames.zip(artistIds).map { (authorName, authorId) ->
                        Info(authorId, authorName)
                    }
                }
            }
        )
    }

    var likedAt by remember {
        mutableStateOf<Long?>(null)
    }

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    downloadState = getDownloadState(mediaItem.mediaId)
    val isDownloaded = if (!isLocal) downloadedStateMedia(mediaItem.mediaId) else true

    var artistsList by persistList<Artist?>("home/artists")
    var artistIds = remember { mutableListOf("") }

    LaunchedEffect(Unit, mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            //if (albumInfo == null)
            albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            //if (artistsInfo == null)
            artistsInfo = Database.songArtistInfo(mediaItem.mediaId)

            artistsInfo?.forEach { info ->
                if (info.id.isNotEmpty()) artistIds.add(info.id)
            }
            Database.getArtistsList(artistIds).collect { artistsList = it }
        }
    }

    LaunchedEffect(Unit, mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).collect { likedAt = it }
    }

    var showCircularSlider by remember {
        mutableStateOf(false)
    }

    var showDialogChangeSongTitle by remember {
        mutableStateOf(false)
    }

    var songSaved by remember {
        mutableStateOf(0)
    }
    LaunchedEffect(Unit, mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            songSaved = Database.songExist(mediaItem.mediaId)
        }
    }

    if (showDialogChangeSongTitle)
        InputTextDialog(
            onDismiss = { showDialogChangeSongTitle = false },
            title = stringResource(R.string.update_title),
            value = mediaItem.mediaMetadata.title.toString(),
            placeholder = stringResource(R.string.title),
            setValue = {
                if (it.isNotEmpty()) {
                    query {
                        Database.updateSongTitle(mediaItem.mediaId, it)
                    }
                    //context.toast("Song Saved $it")
                }
            }
        )

    AnimatedContent(
        targetState = isViewingPlaylists,
        transitionSpec = {
            val animationSpec = tween<IntOffset>(400)
            val slideDirection = if (targetState) AnimatedContentTransitionScope.SlideDirection.Left
            else AnimatedContentTransitionScope.SlideDirection.Right

            slideIntoContainer(slideDirection, animationSpec) togetherWith
                    slideOutOfContainer(slideDirection, animationSpec)
        }, label = ""
    ) { currentIsViewingPlaylists ->
        if (currentIsViewingPlaylists) {
            val sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
            val sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)
            val playlistPreviews by remember {
                Database.playlistPreviews(sortBy, sortOrder)
            }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

            val pinnedPlaylists = playlistPreviews.filter {
                it.playlist.name.startsWith(PINNED_PREFIX, 0, true)
            }

            val unpinnedPlaylists = playlistPreviews.filter {
                !it.playlist.name.startsWith(PINNED_PREFIX, 0, true)
            }

            var isCreatingNewPlaylist by rememberSaveable {
                mutableStateOf(false)
            }

            if (isCreatingNewPlaylist && onAddToPlaylist != null) {
                InputTextDialog(
                    onDismiss = { isCreatingNewPlaylist = false },
                    title = stringResource(R.string.enter_the_playlist_name),
                    value = "",
                    placeholder = stringResource(R.string.enter_the_playlist_name),
                    setValue = { text ->
                        onDismiss()
                        onAddToPlaylist(Playlist(name = text), 0)
                    }
                )
                /*
                TextFieldDialog(
                    hintText = "Enter the playlist name",
                    onDismiss = { isCreatingNewPlaylist = false },
                    onDone = { text ->
                        onDismiss()
                        onAddToPlaylist(Playlist(name = text), 0)
                    }
                )
                 */
            }

            BackHandler {
                isViewingPlaylists = false
            }

            Menu(
                modifier = modifier
                    .requiredHeight(height)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { isViewingPlaylists = false },
                        icon = R.drawable.chevron_back,
                        color = colorPalette.textSecondary,
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    if (onAddToPlaylist != null) {
                        SecondaryTextButton(
                            text = stringResource(R.string.new_playlist),
                            onClick = { isCreatingNewPlaylist = true },
                            alternative = true
                        )
                    }
                }

                if (pinnedPlaylists.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.pinned_playlists),
                        style = typography.m.semiBold,
                        modifier = modifier.padding(start = 20.dp, top = 5.dp)
                    )

                    onAddToPlaylist?.let { onAddToPlaylist ->
                        pinnedPlaylists.forEach { playlistPreview ->
                            MenuEntry(
                                icon = R.drawable.add_in_playlist,
                                text = playlistPreview.playlist.name.substringAfter(PINNED_PREFIX),
                                secondaryText = "${playlistPreview.songCount} " + stringResource(R.string.songs),
                                onClick = {
                                    onDismiss()
                                    onAddToPlaylist(playlistPreview.playlist, playlistPreview.songCount)
                                }
                            )
                        }
                    }
                }

                if (unpinnedPlaylists.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.playlists),
                        style = typography.m.semiBold,
                        modifier = modifier.padding(start = 20.dp, top = 5.dp)
                    )

                    onAddToPlaylist?.let { onAddToPlaylist ->
                        unpinnedPlaylists.forEach { playlistPreview ->
                            MenuEntry(
                                icon = R.drawable.add_in_playlist,
                                text = playlistPreview.playlist.name,
                                secondaryText = "${playlistPreview.songCount} " + stringResource(R.string.songs),
                                onClick = {
                                    onDismiss()
                                    onAddToPlaylist(playlistPreview.playlist, playlistPreview.songCount)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Menu(
                modifier = modifier
                    .onPlaced { height = with(density) { it.size.height.toDp() } }
            ) {
                val thumbnailSizeDp = Dimensions.thumbnails.song + 20.dp
                val thumbnailSizePx = thumbnailSizeDp.px
                val thumbnailArtistSizeDp = Dimensions.thumbnails.song + 10.dp
                val thumbnailArtistSizePx = thumbnailArtistSizeDp.px

                Box(
                    modifier = Modifier
                        .fillMaxSize()

                ) {
                    Image(
                        painter = painterResource(R.drawable.chevron_down),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.text),
                        modifier = Modifier
                            .absoluteOffset(0.dp, -10.dp)
                            .align(Alignment.TopCenter)
                            .size(30.dp)
                            .clickable { onDismiss() }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 12.dp)
                ) {
                    SongItem(
                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx)
                            ?.toString(),
                        isDownloaded = isDownloaded,
                        onDownloadClick = {
                            binder?.cache?.removeResource(mediaItem.mediaId)
                            query {
                                Database.insert(
                                    Song(
                                        id = mediaItem.mediaId,
                                        title = mediaItem.mediaMetadata.title.toString(),
                                        artistsText = mediaItem.mediaMetadata.artist.toString(),
                                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(
                                            thumbnailSizePx
                                        ).toString(),
                                        durationText = null
                                    )
                                )
                            }
                            if (!isLocal)
                                manageDownload(
                                    context = context,
                                    songId = mediaItem.mediaId,
                                    songTitle = mediaItem.mediaMetadata.title.toString(),
                                    downloadState = isDownloaded
                                )
                        },
                        downloadState = downloadState,
                        title = mediaItem.mediaMetadata.title.toString(),
                        authors = mediaItem.mediaMetadata.artist.toString(),
                        duration = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        modifier = Modifier
                            .weight(1f),
                        mediaId = mediaItem.mediaId
                    )


                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                            //icon = R.drawable.heart,
                            color = colorPalette.favoritesIcon,
                            //color = if (likedAt == null) colorPalette.textDisabled else colorPalette.text,
                            onClick = {
                                query {
                                    if (Database.like(
                                            mediaItem.mediaId,
                                            if (likedAt == null) System.currentTimeMillis() else null
                                        ) == 0
                                    ) {
                                        Database.insert(mediaItem, Song::toggleLike)
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(all = 4.dp)
                                .size(24.dp)
                        )

                        if (!isLocal) IconButton(
                            icon = R.drawable.share_social,
                            color = colorPalette.text,
                            onClick = onShare,
                            modifier = Modifier
                                .padding(all = 4.dp)
                                .size(24.dp)
                        )

                    }

                }
/*
                if (artistsList.isNotEmpty())
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp)
                            .fillMaxWidth()
                            //.border(BorderStroke(1.dp, Color.Red))
                            .background(colorPalette.background1)
                    ) {
                        artistsList.forEach { artist ->
                            if (artist != null) {
                                ArtistItem(
                                    artist = artist,
                                    showName = false,
                                    thumbnailSizePx = thumbnailArtistSizePx,
                                    thumbnailSizeDp = thumbnailArtistSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = {
                                            if (onGoToArtist != null) {
                                                onDismiss()
                                                onGoToArtist(artist.id)
                                            }
                                        })
                                )
                            }
                        }
                    }



                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                )

                Spacer(
                    modifier = Modifier
                        .alpha(0.5f)
                        .align(Alignment.CenterHorizontally)
                        .background(colorPalette.textDisabled)
                        .height(1.dp)
                        .fillMaxWidth(1f)
                )
*/
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                )

                  if (!isLocal && songSaved > 0) {
                    MenuEntry(
                        icon = R.drawable.pencil,
                        text = stringResource(R.string.update_title),
                        onClick = {
                            showDialogChangeSongTitle = true
                        }
                    )
                }

                if (!isLocal) onStartRadio?.let { onStartRadio ->
                    MenuEntry(
                        icon = R.drawable.radio,
                        text = stringResource(R.string.start_radio),
                        onClick = {
                            onDismiss()
                            onStartRadio()
                        }
                    )
                }

                onPlayNext?.let { onPlayNext ->
                    MenuEntry(
                        icon = R.drawable.play_skip_forward,
                        text = stringResource(R.string.play_next),
                        onClick = {
                            onDismiss()
                            onPlayNext()
                        }
                    )
                }

                onEnqueue?.let { onEnqueue ->
                    MenuEntry(
                        icon = R.drawable.enqueue,
                        text = stringResource(R.string.enqueue),
                        onClick = {
                            onDismiss()
                            onEnqueue()
                        }
                    )
                }

                if (!isDownloaded)
                    onDownload?.let { onDownload ->
                        MenuEntry(
                            icon = R.drawable.download,
                            text = stringResource(R.string.download),
                            onClick = {
                                onDismiss()
                                onDownload()
                            }
                        )
                    }


                onGoToEqualizer?.let { onGoToEqualizer ->
                    MenuEntry(
                        icon = R.drawable.equalizer,
                        text = stringResource(R.string.equalizer),
                        onClick = {
                            onDismiss()
                            onGoToEqualizer()
                        }
                    )
                }

                // TODO: find solution to this shit
                onShowSleepTimer?.let {
                    val binder = LocalPlayerServiceBinder.current
                    val (_, typography) = LocalAppearance.current
                    var isShowingSleepTimerDialog by remember {
                        mutableStateOf(false)
                    }

                    val sleepTimerMillisLeft by (binder?.sleepTimerMillisLeft
                        ?: flowOf(null))
                        .collectAsState(initial = null)

                    val positionAndDuration = binder?.player?.positionAndDurationState()

                    var timeRemaining by remember { mutableIntStateOf(0) }

                    if (positionAndDuration != null) {
                        timeRemaining = positionAndDuration.value.second.toInt() - positionAndDuration.value.first.toInt()
                    }

                    //val timeToStop = System.currentTimeMillis()

                    if (isShowingSleepTimerDialog) {
                        if (sleepTimerMillisLeft != null) {
                            ConfirmationDialog(
                                text = stringResource(R.string.stop_sleep_timer),
                                cancelText = stringResource(R.string.no),
                                confirmText = stringResource(R.string.stop),
                                onDismiss = { isShowingSleepTimerDialog = false },
                                onConfirm = {
                                    binder?.cancelSleepTimer()
                                    onDismiss()
                                }
                            )
                        } else {
                            DefaultDialog(
                                onDismiss = { isShowingSleepTimerDialog = false }
                            ) {
                                var amount by remember {
                                    mutableStateOf(1)
                                }

                                BasicText(
                                    text = stringResource(R.string.set_sleep_timer),
                                    style = typography.s.semiBold,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 24.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        space = 16.dp,
                                        alignment = Alignment.CenterHorizontally
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                ) {
                                    if (!showCircularSlider) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .alpha(if (amount <= 1) 0.5f else 1f)
                                                .clip(CircleShape)
                                                .clickable(enabled = amount > 1) { amount-- }
                                                .size(48.dp)
                                                .background(colorPalette.background0)
                                        ) {
                                            BasicText(
                                                text = "-",
                                                style = typography.xs.semiBold
                                            )
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            BasicText(
                                                text = stringResource(
                                                    R.string.left,
                                                    formatAsDuration(amount * 5 * 60 * 1000L)
                                                ),
                                                style = typography.s.semiBold,
                                                modifier = Modifier
                                                    .clickable {
                                                        showCircularSlider = !showCircularSlider
                                                    }
                                            )
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .alpha(if (amount >= 60) 0.5f else 1f)
                                                .clip(CircleShape)
                                                .clickable(enabled = amount < 60) { amount++ }
                                                .size(48.dp)
                                                .background(colorPalette.background0)
                                        ) {
                                            BasicText(
                                                text = "+",
                                                style = typography.xs.semiBold
                                            )
                                        }

                                    } else {
                                        CircularSlider(
                                            stroke = 40f,
                                            thumbColor = colorPalette.accent,
                                            text = formatAsDuration(amount * 5 * 60 * 1000L),
                                            modifier = Modifier
                                                .size(300.dp),
                                            onChange = {
                                                amount = (it * 120).toInt()
                                            }
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier
                                        .padding(bottom = 20.dp)
                                        .fillMaxWidth()
                                ) {
                                    SecondaryTextButton(
                                        text = stringResource(R.string.set_to) + " "
                                                + formatAsDuration(timeRemaining.toLong())
                                                + " " + stringResource(R.string.end_of_song),
                                        onClick = {
                                            binder?.startSleepTimer(timeRemaining.toLong())
                                            isShowingSleepTimerDialog = false
                                        }
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {

                                    IconButton(
                                        onClick = { showCircularSlider = !showCircularSlider },
                                        icon = R.drawable.time,
                                        color = colorPalette.text
                                    )
                                    IconButton(
                                        onClick = { isShowingSleepTimerDialog = false },
                                        icon = R.drawable.close,
                                        color = colorPalette.text
                                    )
                                    IconButton(
                                        enabled = amount > 0,
                                        onClick = {
                                            binder?.startSleepTimer(amount * 5 * 60 * 1000L)
                                            isShowingSleepTimerDialog = false
                                        },
                                        icon = R.drawable.checkmark,
                                        color = colorPalette.accent
                                    )
                                }
                            }
                        }
                    }

                    MenuEntry(
                        icon = R.drawable.alarm,
                        text = stringResource(R.string.sleep_timer),
                        onClick = { isShowingSleepTimerDialog = true },
                        trailingContent = sleepTimerMillisLeft?.let {
                            {
                                BasicText(
                                    text = stringResource(
                                        R.string.left,
                                        formatAsDuration(it)
                                    ) + " / " +
                                            now()
                                                .plusSeconds(it / 1000)
                                                .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " +
                                            stringResource(R.string.sleeptimer_stop),
                                    style = typography.xxs.medium,
                                    modifier = modifier
                                        .background(
                                            color = colorPalette.background0,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .animateContentSize()
                                )
                            }
                        }
                    )
                }

                if (onAddToPlaylist != null) {
                    MenuEntry(
                        icon = R.drawable.add_in_playlist,
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { isViewingPlaylists = true },
                        trailingContent = {
                            Image(
                                painter = painterResource(R.drawable.chevron_forward),
                                contentDescription = null,
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    colorPalette.textSecondary
                                ),
                                modifier = Modifier
                                    .size(16.dp)
                            )
                        }
                    )
                }
                /*
                onGoToAlbum?.let { onGoToAlbum ->
                    albumInfo?.let { (albumId) ->
                        MenuEntry(
                            icon = R.drawable.disc,
                            text = stringResource(R.string.go_to_album),
                            onClick = {
                                onDismiss()
                                onGoToAlbum(albumId)
                            }
                        )
                    }
                }
                 */

                if (!isLocal) onGoToAlbum?.let { onGoToAlbum ->
                    albumInfo?.let { (albumId) ->
                        MenuEntry(
                            icon = R.drawable.disc,
                            text = stringResource(R.string.go_to_album),
                            onClick = {
                                onDismiss()
                                onGoToAlbum(albumId)
                            }
                        )
                    }
                }

                if (!isLocal) onGoToArtist?.let { onGoToArtist ->
                    artistsInfo?.forEach { (authorId, authorName) ->
                        MenuEntry(
                            icon = R.drawable.person,
                            text = stringResource(R.string.more_of) + " $authorName",
                            onClick = {
                                onDismiss()
                                onGoToArtist(authorId)
                            }
                        )
                    }
                }

                if (!isLocal) MenuEntry(
                    icon = R.drawable.play,
                    text = stringResource(R.string.listen_on),
                    onClick = { showSelectDialogListenOn = true }
                )

                if (showSelectDialogListenOn)
                    SelectorDialog(
                        title = stringResource(R.string.listen_on),
                        onDismiss = { showSelectDialogListenOn = false },
                        values = listOf(
                            Info(
                                "https://youtube.com/watch?v=${mediaItem.mediaId}",
                                stringResource(R.string.listen_on_youtube)
                            ),
                            Info(
                                "https://music.youtube.com/watch?v=${mediaItem.mediaId}",
                                stringResource(R.string.listen_on_youtube_music)
                            ),
                            Info(
                                "https://piped.kavin.rocks/watch?v=${mediaItem.mediaId}&playerAutoPlay=true",
                                stringResource(R.string.listen_on_piped)
                            ),
                            Info(
                                "https://yewtu.be/watch?v=${mediaItem.mediaId}&autoplay=1",
                                stringResource(R.string.listen_on_invidious)
                            )
                        ),
                        onValueSelected = {
                            binder?.player?.pause()
                            showSelectDialogListenOn = false
                            uriHandler.openUri(it)
                        }
                    )
                /*
                                if (!isLocal) MenuEntry(
                                    icon = R.drawable.play,
                                    text = stringResource(R.string.listen_on_youtube),
                                    onClick = {
                                        onDismiss()
                                        binder?.player?.pause()
                                        uriHandler.openUri("https://youtube.com/watch?v=${mediaItem.mediaId}")
                                    }
                                )

                                val ytNonInstalled = stringResource(R.string.it_seems_that_youtube_music_is_not_installed)
                                if (!isLocal) MenuEntry(
                                    icon = R.drawable.musical_notes,
                                    text = stringResource(R.string.listen_on_youtube_music),
                                    onClick = {
                                        onDismiss()
                                        binder?.player?.pause()
                                        if (!launchYouTubeMusic(context, "watch?v=${mediaItem.mediaId}"))
                                            context.toast(ytNonInstalled)
                                    }
                                )


                                if (!isLocal) MenuEntry(
                                    icon = R.drawable.play,
                                    text = stringResource(R.string.listen_on_piped),
                                    onClick = {
                                        onDismiss()
                                        binder?.player?.pause()
                                        uriHandler.openUri("https://piped.kavin.rocks/watch?v=${mediaItem.mediaId}&playerAutoPlay=true&minimizeDescription=true")
                                    }
                                )
                                if (!isLocal) MenuEntry(
                                    icon = R.drawable.play,
                                    text = stringResource(R.string.listen_on_invidious),
                                    onClick = {
                                        onDismiss()
                                        binder?.player?.pause()
                                        uriHandler.openUri("https://yewtu.be/watch?v=${mediaItem.mediaId}&autoplay=1")
                                    }
                                )

                */

                onRemoveFromQueue?.let { onRemoveFromQueue ->
                    MenuEntry(
                        icon = R.drawable.trash,
                        text = stringResource(R.string.remove_from_queue),
                        onClick = {
                            onDismiss()
                            onRemoveFromQueue()
                        }
                    )
                }

                onRemoveFromPlaylist?.let { onRemoveFromPlaylist ->
                    MenuEntry(
                        icon = R.drawable.trash,
                        text = stringResource(R.string.remove_from_playlist),
                        onClick = {
                            onDismiss()
                            onRemoveFromPlaylist()
                        }
                    )
                }

                if (!isLocal) onHideFromDatabase?.let { onHideFromDatabase ->
                    MenuEntry(
                        icon = R.drawable.trash,
                        text = stringResource(R.string.hide),
                        onClick = onHideFromDatabase
                    )
                }

                if (!isLocal) onRemoveFromQuickPicks?.let {
                    MenuEntry(
                        icon = R.drawable.trash,
                        text = stringResource(R.string.hide_from_quick_picks),
                        onClick = {
                            onDismiss()
                            onRemoveFromQuickPicks()
                        }
                    )
                }
            }
        }
    }
}
