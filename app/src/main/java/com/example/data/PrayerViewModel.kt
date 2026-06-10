package com.example.data

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = PrayerRepository(database.prayerDao())

    // 1. Core Configurable States
    private val _selectedCity = MutableStateFlow(PrayerTimesCalculator.preconfiguredCities[0]) // Makkah
    val selectedCity: StateFlow<PrayerTimesCalculator.CityConfig> = _selectedCity.asStateFlow()

    private val _currentLatitude = MutableStateFlow(21.4225)
    val currentLatitude: StateFlow<Double> = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(39.8262)
    val currentLongitude: StateFlow<Double> = _currentLongitude.asStateFlow()

    private val _juristicRule = MutableStateFlow(PrayerTimesCalculator.JuristicRule.STANDARD)
    val juristicRule: StateFlow<PrayerTimesCalculator.JuristicRule> = _juristicRule.asStateFlow()

    // 2. Computed Prayer Timings
    private val _prayerTimings = MutableStateFlow<PrayerTimesCalculator.PrayerTimings?>(null)
    val prayerTimings: StateFlow<PrayerTimesCalculator.PrayerTimings?> = _prayerTimings.asStateFlow()

    // 3. Silent Settings from Room DB
    val dbSettings: StateFlow<List<PrayerSetting>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Prayer logs
    private val _currentDateKey = MutableStateFlow(getCurrentDateString())
    val currentDateKey: StateFlow<String> = _currentDateKey.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<PrayerLog>>(emptyList())
    val dailyLogs: StateFlow<List<PrayerLog>> = _dailyLogs.asStateFlow()

    // 5. Dynamic nearby mosques
    private val _nearbyMosques = MutableStateFlow<List<LocationHelper.Mosque>>(emptyList())
    val nearbyMosques: StateFlow<List<LocationHelper.Mosque>> = _nearbyMosques.asStateFlow()

    // 6. Next Prayer and Countdown State
    private val _nextPrayerName = MutableStateFlow("...")
    val nextPrayerName: StateFlow<String> = _nextPrayerName.asStateFlow()

    private val _nextPrayerTimeText = MutableStateFlow("00:00")
    val nextPrayerTimeText: StateFlow<String> = _nextPrayerTimeText.asStateFlow()

    private val _countdownText = MutableStateFlow("00:00:00")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    // 7. Active Silent Simulator State
    private val _isCurrentlySilenced = MutableStateFlow(false)
    val isCurrentlySilenced: StateFlow<Boolean> = _isCurrentlySilenced.asStateFlow()

    private val _silencedPrayerName = MutableStateFlow("")
    val silencedPrayerName: StateFlow<String> = _silencedPrayerName.asStateFlow()

    private val _silenceTimeRemainingText = MutableStateFlow("")
    val silenceTimeRemainingText: StateFlow<String> = _silenceTimeRemainingText.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            // Pre-seed Room db default settings if needed
            repository.prepopulateDefaultSettingsIfNeeded()
            
            // Recalculate everything initially
            recalculatePrayerTimes()
            updateNearbyMosques()
            startCountdownTimer()
            checkSilentPrefsState()
        }

        // Keep observing logs for current date
        viewModelScope.launch {
            _currentDateKey.collect { dateKey ->
                repository.getLogsForDate(dateKey).collect { logs ->
                    _dailyLogs.value = logs
                }
            }
        }
    }

    // Call this whenever location, city, date, or juristic rule changes
    fun recalculatePrayerTimes() {
        val calendar = Calendar.getInstance()
        val city = _selectedCity.value
        
        val timings = PrayerTimesCalculator.calculateTimes(
            calendar = calendar,
            latitude = _currentLatitude.value,
            longitude = _currentLongitude.value,
            timezoneOffset = city.timezone,
            method = city.method,
            juristicRule = _juristicRule.value
        )
        _prayerTimings.value = timings
    }

    fun selectCity(city: PrayerTimesCalculator.CityConfig) {
        _selectedCity.value = city
        _currentLatitude.value = city.latitude
        _currentLongitude.value = city.longitude
        recalculatePrayerTimes()
        updateNearbyMosques()
    }

    fun updateJuristicRule(rule: PrayerTimesCalculator.JuristicRule) {
        _juristicRule.value = rule
        recalculatePrayerTimes()
    }

    fun updateCoordinates(latitude: Double, longitude: Double) {
        _currentLatitude.value = latitude
        _currentLongitude.value = longitude
        recalculatePrayerTimes()
        updateNearbyMosques()
    }

    private fun updateNearbyMosques() {
        viewModelScope.launch {
            // Generate customized mosques based on user location
            val mosques = LocationHelper.getNearbyMosques(
                _currentLatitude.value,
                _currentLongitude.value,
                _selectedCity.value.nameEn
            )
            _nearbyMosques.value = mosques
        }
    }

    fun generateMosquesAroundCurrentCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val mosques = LocationHelper.generateDynamicNearbyMosques(latitude, longitude)
            _nearbyMosques.value = mosques
        }
    }

    // Toggle and save custom settings in DB
    fun updatePrayerSetting(setting: PrayerSetting) {
        viewModelScope.launch {
            repository.saveSetting(setting)
        }
    }

    // Ticks the completion log for a prayer item
    fun togglePrayerCompleted(prayerNameEn: String, currentStatus: String) {
        viewModelScope.launch {
            val newStatus = when (currentStatus) {
                "not_prayed" -> "prayed"
                "prayed" -> "prayed_in_mosque"
                else -> "not_prayed"
            }
            repository.logPrayer(
                PrayerLog(
                    dateKey = _currentDateKey.value,
                    prayerNameEn = prayerNameEn,
                    status = newStatus
                )
            )
        }
    }

    // Background Scheduler to test out Silent Mode alarms instantly
    fun triggerDemoSilentAlarm(prayerName: String, durationSeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Intent to trigger MUTE
        val muteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
            action = PrayerSilenceReceiver.ACTION_MUTE_DEVICE
            putExtra("prayer_name", prayerName)
            putExtra("duration", (durationSeconds / 60))
        }
        val mutePending = PendingIntent.getBroadcast(
            context,
            1201,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to trigger UNMUTE
        val unmuteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
            action = PrayerSilenceReceiver.ACTION_UNMUTE_DEVICE
        }
        val unmutePending = PendingIntent.getBroadcast(
            context,
            1202,
            unmuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Run MUTE instantly
        context.sendBroadcast(muteIntent)

        // Set Alarm to UNMUTE in durationSeconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (durationSeconds * 1000),
                unmutePending
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (durationSeconds * 1000),
                unmutePending
            )
        }
    }

    // Schedule actual silent mode for a future prayer time
    fun scheduleSilentMode(prayerNameAr: String, hour: Int, minute: Int, durationMin: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If target is already in the past today, schedule it for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Trigger muting alarm
        val muteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
            action = PrayerSilenceReceiver.ACTION_MUTE_DEVICE
            putExtra("prayer_name", prayerNameAr)
            putExtra("duration", durationMin)
        }
        val mutePending = PendingIntent.getBroadcast(
            context,
            hour * 100 + minute,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis,
            mutePending
        )

        // Trigger un-muting alarm durationMin later
        val unmuteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
            action = PrayerSilenceReceiver.ACTION_UNMUTE_DEVICE
            putExtra("prayer_name", prayerNameAr)
        }
        val unmutePending = PendingIntent.getBroadcast(
            context,
            (hour * 100 + minute) + 999,
            unmuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis + (durationMin * 60 * 1000),
            unmutePending
        )
    }

    fun checkSilentPrefsState() {
        val prefs = context.getSharedPreferences("silent_pray_prefs", Context.MODE_PRIVATE)
        val isSilenced = prefs.getBoolean("is_currently_silenced", false)
        val endTimestamp = prefs.getLong("silence_end_timestamp", 0)

        if (isSilenced && System.currentTimeMillis() < endTimestamp) {
            _isCurrentlySilenced.value = true
            _silencedPrayerName.value = prefs.getString("current_silenced_prayer", "الصلاة") ?: "الصلاة"
            
            val remainingSec = ((endTimestamp - System.currentTimeMillis()) / 1000).toInt()
            val remMin = remainingSec / 60
            val remSec = remainingSec % 60
            _silenceTimeRemainingText.value = String.format("%02d:%02d", remMin, remSec)
        } else {
            _isCurrentlySilenced.value = false
            _silencedPrayerName.value = ""
            _silenceTimeRemainingText.value = ""
        }
    }

    fun forceEndSilencing() {
        val intent = Intent(context, PrayerSilenceReceiver::class.java).apply {
            action = PrayerSilenceReceiver.ACTION_UNMUTE_DEVICE
        }
        context.sendBroadcast(intent)
        checkSilentPrefsState()
    }

    // Core continuous ticker for Next Prayer Countdown and Silent Simulator updates
    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                checkSilentPrefsState()
                updateNextPrayerState()
                delay(1000)
            }
        }
    }

    private fun updateNextPrayerState() {
        val timings = _prayerTimings.value ?: return
        val now = Calendar.getInstance()
        val curMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val curSeconds = now.get(Calendar.SECOND)

        // List of prayers: nameEn, nameAr, minutes of day
        val prayerList = listOf(
            Triple("Fajr", "الفجر", timings.fajrMinutes),
            Triple("Sunrise", "الشروق", timings.sunriseMinutes),
            Triple("Dhuhr", "الظهر", timings.dhuhrMinutes),
            Triple("Asr", "العصر", timings.asrMinutes),
            Triple("Maghrib", "المغرب", timings.maghribMinutes),
            Triple("Isha", "العشاء", timings.ishaMinutes)
        )

        // Find the next prayer
        var nextPrayer = prayerList.firstOrNull { it.third > curMinutes }
        val isNextDay = nextPrayer == null

        if (isNextDay) {
            // Next is Fajr tomorrow
            nextPrayer = prayerList.first()
        }

        val nameAr = nextPrayer!!.second
        val targetMinutes = nextPrayer.third
        
        // Calculate raw remaining seconds
        val remainingSeconds = if (isNextDay) {
            val tillMidnight = (1440 - curMinutes) * 60 - curSeconds
            val afterMidnight = targetMinutes * 60
            tillMidnight + afterMidnight
        } else {
            (targetMinutes - curMinutes) * 60 - curSeconds
        }

        _nextPrayerName.value = nameAr
        _nextPrayerTimeText.value = when (nextPrayer.first) {
            "Fajr" -> timings.fajr
            "Sunrise" -> timings.sunrise
            "Dhuhr" -> timings.dhuhr
            "Asr" -> timings.asr
            "Maghrib" -> timings.maghrib
            else -> timings.isha
        }

        val hrs = remainingSeconds / 3600
        val mns = (remainingSeconds % 3600) / 60
        val scs = remainingSeconds % 60
        _countdownText.value = String.format("%02d:%02d:%02d", hrs, mns, scs)
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
