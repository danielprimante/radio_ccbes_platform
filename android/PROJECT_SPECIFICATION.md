# Especificación Técnica Detallada: Plataforma Radio CCBES

Este documento detalla la arquitectura, flujos de datos y especificaciones técnicas de la plataforma Radio CCBES, cubriendo tanto el desarrollo nativo móvil como el ecosistema administrativo web.

---

## 1. Arquitectura de la Aplicación Android

La aplicación móvil sigue los principios modernos de desarrollo de Android recomendados por Google.

### Estructura de Capas (MVVM)
- **UI Layer (Jetpack Compose):** Manejo de estados mediante `StateFlow`. Pantallas modulares localizadas en `ui/screens`.
- **Domain Layer:** Lógica de negocio encapsulada en ViewModels y repositorios.
- **Data Layer:** 
  - **Firebase Firestore:** Persistencia en la nube para usuarios y posts.
  - **Jetpack DataStore:** Preferencias locales (Auto-play, notificaciones habilitadas).
  - **RadioService:** Servicio `MediaSessionService` que encapsula `ExoPlayer` para la reproducción de audio.

### Componentes Clave
1.  **Módulo de Radio (`RadioService.kt`):**
    - Implementa `Media3` para control persistente.
    - Se conecta a un repositorio dinámico para obtener la URL del streaming actualizada desde Firestore.
    - Soporta control externo (Auriculares, Android Auto).
2.  **Módulo de Comunidad:**
    - Generación de QR dinámicos basados en el `userId`.
    - Integración de ML Kit para detección instantánea de códigos QR desde la cámara.

---

## 2. Ecosistema Web Administrativo

Construido para ser una herramienta de "back-office" potente y rápida.

### Funcionalidades de Gestión
- **Moderación de Usuarios:**
  - Búsqueda avanzada por nombre, email o ID.
  - Capacidad para banear/desbanear usuarios sospechosos.
- **CMS de Contenido:**
  - Visualización de posts activos.
  - Eliminación de contenido inapropiado.
- **Configuración de Plataforma:**
  - Editor en vivo para los términos y condiciones.
  - Gestión de variables de backend sin necesidad de desplegar código nuevo.

### Infraestructura Web
- **Rendering:** Server-Side Rendering (SSR) para mayor seguridad en el dashboard administrativo.
- **CORS Management:** Script especializado en `/web` para interactuar con Google Cloud Bucket, asegurando que las imágenes se sirvan correctamente a la App y la Web.

---

## 3. Especificación de Modelos de Datos (Firestore)

### Colección `users`
| Atributo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | String | UID de Firebase Auth |
| `name` | String | Nombre para mostrar |
| `handle` | String | Alias único (ej: @usuario) |
| `photoUrl` | String (URL) | URL en Firebase Storage |
| `isBanned` | Boolean | Estado de acceso |
| `metadata` | Map | Ciudad, Teléfono, Bio, etc. |

### Colección `posts`
| Atributo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | String | ID generado |
| `userId` | String | Referencia al autor |
| `content` | String | Texto del post |
| `imageUrl` | String (URL) | Imagen opcional |
| `category` | String | (Ej: "Noticia", "Anuncio") |
| `timestamp` | ServerTimestamp | Fecha de creación |

---

## 4. Stack de Notificaciones Push

### Arquitectura de Envío
1.  **Activador:** Acciones en el panel web (Next.js) o procesos de Cloud Functions.
2.  **Transmisor:** OneSignal SDK v5 para segmentación inteligente y envío masivo.
3.  **Receptor:** Implementación nativa en Android gestionando clics para navegación directa a posts o chats específicos.

---

## 5. Mantenimiento y Despliegue

### Requisitos de Desarrollo
- **Android:** JDK 17, Android Studio Ladybug+.
- **Web:** Node.js 18+, NPM 9+.
- **Firebase:** Proyecto activo con Firestore, Auth (Email/Google) y Storage habilitados.
---------------------------------------------------------------
📻 Prompt Maestro para Recrear Radio CCBES Platform en Antigravity
Este documento contiene un prompt completo y detallado para pegar en Antigravity y recrear desde cero el proyecto Radio CCBES Platform en su estado madurativo actual.

