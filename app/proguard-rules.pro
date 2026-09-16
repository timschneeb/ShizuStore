# kotlinx.serialization
# The runtime artifacts ship their own consumer rules for @Serializable
# types; this only covers the app's generated serializers explicitly.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclasseswithmembers class me.timschneeberger.shizustore.** {
    kotlinx.serialization.KSerializer serializer(...);
}
