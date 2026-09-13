# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class me.timschneeberger.shizustore.data.sync.model.** {
    *** Companion;
}
-keepclasseswithmembers class me.timschneeberger.shizustore.data.sync.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class androidx.room.RoomDatabase { *; }