🎯 PROMPT PARA ANTIGRAVITY
Necesito crear una plataforma completa llamada "Radio CCBES Platform" con dos componentes principales:
1. Una aplicación móvil Android nativa (Kotlin + Jetpack Compose)
2. Un portal web administrativo (Next.js 15 + TypeScript + Tailwind CSS 4)
La plataforma debe servir como ecosistema digital para una radio cristiana con funcionalidades de red social, transmisión en vivo y gestión administrativa.
---
## 📱 PARTE 1: APLICACIÓN ANDROID
### Configuración del Proyecto Android
**Package Name:** `com.radio.ccbes`
**Application ID:** `com.radio.ccbes`
**Namespace:** `com.radio.ccbes`
**Versiones y Configuración de Compilación:**
- Kotlin: `2.1.0`
- Gradle AGP: `8.7.3`
- Compile SDK: `35`
- Target SDK: `35`
- Min SDK: `24` (Android 7.0+)
- JVM Toolchain: `17`
- Version Code: `12`
- Version Name: `1.21`
**Build Configuration:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
Dependencias Principales (build.gradle.kts)
kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    
    // Media3 / ExoPlayer para Radio Streaming
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-session:1.9.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    
    // OneSignal v5 para Notificaciones Push
    implementation("com.onesignal:OneSignal:5.4.2")
    
    // ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Coil para Carga de Imágenes con Caché
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Generación de QR (ZXing)
    implementation("com.google.zxing:core:3.5.4")
    
    // DataStore para Preferencias
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    
    // CameraX para Escaneo de QR
    val cameraxVersion = "1.5.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // ML Kit para Detección de Códigos QR
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // Jsoup para Link Previews
    implementation("org.jsoup:jsoup:1.18.1")
    
    // Room Database para Caché Local
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}
Plugins necesarios:

kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}
Arquitectura Android (MVVM)
Estructura de Carpetas:

android/app/src/main/java/com/radio/ccbes/
├── MainActivity.kt
├── RadioApplication.kt
├── data/
│   ├── auth/
│   ├── cache/              # Room Database para caché
│   ├── model/              # Modelos de datos Firestore
│   │   ├── User.kt
│   │   ├── Post.kt
│   │   ├── Comment.kt
│   │   ├── Message.kt
│   │   ├── Chat.kt
│   │   ├── Notification.kt
│   │   ├── Follow.kt
│   │   └── Report.kt
│   ├── repository/
│   │   ├── UserRepository.kt
│   │   ├── PostRepository.kt
│   │   ├── CommentRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── LikeRepository.kt
│   │   ├── FollowRepository.kt
│   │   ├── NotificationRepository.kt
│   │   ├── ImageUploadRepository.kt
│   │   ├── ConfigRepository.kt
│   │   └── ReportRepository.kt
│   ├── service/
│   │   ├── RadioService.kt               # MediaSessionService
│   │   ├── MyFirebaseMessagingService.kt # FCM
│   │   └── OneSignalService.kt
│   └── settings/
│       └── SettingsManager.kt            # DataStore
├── ui/
│   ├── components/         # Componentes reusables de Compose
│   ├── navigation/
│   │   ├── Screen.kt
│   │   ├── NavigationGraph.kt
│   │   └── BottomNavigationBar.kt
│   ├── screens/
│   │   ├── auth/
│   │   │   └── WelcomeScreen.kt
│   │   ├── main/
│   │   │   └── MainScreen.kt
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── PostCard.kt
│   │   ├── radio/
│   │   │   ├── RadioScreen.kt
│   │   │   └── RadioViewModel.kt
│   │   ├── profile/
│   │   │   ├── ProfileScreen.kt
│   │   │   ├── ProfileViewModel.kt
│   │   │   ├── EditProfileScreen.kt
│   │   │   ├── AccountSettingsScreen.kt
│   │   │   ├── ShareProfileScreen.kt    # Generación QR
│   │   │   ├── ScannerScreen.kt         # Escaneo QR
│   │   │   ├── AboutUsScreen.kt
│   │   │   └── TermsAndConditionsScreen.kt
│   │   ├── post/
│   │   │   ├── CreatePostScreen.kt
│   │   │   └── FullScreenImageActivity.kt
│   │   ├── chat/
│   │   │   ├── ChatListScreen.kt
│   │   │   ├── ChatListViewModel.kt
│   │   │   ├── ChatScreen.kt
│   │   │   └── ChatViewModel.kt
│   │   ├── notifications/
│   │   │   ├── NotificationsScreen.kt
│   │   │   └── NotificationsViewModel.kt
│   │   ├── comments/
│   │   │   ├── CommentScreen.kt
│   │   │   └── CommentItem.kt
│   │   ├── news/
│   │   │   ├── NewsScreen.kt
│   │   │   └── NewsViewModel.kt
│   │   └── search/
│   │       └── SearchViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt (Material 3)
└── util/
    └── Constants.kt
