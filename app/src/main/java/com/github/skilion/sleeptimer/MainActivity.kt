package com.github.skilion.sleeptimer

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.devadvance.circularseekbar.CircularSeekBar


const val NOTIFICATIONS_CHANNEL_ID = "timer_channel"

var SETTING_STOP_MUSIC = true
var SETTING_TURN_OFF_BLUETOOTH = true
var SETTING_TURN_OFF_WIFI = true

class MainActivity : AppCompatActivity() {
    private var timer: Timer? = null // timer to update timeText during the countdown
    private var preferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATIONS_CHANNEL_ID, "Timer", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        startButton.setOnClickListener { this.onStartButtonClick() }
        settingsButton.setOnClickListener { this.onSettingsButtonClick() }
        timeSeekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Int, fromUser: Boolean) {
                this@MainActivity.updateUI()
            }
            override fun onStopTrackingTouch(circularSeekBar: CircularSeekBar?) {}
            override fun onStartTrackingTouch(circularSeekBar: CircularSeekBar?) {}
        })

        if (countdownServiceRunning) {
            startButton.setText(R.string.stop)
            startUpdateTimer()
        }

        updateUI()

        preferences = this.getPreferences(Context.MODE_PRIVATE)
        SETTING_STOP_MUSIC = preferences?.getBoolean("stop_music", true) ?: true
        SETTING_TURN_OFF_BLUETOOTH = preferences?.getBoolean("turn_off_bluetooth", false) ?: false
        SETTING_TURN_OFF_WIFI = preferences?.getBoolean("turn_off_wifi", false) ?: false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdateTimer()
    }

    private fun onStartButtonClick() {
        when (countdownServiceRunning) {
            false -> {
                startButton.setText(R.string.stop)

                // start the countdown service
                val timeout = Date().time + (this.timeSeekBar.progress + 1) * 5 * 60 * 1000
                val intent = Intent(this, CountdownService::class.java)
                intent.putExtra("timeout", timeout)
                startService(intent)

                startUpdateTimer()
            }
            true -> {
                startButton.setText(R.string.start)
                stopService(Intent(this, CountdownService::class.java))
                stopUpdateTimer()
            }
        }
    }

    private fun onSettingsButtonClick() {
        val builder = AlertDialog.Builder(this)
        val items = arrayOf(
                getString(R.string.stop_music),
                getString(R.string.turn_off_bluetooth),
                getString(R.string.turn_off_wifi)
        )
        val checkedItems = booleanArrayOf(
                SETTING_STOP_MUSIC,
                SETTING_TURN_OFF_BLUETOOTH,
                SETTING_TURN_OFF_WIFI
        )
        builder.setMultiChoiceItems(items, checkedItems) { _, option, value ->
            when (option) {
                0 -> { SETTING_STOP_MUSIC = value }
                1 -> { SETTING_TURN_OFF_BLUETOOTH = value }
                2 -> { SETTING_TURN_OFF_WIFI = value }
            }
        }
        builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            if (preferences != null) {
                with(preferences!!.edit()) {
                    putBoolean("stop_music", SETTING_STOP_MUSIC)
                    putBoolean("turn_off_bluetooth", SETTING_TURN_OFF_BLUETOOTH)
                    putBoolean("turn_off_wifi", SETTING_TURN_OFF_WIFI)
                    apply()
                }
            }
        }
        val alert = builder.create()
        alert.show()
    }

    private fun updateUI() {
        if (!countdownServiceRunning) {
            val timerMinutes = (this.timeSeekBar.progress + 1) * 5
            val progressText = timerMinutes.toString() + " " + getString(R.string.minutes)
            timeText.text = progressText
            startButton.setText(R.string.start)
        } else {
            timeText.text = remainingTimeText
            timeSeekBar.progress = remainingSeconds / 60 / 5
            startButton.setText(R.string.stop)
        }
    }

    private fun startUpdateTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // only the UI thread can edit the activity appearance
                this@MainActivity.runOnUiThread {
                    updateUI()
                    if (!countdownServiceRunning) {
                        stopUpdateTimer()
                    }
                }
            }
        }, 100, 1000)
    }

    private fun stopUpdateTimer() {
        timer?.cancel()
        timer = null
    }
}
