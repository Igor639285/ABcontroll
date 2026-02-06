# Keep serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$Companion { *; }
-keepclasseswithmembers class ** { @kotlinx.serialization.Serializable *; }