Modelos de Datos Firestore
User.kt:

kotlin
@Keep
data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val handle: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    @get:PropertyName("isBanned") val isBanned: Boolean = false,
    val fcmToken: String? = null,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val role: String = "user",
    val pronouns: String = "",
    val gender: String = "",
    val link: String = "",
    val category: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = ""
)
Post.kt:

kotlin
@Keep
data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userHandle: String = "",
    val userPhotoUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val likes: Int = 0,
    val comments: Int = 0,
    val timestamp: Timestamp = Timestamp.now(),
    @PropertyName("category") private val _category: String = "all"
) {
    val category: PostCategory
        get() = PostCategory.fromValue(_category)
}
@Keep
enum class PostCategory(val value: String, val displayName: String) {
    ALL("all", "Todo"),
    TRENDING("trending", "Tendencias"),
    NEWS("news", "Noticias"),
    REFLECTIONS("reflections", "Reflexiones");
    
    companion object {
        fun fromValue(value: String): PostCategory {
            return values().find { it.value == value } ?: ALL
        }
    }
}
Otros modelos: Comment, Message, Chat, Notification, Follow, Report (siguiendo patrones similares)

RadioService.kt - Servicio de Streaming
Implementación MediaSessionService con Media3/ExoPlayer:

kotlin
class RadioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsManager: SettingsManager
    private var isNotificationEnabled = true
    private lateinit var player: ExoPlayer
    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        
        // Configurar ExoPlayer con atributos de audio
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Metadatos
        val metadata = MediaMetadata.Builder()
            .setTitle("Radio CCBES")
            .setArtist("En Vivo")
            .setDisplayTitle("Radio en Vivo")
            .build()
        
        // Crear MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
        
        // Obtener URL dinámica desde Firestore y preparar reproducción
        serviceScope.launch {
            val configRepository = ConfigRepository()
            val streamUrl = configRepository.getStreamUrl()
            
            val mediaItem = MediaItem.Builder()
                .setMediaId("radio_id")
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            
            if (settingsManager.autoPlayRadio.first()) {
                player.play()
            }
        }
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
RadioApplication.kt - Configuración Global
Inicialización de OneSignal, Firebase, Coil:

kotlin
class RadioApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% memoria disponible
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        // OneSignal Initialization v5
        OneSignal.Debug.logLevel = LogLevel.WARN
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
        
        // Manejador de clics en notificaciones
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = event.notification.additionalData
                val postId = data?.optString("postId")
                val chatId = data?.optString("chatId")
                
                if (!postId.isNullOrEmpty() || !chatId.isNullOrEmpty()) {
                    val intent = Intent(this@RadioApplication, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (!postId.isNullOrEmpty()) putExtra("postId", postId)
                        if (!chatId.isNullOrEmpty()) putExtra("chatId", chatId)
                    }
                    startActivity(intent)
                }
            }
        })
        
        // Vincular usuario a OneSignal
        FirebaseAuth.getInstance().currentUser?.let { user ->
            OneSignal.login(user.uid)
        }
    }
}
MainActivity.kt
Manejo de Deep Links y Notificaciones:

