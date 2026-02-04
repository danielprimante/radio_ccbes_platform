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
1.  **Activador:** Acciones en el panel web o procesos de backend.
2.  **Transmisor:** OneSignal SDK para segmentación de usuarios (Tags: "noticias", "radio_live").
3.  **Receptor:** Implementación de `OSRemoteNotificationReceivedHandler` en Android para gestionar visualización personalizada.

---

## 5. Mantenimiento y Despliegue

### Requisitos de Desarrollo
- **Android:** JDK 17, Android Studio Ladybug+.
- **Web:** Node.js 18+, NPM 9+.
- **Firebase:** Proyecto activo con Firestore, Auth (Email/Google) y Storage habilitados.

