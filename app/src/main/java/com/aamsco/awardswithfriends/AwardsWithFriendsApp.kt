package com.aamsco.awardswithfriends

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aamsco.awardswithfriends.service.AWFFirebaseMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AwardsWithFriendsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            AWFFirebaseMessagingService.CHANNEL_ID,
            "Competition Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about competition activity, winners, and voting"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
