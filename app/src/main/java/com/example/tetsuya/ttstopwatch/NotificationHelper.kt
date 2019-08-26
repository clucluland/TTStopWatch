/*
このファイルは追加しただけで利用してない(できてない)
 */
package com.example.tetsuya.ttstopwatch

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Notification周りのヘルパークラス.
 */
internal class NotificationHelper(context: Context?): ContextWrapper(context) {

    /**
     * グループ.
     */
    enum class Group(val resId: Int) {
        Push(R.string.notification_group_push),
        App(R.string.notification_group_app)
    }

    /**
     * 通知チャンネル.
     */
    enum class Channel(
            val resId: Int,
            val importance: Int,
            val color: Int,
            val visibility: Int,
            val group: Group
    ) {
        News(
                R.string.notification_channel_news,
                NotificationManager.IMPORTANCE_MIN,
                Color.GREEN,
                Notification.VISIBILITY_PRIVATE,
                Group.Push
        ),
        Important(
                R.string.notification_channel_important,
                NotificationManager.IMPORTANCE_HIGH,
                Color.BLUE,
                Notification.VISIBILITY_PRIVATE,
                Group.App
        )
    }

    companion object {
        private val notifyId = AtomicInteger(0)
    }

    private val manager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // グループ生成
            val createGroup: (Group) -> NotificationChannelGroup =
                    fun(group: Group) = NotificationChannelGroup(getString(group.resId), getString(group.resId))
            manager.createNotificationChannelGroups(arrayListOf(createGroup(Group.Push), createGroup(Group.App)))

            // チャンネル生成
            val createChannel: (Channel) -> NotificationChannel =
                    fun(channel: Channel) = NotificationChannel(
                            getString(channel.resId),
                            getString(channel.resId),
                            channel.importance).apply {
                        group = getString(channel.group.resId)
                    }
            manager.createNotificationChannels(arrayListOf(createChannel(Channel.News), createChannel(Channel.Important)))
        }
    }

    /**
     * Notificationをpostします.
     *
     * @param channel: Channel
     */
    fun post(channel: Channel) {
        val pending = TaskStackBuilder.create(this)
                .addNextIntent(Intent(this, MainActivity::class.java))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, getString(channel.resId)) // チャンネル指定
                .setGroup(getString(channel.group.resId)) // グループ指定
                .setColor(channel.color)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle("Title")
                        .bigText("Message"))
                .setAutoCancel(true)
                .setVisibility(channel.visibility)
                .setContentIntent(pending)

        manager.notify(notifyId.incrementAndGet(), notificationBuilder.build())
    }
}
