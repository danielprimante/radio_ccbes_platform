# 📻 Radio CCBES Platform

Bienvenido al repositorio oficial de **Radio CCBES**, una plataforma integral diseñada para conectar a la comunidad a través de transmisiones de radio en vivo, interacción social y una gestión administrativa eficiente.

Este proyecto es un ecosistema robusto de dos componentes: una **aplicación móvil Android** nativa y un **portal web avanzado** (Next.js) para administración.

---

## 📱 Aplicación Android

La aplicación móvil es el corazón de la experiencia del usuario, diseñada con un enfoque en el rendimiento, la accesibilidad y la modernidad.

### ✨ Características Principales
- **🎙️ Radio en Vivo de Alto Rendimiento**:
  - Reproductor persistente mediante `Media3` / `ExoPlayer`.
  - Soporte para audio en segundo plano con controles multimedia en la barra de notificaciones y pantalla de bloqueo.
  - Reconexión automática y gestión inteligente del búfer.
- **🔐 Autenticación Robusta**:
  - Sistema híbrido con Firebase Auth.
  - Soporte completo para Google Sign-In.
  - Perfiles de usuario con metadata personalizada (bio, ciudad, redes sociales).
- **💬 Red Social & Feed Dinámico**:
  - Muro de noticias con soporte para imágenes y múltiples formatos.
  - Sistema de interacciones completo: Likes, comentarios en tiempo real.
  - Chat privado entre usuarios integrado.
  - Actualización por gestos "Pull-to-refresh".
- **📸 Comunidad QR Exclusiva**:
  - Generación de códigos QR únicos para cada perfil de usuario.
  - Escáner integrado de alta velocidad basado en CameraX y ML Kit.
- **🔔 Gestión de Notificaciones Avanzada**:
  - Integración nativa con OneSignal v5 y Firebase Cloud Messaging (FCM).
  - Soporte para navegación profunda (Deep Linking) desde la notificación.
- **🎨 UI/UX Premium**:
  - Interfaz 100% Declarativa con `Jetpack Compose`.
  - Sistema de diseño Material Design 3 con soporte para temas dinámicos.

### ⚙️ Compatibilidad y Arquitectura
- **Arquitectura:** MVVM (Model-View-ViewModel) con Repositorios y Inyección de Dependencias.
- **Soporte:** Android 7.0 (API 24) hasta Android 14+ (API 34).
- **Justificación:** Optimizado para cubrir el 95% de los dispositivos activos, asegurando la estabilidad de los servicios de streaming `Media3`.

---

## 🌐 Portal Web Administrativo (Control Center)

El portal web es la herramienta central para los administradores y el soporte, ofreciendo una visión clara y control total sobre la plataforma.

### ✨ Características Principales
- **⚡ Arquitectura Next.js 15+**: Aprovecha el App Router para una navegación instantánea y optimización SEO.
- **🎨 Estética Futurista**: Desarrollado con `Tailwind CSS 4`, ofreciendo un diseño "glassmorphic" y ultra-responsivo.
- **🛡️ Dashboard Administrativo**:
  - Gestión centralizada de usuarios (visualización, baneos, edición).
  - Moderación de posts y comentarios en tiempo real.
- **🖼️ Gestión de Medios Cloud**:
  - Integración profunda con Google Cloud Storage para almacenamiento de imágenes de posts y perfiles.
  - Configuración automatizada de políticas CORS para acceso seguro.
- **⚙️ Configuración Global**:
  - Panel para actualizar URLs de streaming, términos y condiciones, y políticas de privacidad directamente desde la web.

---

## 🛠️ Stack Tecnológico Detallado

| Capa | Tecnologías |
| :--- | :--- |
| **Android Core** | Kotlin, Coroutines, Flow, Dagger/Hilt. |
| **Android UI** | Jetpack Compose, Material 3, Coil (Imágenes). |
| **Mobile Services** | Media3 (ExoPlayer), CameraX, ML Kit, ZXing. |
| **Web Frontend** | Next.js 15, TypeScript, Tailwind CSS 4, Lucid Icons. |
| **Backend/Cloud** | Firebase (Auth/Firestore/Cloud Functions), Google Cloud Platform. |
| **Notificaciones** | OneSignal, FCM. |

---

## 📁 Estructura del Repositorio

```text
radio_ccbes_platform/
├── android/              # Proyecto Android Studio (Kotlin/Compose)
│   ├── app/              # Aplicación principal
│   │   ├── src/main/java # Lógica de negocio, UI y Repositorios
│   │   └── build.gradle.kts # Dependencias y Configuración SDK
│   └── PROJECT_SPECIFICATION.md # Documentación técnica detallada
├── web/                  # Portal administrativo (Next.js/TypeScript)
│   ├── app/              # Rutas y páginas (Dashboard, Moderación)
│   ├── components/       # Componentes UI reusables
│   └── lib/              # SDKs de Firebase y utilidades
└── README.md             # Documentación principal
```

---

## 🚀 Cómo Empezar

### Android
1. Abre `/android` en Android Studio.
2. Asegúrate de tener el archivo `google-services.json` de tu proyecto Firebase.
3. El proyecto usa Kotlin 1.9+ y Gradle 8.5+.

### Web
1. Navega a la carpeta `/web`.
2. Instala: `npm install`.
3. Configura las variables en `.env.local` (Firebase Client & Admin SDK).
4. Lanza: `npm run dev`.

---

## 📄 Licencia

Este proyecto está bajo la Licencia de Radio CCBES. Todos los derechos reservados.

