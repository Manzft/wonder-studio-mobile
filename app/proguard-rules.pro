# Mantiene las anotaciones de kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
	*** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
	kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.manzft.wonderstudio.**$$serializer { *; }
-keepclassmembers class com.manzft.wonderstudio.** {
	*** Companion;
}
-keepclasseswithmembers class com.manzft.wonderstudio.** {
	kotlinx.serialization.KSerializer serializer(...);
}
