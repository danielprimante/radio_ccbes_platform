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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Message
import com.radio.ccbes.ui.theme.RedAccent
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
    var isTyping by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Media Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(context, it) }
    }

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { viewModel.sendImage(context, it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = FileUtils.createImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(chatId) {
        viewModel.initChat(chatId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    // Auto-scroll when typing
    LaunchedEffect(messageText, isFocused) {
        if (messageText.isNotEmpty() && isFocused && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = uiState.messages.size - 1,
                scrollOffset = 0
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 0.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (uiState.otherUserPhoto.isNotEmpty()) {
                                AsyncImage(
                                    model = uiState.otherUserPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = uiState.otherUserName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.otherUserHandle.isNotEmpty()) {
                                Text(
                                    text = if (uiState.otherUserHandle.startsWith("@")) uiState.otherUserHandle else "@${uiState.otherUserHandle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Borrar chat") },
                            onClick = {
                                showMenu = false
                                viewModel.deleteChat { navController.navigateUp() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reportar usuario") },
                            onClick = {
                                showMenu = false
                                viewModel.reportUser("Reporte desde chat")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bloquear usuario") },
                            onClick = {
                                showMenu = false
                                viewModel.blockUser()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background, 
        bottomBar = {
            ChatInput(
                messageText = messageText,
                onMessageChange = { 
                    messageText = it
                    isTyping = it.isNotEmpty()
                },
                onSend = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                    isTyping = false
                },
                onFocusChanged = { focused ->
                    isFocused = focused
                },
                onCameraClick = {
                    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        val uri = FileUtils.createImageUri(context)
                        tempImageUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    val isMine = message.senderId == currentUserId
                    MessageBubble(
                        modifier = Modifier.animateItem(
                            placementSpec = spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        message = message,
                        isMine = isMine,
                        onEdit = { messageId, newContent ->
                            viewModel.editMessage(messageId, newContent)
                        },
                        onDelete = { messageId ->
                            viewModel.deleteMessage(messageId)
                        },
                        onImageClick = { _, postId ->
                            if (postId != null) {
                                navController.navigate(com.radio.ccbes.ui.navigation.Screen.Comments.createRoute(postId))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            if (uiState.isUploading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedAccent)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onEdit: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onImageClick: (String, String?) -> Unit = { _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }
    
    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar mensaje") },
            text = {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe tu mensaje") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) {
                            onEdit(message.id, editText)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar mensaje") },
            text = { Text("¿Estás seguro de que quieres eliminar este mensaje? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(message.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) RedAccent else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            tonalElevation = 0.5.dp,
            modifier = Modifier.combinedClickable(
                onClick = { },
                onLongClick = {
                    if (isMine && message.type == "text") {
                        showMenu = true
                    }
                }
            )
        ) {
            Box {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        if (message.type == "image") {
                            val context = LocalContext.current
                            AsyncImage(
                                model = message.content,
                                contentDescription = "Imagen",
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .heightIn(max = 400.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!message.postId.isNullOrBlank()) {
                                            onImageClick(message.content, message.postId)
                                        } else {
                                            val intent = android.content.Intent(context, com.radio.ccbes.ui.screens.post.FullScreenImageActivity::class.java).apply {
                                                putExtra("IMAGE_URL", message.content)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                        Text(
                            text = message.content,
                            fontSize = 15.sp
                        )
                        if (message.isEdited) {
                            Text(
                                text = "(editado)",
                                fontSize = 10.sp,
                                color = if (isMine) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatToTime(message.timestamp),
                            fontSize = 10.sp,
                            color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isMine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✓✓", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
                
                // Menu for edit/delete
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            showMenu = false
                            editText = message.content
                            showEditDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Borrar", color = RedAccent) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
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
    onFocusChanged: (Boolean) -> Unit = {},
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onGalleryClick) { 
                        Icon(Icons.Default.AttachFile, "Galería", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                    
                    TextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        placeholder = { Text("Mensaje", color = Color.Gray, fontSize = 16.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                onFocusChanged(focusState.isFocused)
                            },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    IconButton(onClick = onCameraClick) { 
                        Icon(Icons.Default.CameraAlt, "Cámara", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                }
            }
            
            AnimatedVisibility(
                visible = messageText.isNotBlank(),
                enter = fadeIn(animationSpec = tween(200)) + 
                        scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                exit = fadeOut(animationSpec = tween(150)) + 
                       scaleOut(
                           targetScale = 0.8f,
                           animationSpec = tween(150)
                       )
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                val scale by animateFloatAsState(
                    targetValue = if (messageText.isNotBlank()) 1f else 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "sendButtonScale"
                )
                
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(RedAccent.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = RedAccent,
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        }
    }
}
