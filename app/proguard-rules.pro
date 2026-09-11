# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# WorkManager + HiltWorker
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# Komponen manifest (service/receiver)
-keep class com.optimizer.android.LocalFirewallService { *; }
-keep class com.optimizer.android.BlackholeNotificationService { *; }
-keep class com.optimizer.android.HibernationService { *; }
-keep class com.optimizer.android.WorkProfileReceiver { *; }
-keep class com.optimizer.android.PackageRemoveReceiver { *; }

# Media3 / CameraX reflection
-dontwarn androidx.media3.**
-keep class androidx.media3.common.util.** { *; }
-keep class androidx.camera.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, SourceFile, LineNumberTable
-dontwarn kotlin.**
