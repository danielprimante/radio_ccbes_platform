# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\danie\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep OneSignal classes
-keep class com.onesignal.** { *; }

# Keep Media3/ExoPlayer
-keep class androidx.media3.** { *; }

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep Models
-keep class com.radio.ccbes.data.model.** { *; }

# Standard R8/ProGuard rules
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes *Annotation*
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
