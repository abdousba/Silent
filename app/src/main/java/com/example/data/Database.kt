package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "prayer_settings")
data class PrayerSetting(
    @PrimaryKey val prayerNameEn: String, // "fajr", "dhuhr", "asr", "maghrib", "isha"
    val isAutoSilent: Boolean = true,
    val silenceDurationMinutes: Int = 20,
    val isAlertEnabled: Boolean = true
)

@Entity(tableName = "prayer_logs", primaryKeys = ["dateKey", "prayerNameEn"])
data class PrayerLog(
    val dateKey: String, // "YYYY-MM-DD"
    val prayerNameEn: String, // "fajr", "dhuhr", "asr", "maghrib", "isha"
    val status: String, // "not_prayed", "prayed", "prayed_in_mosque"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mosque_bookmarks")
data class MosqueBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

// 2. DAOs
@Dao
interface PrayerDao {
    // Settings
    @Query("SELECT * FROM prayer_settings")
    fun getAllSettings(): Flow<List<PrayerSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: PrayerSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSettings(settings: List<PrayerSetting>)

    // Logs
    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey")
    fun getLogsForDate(dateKey: String): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_logs")
    fun getAllLogs(): Flow<List<PrayerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: PrayerLog)

    // Mosques
    @Query("SELECT * FROM mosque_bookmarks ORDER BY addedTimestamp DESC")
    fun getAllBookmarkedMosques(): Flow<List<MosqueBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: MosqueBookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: MosqueBookmark)
}

// 3. Database
@Database(
    entities = [PrayerSetting::class, PrayerLog::class, MosqueBookmark::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "silent_pray_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
