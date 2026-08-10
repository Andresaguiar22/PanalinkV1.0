package com.example.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.audio.AudioTrackEntity
import com.example.media.ui.components.TrackItem
import com.example.media.playlist.PlaylistEntity
import com.example.media.player.ui.MusicViewModel

/**
 * P6.7 - Music Home Screen
 * Primary dashboard for PanaLink Audio Studio & Playlist Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicHomeScreen(
    onBackClick: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onInvitationsClick: () -> Unit,
    onPlayTrack: (AudioTrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // In a real app, these would be injected or obtained from a ViewModelFactory
    // For this prototype, we'll use a simplified approach
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioLibraryManager = remember { com.example.media.audio.AudioLibraryManager(com.example.media.audio.AudioRepository(com.example.data.database.PanalinkDatabase.getDatabase(context).audioDao())) }
    val playlistDao = remember { com.example.data.database.PanalinkDatabase.getDatabase(context).playlistDao() }
    val collaboratorDao = remember { com.example.data.database.PanalinkDatabase.getDatabase(context).collaboratorDao() }
    val playlistRepository = remember { com.example.media.playlist.PlaylistRepository(playlistDao, collaboratorDao) }
    
    val viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MusicViewModel(audioLibraryManager, playlistRepository) as T
            }
        }
    )

    val playlists by viewModel.playlists.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Explorar, 1: Playlists, 2: Álbumes, 3: Artistas
    var selectedTrack by remember { mutableStateOf<AudioTrackEntity?>(null) }
    var showTrackOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PanaLink Music", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onInvitationsClick) {
                            Icon(Icons.Default.Notifications, contentDescription = "Invitaciones", tint = Color.White)
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Nueva Playlist", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
                
                // Professional Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar canciones, artistas...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF38BDF8),
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Explorar", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Playlists", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("Álbumes", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                        Text("Artistas", modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (searchQuery.isNotEmpty()) {
                SearchResults(
                    tracks = filteredTracks, 
                    onTrackClick = onPlayTrack,
                    onTrackOptionsClick = {
                        selectedTrack = it
                        showTrackOptions = true
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> ExploreSection(
                        playlists = playlists,
                        recentTracks = recentTracks,
                        favoriteTracks = favoriteTracks,
                        onPlaylistClick = onPlaylistClick,
                        onPlayTrack = onPlayTrack,
                        onTrackOptionsClick = { 
                            selectedTrack = it
                            showTrackOptions = true
                        },
                        onCreatePlaylist = { showCreateDialog = true }
                    )
                    1 -> PlaylistGrid(playlists = playlists, onClick = onPlaylistClick)
                    2 -> AlbumGrid(albums = albums)
                    3 -> ArtistGrid(artists = artists)
                }
            }
        }
    }

    if (showTrackOptions && selectedTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showTrackOptions = false },
            containerColor = Color(0xFF1E293B)
        ) {
            TrackOptionsBottomSheet(
                track = selectedTrack!!,
                onPlayNext = { /* viewModel.playNext(selectedTrack!!) */ },
                onAddToPlaylist = { /* viewModel.addToPlaylist(selectedTrack!!) */ },
                onFavorite = { /* viewModel.toggleFavorite(selectedTrack!!) */ },
                onEditMetadata = { /* open editor */ },
                onShare = { /* share track */ },
                onDelete = { /* delete track */ },
                onDismiss = { showTrackOptions = false }
            )
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva Playlist") },
            text = {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Nombre de la playlist") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (playlistName.isNotBlank()) {
                        viewModel.createPlaylist(playlistName)
                        showCreateDialog = false
                    }
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ExploreSection(
    playlists: List<PlaylistEntity>,
    recentTracks: List<AudioTrackEntity>,
    favoriteTracks: List<AudioTrackEntity>,
    onPlaylistClick: (String) -> Unit,
    onPlayTrack: (AudioTrackEntity) -> Unit,
    onTrackOptionsClick: (AudioTrackEntity) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        item {
            SectionHeader("Mis Playlists", onSeeAll = {})
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item { CreatePlaylistCard(onClick = onCreatePlaylist) }
                items(playlists) { playlist ->
                    PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
                }
            }
        }

        item {
            SectionHeader("Escuchado recientemente")
            if (recentTracks.isEmpty()) {
                Text("No hay actividad reciente", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        items(recentTracks) { track ->
            TrackItem(track = track, onClick = { onPlayTrack(track) }, onTrackOptionsClick = onTrackOptionsClick)
        }

        item {
            SectionHeader("Tus favoritos")
            if (favoriteTracks.isEmpty()) {
                Text("No tienes favoritos aún", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        items(favoriteTracks) { track ->
            TrackItem(track = track, onClick = { onPlayTrack(track) }, onTrackOptionsClick = onTrackOptionsClick)
        }
        
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun SearchResults(tracks: List<AudioTrackEntity>, onTrackClick: (AudioTrackEntity) -> Unit, onTrackOptionsClick: (AudioTrackEntity) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { 
            Text("Resultados de búsqueda", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp)) 
        }
        items(tracks) { track ->
            TrackItem(track = track, onClick = { onTrackClick(track) }, onTrackOptionsClick = onTrackOptionsClick)
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("Ver todas", color = Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
fun CreatePlaylistCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(140.dp).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nueva", color = Color.White)
        }
    }
}

@Composable
fun PlaylistCard(playlist: PlaylistEntity, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(140.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF334155))
            ) {
                if (!playlist.coverPath.isNullOrEmpty()) {
                    AsyncImage(model = playlist.coverPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(48.dp).align(Alignment.Center), tint = Color(0xFF38BDF8))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(playlist.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Playlist", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlaylistGrid(playlists: List<PlaylistEntity>, onClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(playlists.chunked(2)) { rowPlaylists ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowPlaylists.forEach { playlist ->
                    Box(modifier = Modifier.weight(1f)) {
                        PlaylistCard(playlist = playlist, onClick = { onClick(playlist.id) })
                    }
                }
                if (rowPlaylists.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AlbumGrid(albums: List<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(albums) { album ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(album, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArtistGrid(artists: List<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(artists) { artist ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(artist, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
