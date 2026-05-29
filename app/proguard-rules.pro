# Add project specific ProGuard rules here.
#
# Gson + מודלים (שדות חייבים להישמר ל-deserialization)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# כל המודלים ש-Gson ממיר מ-Appwrite
-keep class com.example.cars.model.** { *; }

# שורות stack
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