kotlin
class MainActivity : ComponentActivity() {
    private var initialPostId by mutableStateOf<String?>(null)
    private var initialChatId by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        // Solicitar permiso de notificaciones Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            RadioCCBESTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        initialPostId = initialPostId,
                        initialChatId = initialChatId,
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }
    
    private fun handleIntent(intent: Intent?) {
        // Extras de notificación
        intent?.extras?.let { bundle ->
            bundle.getString("postId")?.let { initialPostId = it }
            bundle.getString("chatId")?.let { initialChatId = it }
        }
        
        // Deep Links
        intent?.data?.let { uri ->
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == "post") {
                initialPostId = pathSegments[1]
            }
        }
    }
}
AndroidManifest.xml
Permisos y Configuraciones:

xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    
    <application
        android:name=".RadioApplication"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.RadioCCBES">
        
        <!-- Configuración FCM -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@mipmap/ic_launcher" />
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="fcm_default_channel" />
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- Deep Links -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" />
                <data android:host="ccbes.com.ar" />
                <data android:pathPrefix="/post" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".data.service.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        
        <service
            android:name=".data.service.RadioService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
Características Principales de la App Android
Sistema de Radio en Vivo:

Reproductor persistente con Media3/ExoPlayer
Controles en notificación y pantalla de bloqueo
Reconexión automática
URL de streaming dinámica desde Firestore
Auto-play configurable
Autenticación:

Firebase Auth (Email/Password + Google Sign-In)
Manejo de sesión persistente
Pantalla de bienvenida
Feed Social:

Muro de posts con categorías (All, Trending, News, Reflections)
Likes y comentarios en tiempo real
Imágenes con Coil (caché optimizada)
Pull-to-refresh
Sistema de reportes
Perfiles de Usuario:

Edición de perfil (bio, ciudad, teléfono, etc.)
Foto de perfil
Generación de QR único por usuario
Escaneo de QR con CameraX + ML Kit
Chat Privado:

Mensajería 1-a-1 entre usuarios
Actualización en tiempo real con Firestore
Lista de chats
Notificaciones Push:

OneSignal v5 + FCM
Deep linking a posts o chats específicos
Segmentación por usuario
UI/UX:

100% Jetpack Compose
Material Design 3
Tema dinámico
Navegación bottom bar
🌐 PARTE 2: PORTAL WEB ADMINISTRATIVO
Configuración del Proyecto Web
Framework: Next.js 15 (App Router) Lenguaje: TypeScript Styling: Tailwind CSS 4

package.json:

json
{
  "name": "web",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start"
  },
  "dependencies": {
    "@google-cloud/storage": "^7.18.0",
    "firebase": "^10.7.1",
    "firebase-admin": "^13.6.0",
    "next": "16.1.1",
    "nodemailer": "^7.0.12",
    "react": "19.2.3",
    "react-dom": "19.2.3"
  },
  "devDependencies": {
    "@tailwindcss/postcss": "^4",
    "@types/node": "^20",
    "@types/nodemailer": "^7.0.5",
    "@types/react": "^19",
    "@types/react-dom": "^19",
    "eslint": "^9",
    "eslint-config-next": "16.1.1",
    "tailwindcss": "^4",
    "typescript": "^5"
  }
}
Estructura de Carpetas Web
web/
├── app/
│   ├── admin/
│   │   ├── layout.tsx              # Layout administrativo
│   │   ├── page.tsx                # Dashboard
│   │   ├── users/
│   │   │   ├── page.tsx            # Gestión de usuarios
│   │   │   └── [id]/page.tsx       # Detalle de usuario
│   │   ├── posts/
│   │   │   ├── page.tsx            # Moderación de posts
│   │   │   ├── create/page.tsx     # Crear post desde web
│   │   │   └── [id]/page.tsx       # Editar post
│   │   ├── moderation/
│   │   │   └── page.tsx            # Centro de moderación
│   │   ├── settings/
│   │   │   └── page.tsx            # Configuración global
│   │   ├── web-configuration/
│   │   │   └── page.tsx            # Configuración landing page
│   │   └── repairs/
│   │       └── page.tsx            # Herramientas de reparación
│   ├── login/
│   │   └── page.tsx                # Login administrativo
│   ├── politicas/
│   │   └── page.tsx                # Políticas públicas
│   ├── page.tsx                    # Landing page pública
│   ├── layout.tsx                  # Layout raíz
│   ├── globals.css                 # Estilos globales
│   └── not-found.tsx
├── components/
│   ├── Navbar.tsx
│   └── Sidebar.tsx
├── lib/
│   ├── firebase.ts                 # Firebase Client SDK
│   ├── firebase-admin.ts           # Firebase Admin SDK
│   ├── storage.ts                  # Integración ImgBB
│   └── api.ts                      # API de Firestore
├── public/
│   ├── logo.png
│   └── ...
├── .env.local                      # Variables de entorno
├── next.config.ts
├── tailwind.config.ts
└── tsconfig.json
Configuración Firebase (lib/firebase.ts)
typescript
import { initializeApp, getApps } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
const firebaseConfig = {
    apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
    authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
    projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
    storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
    appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID
};
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);
export { app, auth, db, storage };
Sistema de Almacenamiento (lib/storage.ts)
Integración con ImgBB para subida de imágenes:

