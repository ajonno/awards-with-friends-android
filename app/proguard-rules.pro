# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes for Firebase/Firestore serialization
-keepclassmembers class com.aamsco.awardswithfriends.data.model.** {
    *;
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
}

# Keep generic signatures for Firestore
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin serialization
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }
