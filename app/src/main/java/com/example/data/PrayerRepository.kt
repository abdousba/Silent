package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class PrayerRepository(private val prayerDao: PrayerDao) {

    val allSettings: Flow<List<PrayerSetting>> = prayerDao.getAllSettings()
    val bookmarkedMosques: Flow<List<MosqueBookmark>> = prayerDao.getAllBookmarkedMosques()
    val allLogs: Flow<List<PrayerLog>> = prayerDao.getAllLogs()

    suspend fun getLogsForDate(dateKey: String): Flow<List<PrayerLog>> {
        return prayerDao.getLogsForDate(dateKey)
    }

    suspend fun saveSetting(setting: PrayerSetting) {
        prayerDao.insertOrUpdateSetting(setting)
    }

    suspend fun logPrayer(log: PrayerLog) {
        prayerDao.insertOrUpdateLog(log)
    }

    suspend fun addBookmark(bookmark: MosqueBookmark) {
        prayerDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmark: MosqueBookmark) {
        prayerDao.deleteBookmark(bookmark)
    }

    suspend fun prepopulateDefaultSettingsIfNeeded() {
        val currentSettings = prayerDao.getAllSettings().firstOrNull()
        if (currentSettings.isNullOrEmpty()) {
            val defaults = listOf(
                PrayerSetting("fajr", isAutoSilent = true, silenceDurationMinutes = 20, isAlertEnabled = true),
                PrayerSetting("dhuhr", isAutoSilent = true, silenceDurationMinutes = 20, isAlertEnabled = true),
                PrayerSetting("asr", isAutoSilent = true, silenceDurationMinutes = 20, isAlertEnabled = true),
                PrayerSetting("maghrib", isAutoSilent = true, silenceDurationMinutes = 20, isAlertEnabled = true),
                PrayerSetting("isha", isAutoSilent = true, silenceDurationMinutes = 20, isAlertEnabled = true)
            )
            prayerDao.insertAllSettings(defaults)
        }
    }
}
