# Tupaz ProGuard Rules

# Preserve JNI native method signatures
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve pipeline bridge and model classes
-keep class com.tupaz.pipeline.** { *; }
-keep class com.tupaz.domain.** { *; }

# Strip debug log calls in release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
