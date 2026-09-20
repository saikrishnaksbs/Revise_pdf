# PdfBox-Android reflectively loads font/encoding resources; keep its classes intact.
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-keep class androidx.room.** { *; }
