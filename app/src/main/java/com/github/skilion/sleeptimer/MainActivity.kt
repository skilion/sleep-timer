package com.github.skilion.sleeptimer

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.devadvance.circularseekbar.CircularSeekBar


enum class AppState {
    STOP,
    RUNNING
}

const val NOTIFICATIONS_CHANNEL_ID = "timer_channel"

class MainActivity : AppCompatActivity() {
    private var appState = AppState.STOP
    private var timer: Timer? = null // timer to update timeText during the countdown

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
                this@MainActivity.updateUI();
            }
            override fun onStopTrackingTouch(circularSeekBar: CircularSeekBar?) {}
            override fun onStartTrackingTouch(circularSeekBar: CircularSeekBar?) {}
        })

        if (countdownServiceRunning) {
            appState = AppState.RUNNING
            startButton.setText(R.string.stop)
            startUpdateTimer()
        }

        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdateTimer()
    }
    private fun onStartButtonClick() {
        when (appState) {
            AppState.STOP -> {
                appState = AppState.RUNNING
                startButton.setText(R.string.stop)

                // start the countdown service
                val timeout = Date().time + (this.timeSeekBar.progress + 1) * 5 * 60 * 1000
                val intent = Intent(this, CountdownService::class.java)
                intent.putExtra("timeout", timeout)
                startService(intent)

                startUpdateTimer()
            }
            AppState.RUNNING -> {
                appState = AppState.STOP
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
        val checkedItems = booleanArrayOf(true, false, false)
        builder.setMultiChoiceItems(items, checkedItems, null)
        builder.setPositiveButton(getString(android.R.string.ok), null)
        builder.setNegativeButton(getString(android.R.string.cancel), null)
        val alert = builder.create()
        alert.show()
    }

    private fun updateUI() {
        if (!countdownServiceRunning) {
            val timerMinutes = (this.timeSeekBar.progress + 1) * 5;
            val progressText = timerMinutes.toString() + " " + getString(R.string.minutes)
            timeText.text = progressText
        } else {
            timeText.text = remainingTimeText
            timeSeekBar.progress = remainingSeconds / 60 / 5
        }
    }

    private fun startUpdateTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // only the UI thread can edit the activity appearance
                this@MainActivity.runOnUiThread {
                    updateUI()
                }
            }
        }, 100, 1000)
    }

    private fun stopUpdateTimer() {
        timer?.cancel()
        timer = null
    }
}
