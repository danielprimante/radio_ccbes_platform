package com.radio.ccbes.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.radio.ccbes.R
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.auth.AuthManager
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.theme.RedAccent
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    val defaultPrivacyUrl = stringResource(R.string.privacy_policy_url)
    var privacyPolicyUrl by remember { mutableStateOf(defaultPrivacyUrl) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isEmailLoginVisible by remember { mutableStateOf(false) }

    // Cargar URL de políticas desde Firestore
    LaunchedEffect(Unit) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("content")
            .document("landing")
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null) {
                    val url = doc.getString("radio.privacyPolicyLink")
                    if (!url.isNullOrBlank()) {
                        privacyPolicyUrl = if (url.startsWith("/")) {
                            "https://ccbes.com.ar$url"
                        } else {
                            url
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }



    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(stringResource(R.string.terms_and_conditions_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.terms_dialog_content))
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text(stringResource(R.string.close), color = RedAccent)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {

            if (isLoading) {
                CircularProgressIndicator(color = RedAccent)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.welcome_to),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = stringResource(R.string.welcome_subtitle),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(64.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = RedAccent)
                        )
                        Text(
                            text = stringResource(R.string.accept_terms),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.terms_and_conditions),
                            fontSize = 14.sp,
                            color = RedAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showTermsDialog = true }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Checkbox(
                            checked = privacyAccepted,
                            onCheckedChange = { privacyAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = RedAccent)
                        )
                        Text(
                            text = stringResource(R.string.accept_privacy),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.privacy_policy),
                            fontSize = 14.sp,
                            color = RedAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(privacyPolicyUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val idToken = AuthManager.signInWithGoogle(context)
                                if (idToken != null) {
                                    isLoading = true
                                    AuthManager.firebaseAuthWithGoogle(idToken) { success ->
                                        if (success) {
                                            val firebaseUser = auth.currentUser
                                            if (firebaseUser != null) {
                                                scope.launch {
                                                    val existingUser = userRepository.getUser(firebaseUser.uid)
                                                    if (existingUser?.isBanned == true) {
                                                        isLoading = false
                                                        try {
                                                            snackbarHostState.showSnackbar("Tu cuenta ha sido suspendida")
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        auth.signOut()
                                                        return@launch
                                                    }

                                                    val newUser = User(
                                                        id = firebaseUser.uid,
                                                        name = firebaseUser.displayName ?: "Usuario",
                                                        handle = "@${
                                                            firebaseUser.displayName?.replace(" ", "")
                                                                ?.lowercase() ?: "usuario"
                                                        }",
                                                        photoUrl = firebaseUser.photoUrl?.toString(),
                                                        privacyAccepted = true,
                                                        termsAccepted = true
                                                    )
                                                    userRepository.createOrUpdateUser(newUser)
                                                    isLoading = false
                                                    onLoginSuccess()
                                                }
                                            } else {
                                                isLoading = false
                                            }
                                        } else {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No se pudo obtener la cuenta de Google. Intenta de nuevo.")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = termsAccepted && privacyAccepted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedAccent,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            stringResource(R.string.login_google),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEmailLoginVisible) {
                        TextButton(
                            onClick = { isEmailLoginVisible = true },
                            enabled = termsAccepted && privacyAccepted
                        ) {
                            Text(
                                stringResource(R.string.login_with_email),
                                color = if (termsAccepted && privacyAccepted) RedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(stringResource(R.string.label_email)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedAccent,
                                    focusedLabelColor = RedAccent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(stringResource(R.string.password)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedAccent,
                                    focusedLabelColor = RedAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        isLoading = true
                                        AuthManager.signInWithEmail(email, password) { success, error ->
                                            if (success) {
                                                isLoading = false
                                                onLoginSuccess()
                                            } else {
                                                isLoading = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(error ?: context.getString(R.string.login_error_generic))
                                                }
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.fill_all_fields))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                            ) {
                                Text(stringResource(R.string.enter_button), fontWeight = FontWeight.Bold)
                            }

                            TextButton(onClick = { isEmailLoginVisible = false }) {
                                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
