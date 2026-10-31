# Pokétch Mobile Production R8 / ProGuard Configuration

# 1. Room Persistence: @Entity, @Dao, and RoomDatabase classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.poketch.core.data.local.entity.** { *; }
-keep class com.poketch.core.data.local.dao.** { *; }
-keep class com.poketch.core.data.local.db.** { *; }

# 2. Pokémon 151 Seed Serialization & Domain Models
-keep class com.poketch.core.data.local.seed.** { *; }
-keep class com.poketch.core.domain.model.** { *; }
-keep class com.poketch.core.domain.calculator.** { *; }
-keep class com.poketch.core.domain.engine.** { *; }

# 3. Wearable Data Layer WearableListenerService entrypoints
-keep class * extends com.google.android.gms.wearable.WearableListenerService { *; }
-keep class com.poketch.core.sync.BasePoketchWearableListenerService { *; }
-keep class com.poketch.core.sync.** { *; }

# 4. Kotlin Enums reflection preservation
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
