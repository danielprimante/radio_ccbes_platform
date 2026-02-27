package com.radio.ccbes.ui.screens.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Message
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.util.FileUtils
import com.radio.ccbes.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }

    // Lanzadores de medios
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendImage(context, it) }
    }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempImageUri?.let { viewModel.sendImage(context, it) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = FileUtils.createImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(chatId) { viewModel.initChat(chatId) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            if (isInitialLoad) {
                listState.scrollToItem(0)
                isInitialLoad = false
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            if (uiState.otherUserPhoto.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uiState.otherUserPhoto)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else Icon(Icons.Default.Person, null, Modifier.padding(8.dp), tint = Color.Gray)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(uiState.otherUserName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (uiState.otherUserHandle.isNotEmpty()) {
                                Text(
                                    if (uiState.otherUserHandle.startsWith("@")) uiState.otherUserHandle else "@${uiState.otherUserHandle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Borrar chat") }, onClick = { showMenu = false; viewModel.deleteChat { navController.navigateUp() } })
                        DropdownMenuItem(text = { Text("Reportar usuario") }, onClick = { showMenu = false; viewModel.reportUser("Reporte") })
                        DropdownMenuItem(text = { Text("Bloquear usuario") }, onClick = { showMenu = false; viewModel.blockUser() })
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(
                        items = uiState.messages.asReversed(),
                        key = { it.id }
                    ) { message ->
                        val isMine = message.senderId == currentUserId
                        MessageBubble(
                            message = message,
                            isMine = isMine,
                            sharedPost = uiState.sharedPosts[message.postId],
                            onEdit = { id, text -> viewModel.editMessage(id, text) },
                            onDelete = { id -> viewModel.deleteMessage(id) },
                            onImageClick = { _, postId ->
                                postId?.let { navController.navigate(Screen.PostDetail.createRoute(it)) }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (uiState.isUploading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = RedAccent)
                }
            }

            ChatInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = { if (messageText.isNotBlank()) { viewModel.sendMessage(messageText); messageText = "" } },
                onCameraClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = FileUtils.createImageUri(context)
                        tempImageUri = uri
                        cameraLauncher.launch(uri)
                    } else permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onGalleryClick = { galleryLauncher.launch("image/*") }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    sharedPost: Post? = null,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onImageClick: (String, String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar mensaje") },
            text = { TextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (editText.isNotBlank()) { onEdit(message.id, editText); showEditDialog = false } }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar") },
            text = { Text("¿Eliminar este mensaje?") },
            confirmButton = { TextButton(onClick = { onDelete(message.id); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) RedAccent else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            modifier = Modifier.combinedClickable(
                onClick = { },
                onLongClick = { if (isMine && message.type == "text") showMenu = true }
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.postId != null) {
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onImageClick(message.content, message.postId) }
                    ) {
                        if (sharedPost != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.LightGray) {
                                    AsyncImage(model = sharedPost.userPhotoUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (sharedPost.userHandle.isNotEmpty()) "@${sharedPost.userHandle.removePrefix("@")}" else sharedPost.userName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(sharedPost.imageUrl ?: message.content)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 400.dp),
                                contentScale = ContentScale.FillWidth
                            )
                            if (sharedPost.content.isNotEmpty()) {
                                Text(
                                    text = sharedPost.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = RedAccent, strokeWidth = 3.dp)
                            }
                        }
                    }
                } else if (message.type == "image") {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.content)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen",
                        modifier = Modifier
                            .widthIn(max = 250.dp)
                            .heightIn(min = 100.dp, max = 450.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    // AQUÍ ESTÁ EL CAMBIO DE ALINEACIÓN:
                    FlowRow(
                        modifier = Modifier.widthIn(max = 260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.Center // Centra los elementos entre sí si hay varias líneas
                    ) {
                        // El mensaje principal
                        Text(
                            text = message.content,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterVertically) // Fuerza alineación central
                        )

                        // Contenedor de la hora y estado
                        Row(
                            verticalAlignment = Alignment.CenterVertically, // Alinea la hora con el centro del texto
                            modifier = Modifier.padding(top = 2.dp) // Pequeño ajuste fino
                        ) {
                            if (message.isEdited) {
                                Text(
                                    "(editado)",
                                    fontSize = 10.sp,
                                    modifier = Modifier.alpha(0.6f).padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = TimeUtils.formatToTime(message.timestamp),
                                fontSize = 10.sp,
                                modifier = Modifier.alpha(0.7f)
                            )
                        }
                    }
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; showEditDialog = true })
                    DropdownMenuItem(text = { Text("Borrar", color = RedAccent) }, onClick = { showMenu = false; showDeleteDialog = true })
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onGalleryClick) { Icon(Icons.Default.AttachFile, null) }
                Box(Modifier.weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (messageText.isEmpty()) Text("Mensaje", color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                IconButton(onClick = onCameraClick) { Icon(Icons.Default.CameraAlt, null) }
            }
        }

        AnimatedVisibility(
            visible = messageText.isNotBlank(),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RedAccent)
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}