typescript
const IMGBB_API_KEY = "d7d8c8028977e32ee9fab67b251c6697";
export async function uploadImage(file: File, path: string = 'posts'): Promise<string> {
    try {
        const formData = new FormData();
        formData.append('image', file);
        
        const response = await fetch(`https://api.imgbb.com/1/upload?key=${IMGBB_API_KEY}`, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('Error en la respuesta de ImgBB');
        }
        
        const data = await response.json();
        return data.data.url;
    } catch (error) {
        console.error('Error uploading image to ImgBB:', error);
        throw new Error('Error al subir la imagen');
    }
}
export async function compressImage(
    file: File,
    maxWidth: number = 1200,
    quality: number = 0.8
): Promise<File> {
    return new Promise((resolve, reject) => {
        const objectUrl = URL.createObjectURL(file);
        const img = new Image();
        img.src = objectUrl;
        
        img.onload = () => {
            URL.revokeObjectURL(objectUrl);
            const canvas = document.createElement('canvas');
            let width = img.width;
            let height = img.height;
            
            if (width > maxWidth) {
                height = (maxWidth / width) * height;
                width = maxWidth;
            }
            
            canvas.width = width;
            canvas.height = height;
            
            const ctx = canvas.getContext('2d');
            if (!ctx) {
                reject(new Error('No se pudo obtener el contexto del canvas'));
                return;
            }
            
            ctx.drawImage(img, 0, 0, width, height);
            
            const isTransparent = file.type === 'image/png' || file.type === 'image/webp';
            const outputType = isTransparent ? 'image/webp' : 'image/jpeg';
            
            canvas.toBlob(
                (blob) => {
                    if (blob) {
                        const compressedFile = new File([blob], file.name, {
                            type: outputType,
                            lastModified: Date.now(),
                        });
                        resolve(compressedFile);
                    } else {
                        reject(new Error('Error al comprimir la imagen'));
                    }
                },
                outputType,
                quality
            );
        };
    });
}
API de Firestore (lib/api.ts)
Funciones principales:

typescript
import {
    collection, addDoc, updateDoc, deleteDoc, doc,
    getDocs, query, orderBy, where, Timestamp, getDoc, setDoc
} from 'firebase/firestore';
import { db } from './firebase';
export interface Post {
    id?: string;
    userId: string;
    userName: string;
    userHandle: string;
    userPhotoUrl?: string;
    content: string;
    imageUrl?: string;
    likes: number;
    comments: number;
    timestamp: Timestamp;
    category: 'all' | 'trending' | 'news' | 'reflections';
}
export interface User {
    id: string;
    name: string;
    handle: string;
    photoUrl?: string;
    bio: string;
    isBanned: boolean;
    role?: 'admin' | 'user';
    city?: string;
    phone?: string;
    email?: string;
}
// Posts
export async function getPosts(): Promise<Post[]> {
    const q = query(collection(db, 'posts'), orderBy('timestamp', 'desc'));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Post));
}
export async function deletePost(id: string): Promise<void> {
    await deleteDoc(doc(db, 'posts', id));
}
// Users
export async function getUsers(): Promise<User[]> {
    const q = query(collection(db, 'users'), orderBy('name', 'asc'));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as User));
}
export async function updateUser(id: string, data: Partial<User>): Promise<void> {
    await updateDoc(doc(db, 'users', id), data);
    await cascadeUserUpdate(id, data);
}
export async function banUser(id: string): Promise<void> {
    await updateUser(id, { isBanned: true });
}
export async function unbanUser(id: string): Promise<void> {
    await updateUser(id, { isBanned: false });
}
// Settings
export async function getRadioSettings() {
    const docSnap = await getDoc(doc(db, 'settings', 'radio'));
    return docSnap.exists() ? docSnap.data() : null;
}
export async function updateRadioSettings(settings: any): Promise<void> {
    await setDoc(doc(db, 'settings', 'radio'), settings, { merge: true });
}
Landing Page Pública (app/page.tsx)
Características:

