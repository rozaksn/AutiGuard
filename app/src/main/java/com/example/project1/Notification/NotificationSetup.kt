package com.example.project1.Notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.project1.Homepage.MainActivity
import com.example.project1.R

class NotificationSetup(context:Context) {
    private var CHANNEL_ID = "channel_id_range"
    private val ctx = context
    val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun sendRangeNotification(){
        val intent = Intent(ctx,MainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(ctx,0, intent, flags)
        val notifyManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(ctx,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_circle_notifications)
            .setVibrate(longArrayOf(1000))
            .setSound(ringtone)
            .setContentText(ctx.getString(R.string.out_range))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,"out_range_notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(1000)

            notification.setChannelId(CHANNEL_ID)
            notifyManager.createNotificationChannel(notificationChannel)
        }
        notification.setAutoCancel(true)
        val notificationBuilder = notification.build()
        notificationBuilder.flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_ONLY_ALERT_ONCE
        notifyManager.notify(1,notificationBuilder)
    }

    fun sendLoudnessNotification(){
        val intent = Intent(ctx,MainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(ctx,0, intent, flags)
        val notifyManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(ctx,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_circle_notifications_red)
            .setVibrate(longArrayOf(1000))
            .setSound(ringtone)
            .setContentText(ctx.getString(R.string.loudness_notification))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,"loudness_notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(1000)

            notification.setChannelId(CHANNEL_ID)
            notifyManager.createNotificationChannel(notificationChannel)
        }
        notification.setAutoCancel(true)
        val notificationBuilder = notification.build()
        notificationBuilder.flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_ONLY_ALERT_ONCE
        notifyManager.notify(2,notificationBuilder)
    }
}