# Hardened R8 Optimization & Obfuscation Rules for Telefam
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 't.c.o'
-overloadaggressively
-mergeinterfacesaggressively
-renamesourcefileattribute SourceFile
-keepattributes Signature,*Annotation*

# Strip all verbose/debug/info/warning logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int wtf(...);
}

# Keep only the JNI bridge entry points invoked by libtelefam_core.so
-keep class com.telefam.company.app.nativecore.TelefamNativeCore {
    native <methods>;
}

-keepclassmembers class com.telefam.company.app.nativecore.TelefamNativeCore {
    public static *;
}

# Keep Compose / Android entry points referenced by manifest
-keep public class com.telefam.company.app.TelefamApplication
-keep public class com.telefam.company.app.MainActivity

# OkHttp / Conscrypt / Credential Manager standard rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class androidx.credentials.** { *; }
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class com.telefam.company.app.data.TelefamDatabase_Impl {
    <init>(...);
}
