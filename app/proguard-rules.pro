# Add project specific ProGuard rules here.
# Preserve line numbers and source file names for crash diagnostics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# @Keep annotations
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

# Kotlinx Serialization (Navigation Compose type-safe @Serializable routes)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontwarn kotlinx.serialization.json.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep,includedescriptorclasses class com.example.ui.*Route** { *; }
-keepclassmembers class com.example.ui.*Route**$Companion {
    *** serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static final <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep,includedescriptorclasses class <1>$$serializer { *; }

# Firebase Firestore models
-keepclassmembers class com.example.domain.model.** {
    public <init>(...);
    <fields>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# Room database entities, DAOs & converters
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class com.example.data.local.** { *; }

# Hilt / Dagger generated factories
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Media3 ExoPlayer, Transformer & Effects (shader/effect reflection)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Compose, Haze, Coil & Security libraries
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**
-dontwarn coil.**
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**

# Google Play Services / Credential Manager / Coroutines
-dontwarn com.google.android.gms.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