Hero section dinámico con imagen de fondo
Sección de Radio con QR y link a Play Store
Formulario de pedidos de oración
Modal de ofrendas con CBU/Alias
Eventos de la iglesia
Integración con Firestore para contenido dinámico
Responsive design con Tailwind CSS
Estructura:

typescript
"use client";
import { useState, useEffect } from "react";
import { doc, getDoc, collection, addDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
export default function Home() {
    const [heroData, setHeroData] = useState({
        title: "",
        subtitle: "",
        year: "",
        backgroundImage: "",
        whatsapp: ""
    });
    
    const [events, setEvents] = useState<string[]>([]);
    const [radioData, setRadioData] = useState({/*...*/});
    const [socialConfig, setSocialConfig] = useState({/*...*/});
    
    useEffect(() => {
        const fetchContent = async () => {
            const landingRef = doc(db, "content", "landing");
            const landingSnap = await getDoc(landingRef);
            if (landingSnap.exists()) {
                const data = landingSnap.data();
                // Actualizar estados...
            }
        };
        fetchContent();
    }, []);
    
    return (
        <div className="min-h-screen">
            {/* Navigation Bar */}
            {/* Hero Section */}
            {/* Radio Section con QR */}
            {/* Prayer & Offering */}
            {/* Events Grid */}
            {/* Footer */}
        </div>
    );
}
Dashboard Administrativo (app/admin/page.tsx)
Funcionalidades:

Estadísticas en tiempo real (total posts, users, likes, comments)
Gráficos de actividad
Accesos rápidos a secciones
Últimos posts publicados
Últimos usuarios registrados
Panel de Gestión de Usuarios (app/admin/users/page.tsx)
Funcionalidades:

Listado de todos los usuarios
Búsqueda por nombre/email/handle
Editar perfil de usuario
Banear/Desbanear usuarios
Ver detalles completos
Panel de Moderación de Posts (app/admin/posts/page.tsx)
Funcionalidades:

Listado de posts por categoría
Eliminación de posts inapropiados
Edición de contenido
Creación de posts desde el admin
Carga de imágenes con compresión
Configuración Global (app/admin/settings/page.tsx)
Permite configurar:

URL de streaming de radio
Términos y condiciones
Políticas de privacidad
Información de contacto
Redes sociales
Estilos y Diseño
Tailwind CSS 4 + Diseño Glassmorphic:

css
/* globals.css */
@import "tailwindcss";
:root {
  --primary: #B91C1C;
  --background: #FFFFFF;
}
@theme {
  --color-primary: #B91C1C;
  --font-display: 'Montserrat', sans-serif;
}
.glass-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
🔥 FIREBASE CONFIGURATION
Colecciones de Firestore
1. users:

id (auto)
name, handle, photoUrl, bio
email, phone, city
isBanned, role, termsAccepted
fcmToken
2. posts:

id (auto)
userId, userName, userHandle, userPhotoUrl
content, imageUrl, images[]
category (all/trending/news/reflections)
likes, comments
timestamp
3. comments:

id (auto)
postId, userId, userName, userPhotoUrl
content, imageUrl
likesCount
timestamp
4. likes:

id (auto)
postId, userId
timestamp
5. chats:

id (auto)
participants[] (array de 2 IDs)
lastMessage, lastMessageTimestamp
unreadCount (map de userId -> count)
6. messages:

id (auto)
chatId, senderId, recipientId
content, imageUrl
read
timestamp
7. notifications:

id (auto)
userId, type, fromUserId
fromUserName, fromUserProfilePic
postId, chatId
content, read
timestamp
8. follows:

id (auto)
followerId, followingId
timestamp
9. reports:

id (auto)
postId, commentId
reportedBy, reason
status (pending/reviewed/dismissed)
timestamp
10. settings:

Documentos: radio, about, terms, privacy
Campos dinámicos según el tipo
11. content:

Documento: landing
hero {title, subtitle, year, backgroundImage, whatsapp}
events[] (array de URLs de imágenes)
offering {message, alias, cbu}
radio {qrImage, playStoreLink}
12. prayer_requests:

id (auto)
name, phone, request
timestamp
Firebase Security Rules
javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId || 
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Posts
    match /posts/{postId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth.uid == resource.data.userId || 
                               get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Comments, Likes, etc. (similar patterns)
    
    // Settings (solo admin puede escribir)
    match /settings/{document} {
      allow read: if true;
      allow write: if request.auth != null && 
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
📡 NOTIFICACIONES PUSH
OneSignal Configuration
OneSignal App ID: ONESIGNAL_APP_ID (definir en Constants.kt)

Segmentación:

Login de usuario: OneSignal.login(userId)
Tags personalizados para targeting
Data adicional: {postId, chatId} para deep linking
Envío desde Web:

typescript
// Usando OneSignal REST API desde Cloud Functions o Server Actions
const sendNotification = async (userId: string, message: string, data: any) => {
    const response = await fetch('https://onesignal.com/api/v1/notifications', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Basic ${ONESIGNAL_REST_API_KEY}`
        },
        body: JSON.stringify({
            app_id: ONESIGNAL_APP_ID,
            include_external_user_ids: [userId],
            contents: { en: message },
            data: data
        })
    });
};
🎨 OPTIMIZACIONES IMPLEMENTADAS
Android
Caché de Imágenes (Coil):

25% memoria RAM para caché in-memory
512 MB de caché en disco
Crossfade transitions
Room Database:

Caché local para posts y usuarios
Sincronización con Firestore
ProGuard:

Minificación habilitada en release
Optimización de código
Media3 Optimization:

Audio focus automático
Manejo de desconexión de auriculares
Reconexión inteligente
DataStore:

Persistencia de preferencias (auto-play, notificaciones)
Web
Next.js 15 Optimizations:

App Router para navegación instantánea
Server-Side Rendering para admin
Static Export para landing page
Compresión de Imágenes:

WebP para transparencias
JPEG para otras imágenes
Compresión a 1200px max width
ImgBB Integration:

CDN gratuito para imágenes
Sin costos de Firebase Storage
Tailwind CSS 4:

Tree-shaking automático
JIT compiler
🚀 PASOS DE IMPLEMENTACIÓN
FASE 1: Configuración Inicial
Crear proyecto Firebase (Auth, Firestore, Storage, Cloud Messaging activados)
Configurar OneSignal (crear app Android)
Crear proyecto Android Studio con las dependencias listadas
Crear proyecto Next.js 15 con TypeScript
FASE 2: Backend (Firebase)
Crear colecciones en Firestore según el esquema
Configurar reglas de seguridad
Poblar documento settings/radio con URL de streaming inicial
Crear documento content/landing con datos de la landing page
FASE 3: Android
Implementar MainActivity y RadioApplication
Crear modelos de datos (User, Post, etc.)
Implementar repositorios (UserRepository, PostRepository, etc.)
Crear RadioService con Media3/ExoPlayer
Implementar pantallas de UI con Compose:
WelcomeScreen (Auth)
HomeScreen (Feed)
RadioScreen (Reproductor)
ProfileScreen (Perfil + QR)
ChatListScreen y ChatScreen
CreatePostScreen
Configurar navegación con NavHost
Integrar OneSignal y FCM
Implementar CameraX + ML Kit para QR scanner
Configurar google-services.json
FASE 4: Web
Implementar lib/firebase.ts y lib/firebase-admin.ts
Crear lib/api.ts con funciones de Firestore
Implementar lib/storage.ts con ImgBB
Crear landing page (app/page.tsx)
Implementar login administrativo (app/login/page.tsx)
Crear dashboard admin (app/admin/page.tsx)
Implementar gestión de usuarios (app/admin/users/)
Implementar moderación de posts (app/admin/posts/)
Crear panel de configuración (app/admin/settings/)
Configurar .env.local con credenciales de Firebase
FASE 5: Testing
Probar autenticación (Email + Google Sign-In)
Verificar reproducción de radio en background
Testear creación de posts con imágenes
Probar sistema de likes y comentarios
Verificar chats privados
Testear notificaciones push y deep linking
Probar escaneo y generación de QR
Verificar panel admin (baneos, moderación, configuración)
FASE 6: Despliegue
Compilar APK/AAB con firma de release
Subir a Google Play Console
Desplegar portal web a Vercel/Netlify
Configurar dominio personalizado
Publicar app móvil
📋 VARIABLES DE ENTORNO
Android (local.properties o Constants.kt)
kotlin
ONESIGNAL_APP_ID = "tu-onesignal-app-id"
Web (.env.local)
env
NEXT_PUBLIC_FIREBASE_API_KEY=
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=
NEXT_PUBLIC_FIREBASE_PROJECT_ID=
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
NEXT_PUBLIC_FIREBASE_APP_ID=
FIREBASE_ADMIN_PROJECT_ID=
FIREBASE_ADMIN_PRIVATE_KEY=
FIREBASE_ADMIN_CLIENT_EMAIL=
🎯 CARACTERÍSTICAS COMPLETAS
Android
✅ Radio en vivo persistente (Media3/ExoPlayer) ✅ Autenticación Firebase (Email + Google) ✅ Feed de posts con categorías ✅ Likes y comentarios en tiempo real ✅ Chat privado entre usuarios ✅ Notificaciones push (OneSignal v5 + FCM) ✅ Deep linking ✅ Generación de QR por usuario ✅ Escaneo de QR con ML Kit ✅ Edición de perfil completo ✅ Sistema de reportes ✅ Caché de imágenes optimizada ✅ Material Design 3 ✅ 100% Jetpack Compose ✅ MVVM Architecture

Web
✅ Landing page institucional dinámica ✅ Dashboard administrativo ✅ Gestión de usuarios (editar, banear) ✅ Moderación de posts y comentarios ✅ Creación de posts desde web ✅ Configuración global de radio ✅ Editor de términos y políticas ✅ Gestión de contenido landing ✅ Integración ImgBB para imágenes ✅ Diseño glassmorphic con Tailwind 4 ✅ Next.js 15 con App Router ✅ TypeScript completo

💡 NOTAS IMPORTANTES
google-services.json: Debe descargarse desde Firebase Console y colocarse en android/app/

OneSignal: Crear app en OneSignal Dashboard, obtener App ID y configurarlo en Android

Streaming URL: Configurar en Firestore settings/radio la URL del stream de audio

ImgBB API Key: La API key proporcionada es de ejemplo, reemplazar con tu propia key

Admin User: Crear el primer usuario admin manualmente en Firestore añadiendo role: "admin"

Deep Links: Configurar en Firebase Console el dominio para App Links

Iconos y Assets: Generar iconos de launcher en diferentes resoluciones (mipmap-*)

Multiidioma: El proyecto está en español, pero es fácilmente adaptable a múltiples idiomas con strings resources

Este es el prompt completo. Con esta información puedes recrear el proyecto Radio CCBES Platform desde cero, llegando a su estado madurativo actual con todas las funcionalidades implementadas y optimizaciones aplicadas.

---
## 📝 Notas de Uso del Prompt
1. **Copiar y pegar** el contenido dentro del bloque de código en Antigravity
2. Antigravity creará la estructura completa del proyecto
3. Necesitarás configurar:
   - Proyecto Firebase
   - Cuenta OneSignal
   - API Key de ImgBB
   - google-services.json
4. Seguir las fases de implementación en orden
5. Probar exhaustivamente cada componente antes de despliegue
---
**Fecha de creación:** 2026-02-09
**Versión del proyecto documentada:** 1.21 (Build 12)