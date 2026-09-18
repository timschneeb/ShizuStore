# Keep original class and member names in release builds; shrinking still applies.
-dontobfuscate
# Keep frames and line numbers 1:1 so release stack traces match the source.
-dontoptimize

# kotlinx.serialization
# The runtime artifacts ship their own consumer rules for @Serializable
# types; this only covers the app's generated serializers explicitly.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclasseswithmembers class me.timschneeberger.shizustore.** {
    kotlinx.serialization.KSerializer serializer(...);
}
