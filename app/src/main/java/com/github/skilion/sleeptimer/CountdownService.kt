package com.github.skilion.sleeptimer

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import java.util.*

var countdownServiceRunning = false
var remainingTimeText = "--:--:--"
var remainingSeconds = 0

class CountdownService : Service() {
    private var timeout = Date(0)
    private val timer = Timer() // timer to update the notification during the countdown
    private val notificationId = 1
    private val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL_ID)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timeout = Date(intent?.getLongExtra("timeout", 0) ?: 0)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                update()
            }
        }, 0, 1000)
        return START_REDELIVER_INTENT
    }

    override fun onBind(p0: Intent?): IBinder {
        TODO("not implemented")
    }

    override fun onCreate() {
        countdownServiceRunning = true
        createNotification()
    }

    override fun onDestroy() {
        countdownServiceRunning = false
        timer.cancel()
        deleteNotification()
    }

    private fun update() {
        val seconds = (timeout.time - Date().time).toInt() / 1000
        if (seconds >= 0) {
            // build time string of the remaining time
            val remainingTime = StringBuilder()
            if (seconds / 3600 > 0) {
                remainingTime.append(seconds / 3600)
                remainingTime.append(":")
            }
            remainingTime.append(String.format("%02d:%02d", seconds / 60 % 60, seconds % 60))

            // update global time variables
            remainingTimeText = remainingTime.toString()
            remainingSeconds = seconds

            updateNotification()
        } else {
            // timer endend
            if (SETTING_STOP_MUSIC) sendMediaButton(KeyEvent.KEYCODE_MEDIA_STOP)
            if (SETTING_TURN_OFF_BLUETOOTH) disableBluetooth()
            if (SETTING_TURN_OFF_WIFI) disableWifi()
            stopSelf()
        }
    }

    private fun updateNotification() {
        notificationBuilder.setContentTitle(remainingTimeText)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun createNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        with(notificationBuilder) {
            setSmallIcon(R.drawable.baseline_music_off_24)
            setContentIntent(pendingIntent)
        }
        startForeground(notificationId, notificationBuilder.build())
    }

    private fun deleteNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(notificationId)
        }
    }

    private fun sendMediaButton(keyCode: Int) {
        var keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        var intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        sendBroadcast(intent)

        keyEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        sendBroadcast(intent)
    }

    private fun disableBluetooth() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.disable()
    }

    private fun disableWifi() {
        val wifiManager = applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE)
        if (wifiManager is WifiManager) {
            wifiManager.setWifiEnabled(false)
        }
    }
}