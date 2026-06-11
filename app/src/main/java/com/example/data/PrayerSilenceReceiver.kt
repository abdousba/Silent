package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerSilenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val duration = intent.getIntExtra("duration", 20)

        Log.d("PrayerSilenceReceiver", "Received action: $action for prayer: $prayerName")

        createNotificationChannel(context, notificationManager)

        // Handle System Boot Completed to re-schedule all alarms
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("PrayerSilenceReceiver", "System boot completed. Rescheduling all prayer alarms...")
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    rescheduleAlarmsAfterReboot(context)
                } catch (e: Exception) {
                    Log.e("PrayerSilenceReceiver", "Reboot reschedule failed: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Handle Prayer Time Alarm for notification/Adhan alert and auto-mute scheduling
        if (action == ACTION_PRAYER_ALARM) {
            val prayerAr = intent.getStringExtra("prayer_name_ar") ?: "الصلاة"
            val prayerEn = intent.getStringExtra("prayer_name_en") ?: "Fajr"
            Log.d("PrayerSilenceReceiver", "Prayer alarm triggered for: $prayerAr ($prayerEn)")
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.prayerDao()
                    
                    val allSettings = dao.getAllSettings().first()
                    val prayerSetting = allSettings.find { it.prayerNameEn.equals(prayerEn, ignoreCase = true) }
                    
                    val isAutoSilent = prayerSetting?.isAutoSilent ?: true
                    val isAlertEnabled = prayerSetting?.isAlertEnabled ?: true
                    val silenceDuration = prayerSetting?.silenceDurationMinutes ?: 20
                    
                    val sharedPrefs = context.getSharedPreferences("silent_pray_prefs", Context.MODE_PRIVATE)
                    val isAdhanEnabled = sharedPrefs.getBoolean("prefs_adhan_enabled", true)
                    
                    if (isAdhanEnabled && isAlertEnabled) {
                        showPrayerTimeAlert(context, notificationManager, prayerAr)
                    }
                    
                    if (isAutoSilent) {
                        // Deliver real mute broadcast command
                        val muteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
                            setAction(ACTION_MUTE_DEVICE)
                            putExtra("prayer_name", prayerAr)
                            putExtra("duration", silenceDuration)
                        }
                        context.sendBroadcast(muteIntent)
                        
                        // Schedule actual unmuting duration min later
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val unmuteIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
                            setAction(ACTION_UNMUTE_DEVICE)
                            putExtra("prayer_name", prayerAr)
                        }
                        val unmutePending = PendingIntent.getBroadcast(
                            context,
                            4000,
                            unmuteIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        val unmuteTime = System.currentTimeMillis() + (silenceDuration * 60 * 1000L)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                unmuteTime,
                                unmutePending
                            )
                        } else {
                            alarmManager.set(
                                android.app.AlarmManager.RTC_WAKEUP,
                                unmuteTime,
                                unmutePending
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PrayerSilenceReceiver", "Failed handling prayer alarm: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        when (action) {
            ACTION_MUTE_DEVICE -> {
                // Try to put device in Silent/Vibrate mode
                var isHardwareMuted = false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            isHardwareMuted = true
                        }
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        isHardwareMuted = true
                    }
                } catch (e: Exception) {
                    Log.e("PrayerSilenceReceiver", "Failed to mute device hardware: ${e.message}")
                }

                // Post a status notification
                val statusText = if (isHardwareMuted) {
                    "تم تفعيل الوضع الصامت تلقائياً لصلاة $prayerName لمدة $duration دقيقة"
                } else {
                    "حان وقت صلاة $prayerName! يرجى خفض الصوت (الوضع الصامت مفعل داخل التطبيق)"
                }

                showNotification(
                    context, 
                    notificationManager, 
                    NOTIFICATION_ID_SILENT, 
                    "الوضع الصامت مفعل 🌙", 
                    statusText
                )

                // Save status in SharedPrefs for immediate UI updates
                val prefs = context.getSharedPreferences("silent_pray_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("is_currently_silenced", true)
                    putString("current_silenced_prayer", prayerName)
                    putLong("silence_end_timestamp", System.currentTimeMillis() + (duration * 60 * 1000))
                    apply()
                }

                // Send a local broadcast to notify MainActivity if it is running
                val updateIntent = Intent(ACTION_UI_UPDATE).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            }

            ACTION_UNMUTE_DEVICE -> {
                // Try to restore sound
                var isHardwareRestored = false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            isHardwareRestored = true
                        }
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        isHardwareRestored = true
                    }
                } catch (e: Exception) {
                    Log.e("PrayerSilenceReceiver", "Failed to restore sound: ${e.message}")
                }

                // Reset SharedPrefs state
                val prefs = context.getSharedPreferences("silent_pray_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("is_currently_silenced", false)
                    putString("current_silenced_prayer", null)
                    putLong("silence_end_timestamp", 0)
                    apply()
                }

                // Post completion notification
                val alertText = "تقبل الله صلاتكم. تم إلغاء الوضع الصامت وإعادة تفعيل رنين الهاتف ✨"
                showNotification(
                    context, 
                    notificationManager, 
                    NOTIFICATION_ID_UNMUTED, 
                    "انتهت الصلاة | تقبل الله 🤲", 
                    alertText
                )

                // Send a local broadcast to notify MainActivity
                val updateIntent = Intent(ACTION_UI_UPDATE).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }

    private fun showPrayerTimeAlert(
        context: Context,
        notificationManager: NotificationManager,
        prayerNameAr: String
    ) {
        val channelId = "adhan_alerts_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات أذان الصلوات 🕌",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة تنبيهات موعد الأذان للصلوات الخمس"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1500,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        
        try {
            val r = android.media.RingtoneManager.getRingtone(context, soundUri)
            r.play()
        } catch (e: Exception) {
            Log.e("PrayerSilenceReceiver", "Could not play notification sound: ${e.message}")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("نداء الصلاة 🕌")
            .setContentText("حان الآن موعد أذان صلاة $prayerNameAr في منطقتك")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setStyle(NotificationCompat.BigTextStyle().bigText("الله أكبر، الله أكبر... حان الآن موعد أذان صلاة $prayerNameAr في موقعك الحالي. تقبل الله منا ومنكم صالح الأعمال."))

        notificationManager.notify(NOTIFICATION_ID_ADHAN, builder.build())
    }

    private fun rescheduleAlarmsAfterReboot(context: Context) {
        val cityPrefs = context.getSharedPreferences("saved_city_prefs", Context.MODE_PRIVATE)
        if (!cityPrefs.contains("city_name_en")) return
        
        val nameAr = cityPrefs.getString("city_name_ar", "مكة المكرمة") ?: "مكة المكرمة"
        val nameEn = cityPrefs.getString("city_name_en", "Makkah") ?: "Makkah"
        val latitude = cityPrefs.getFloat("city_latitude", 21.4225f).toDouble()
        val longitude = cityPrefs.getFloat("city_longitude", 39.8262f).toDouble()
        val timezone = cityPrefs.getFloat("city_timezone", 3.0f).toDouble()
        val methodName = cityPrefs.getString("city_method", PrayerTimesCalculator.CalculationMethod.UMM_AL_QURA.name)
        val method = try {
            PrayerTimesCalculator.CalculationMethod.valueOf(methodName ?: "UMM_AL_QURA")
        } catch (e: Exception) {
            PrayerTimesCalculator.CalculationMethod.UMM_AL_QURA
        }

        val calendar = java.util.Calendar.getInstance()
        val timings = PrayerTimesCalculator.calculateTimes(
            calendar = calendar,
            latitude = latitude,
            longitude = longitude,
            timezoneOffset = timezone,
            method = method,
            juristicRule = PrayerTimesCalculator.JuristicRule.STANDARD
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val prayers = listOf(
            Triple("الفجر", "Fajr", timings.fajrMinutes),
            Triple("الظهر", "Dhuhr", timings.dhuhrMinutes),
            Triple("العصر", "Asr", timings.asrMinutes),
            Triple("المغرب", "Maghrib", timings.maghribMinutes),
            Triple("العشاء", "Isha", timings.ishaMinutes)
        )

        val todayYear = calendar.get(java.util.Calendar.YEAR)
        val todayMonth = calendar.get(java.util.Calendar.MONTH)
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        prayers.forEachIndexed { index, (pAr, pEn, minutes) ->
            val alarmTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, todayYear)
                set(java.util.Calendar.MONTH, todayMonth)
                set(java.util.Calendar.DAY_OF_MONTH, todayDay)
                set(java.util.Calendar.HOUR_OF_DAY, minutes / 60)
                set(java.util.Calendar.MINUTE, minutes % 60)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (alarmTime.timeInMillis <= System.currentTimeMillis()) {
                alarmTime.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val alarmIntent = Intent(context, PrayerSilenceReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM
                putExtra("prayer_name_ar", pAr)
                putExtra("prayer_name_en", pEn)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                3000 + index,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    alarmTime.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Int,
        title: String,
        body: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        notificationManager.notify(id, builder.build())
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الصلاة الصامتة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة تنبيهات كتم الصوت وإلغائه بعد انتهاء وقت الصلاة"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "silent_pray_channel"
        const val NOTIFICATION_ID_SILENT = 1001
        const val NOTIFICATION_ID_UNMUTED = 1002
        const val NOTIFICATION_ID_ADHAN = 1003

        const val ACTION_MUTE_DEVICE = "com.example.silentpray.ACTION_MUTE"
        const val ACTION_UNMUTE_DEVICE = "com.example.silentpray.ACTION_UNMUTE"
        const val ACTION_PRAYER_ALARM = "com.example.silentpray.ACTION_PRAYER_ALARM"
        const val ACTION_UI_UPDATE = "com.example.silentpray.ACTION_UI_UPDATE"
    }
}
