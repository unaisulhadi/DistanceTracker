package com.hadi.distancetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.hadi.distancetracker.util.Constants.ACTION_SERVICE_START
import com.hadi.distancetracker.util.Constants.ACTION_SERVICE_STOP
import com.hadi.distancetracker.util.Constants.NOTIFICATION_CHANNEL_ID
import com.hadi.distancetracker.util.Constants.NOTIFICATION_CHANNEL_NAME
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService : LifecycleService() {

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    companion object {
        val started = MutableLiveData<Boolean>()
    }

    fun setInitialValues() {
        started.postValue(false)
    }


    override fun onCreate() {
        setInitialValues()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_SERVICE_START -> {
                    started.postValue(true)
                }
                ACTION_SERVICE_STOP -> {
                    started.postValue(false)
                    stopSelf()
                }
                else -> {}
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)

        }
    }

}
