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

class PrayerSilenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val duration = intent.getIntExtra("duration", 20)

        Log.d("PrayerSilenceReceiver", "Received action: $action for prayer: $prayerName")

        createNotificationChannel(context, notificationManager)

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

        const val ACTION_MUTE_DEVICE = "com.example.silentpray.ACTION_MUTE"
        const val ACTION_UNMUTE_DEVICE = "com.example.silentpray.ACTION_UNMUTE"
        const val ACTION_UI_UPDATE = "com.example.silentpray.ACTION_UI_UPDATE"
    }
}
