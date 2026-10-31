# Pokétch Wear OS Production R8 / ProGuard Configuration

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

# 4. Wear OS Tiles, Watch Face Complications, & Health Services
-keep class * extends androidx.wear.tiles.TileService { *; }
-keep class * extends androidx.wear.watchface.complications.datasource.ComplicationDataSourceService { *; }
-keep class * extends androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService { *; }
-keep class com.poketch.wear.tile.** { *; }
-keep class com.poketch.wear.complication.** { *; }
-keep class com.poketch.wear.health.** { *; }
-keep class com.poketch.wear.reminders.** { *; }

# 5. Kotlin Enums reflection preservation
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
