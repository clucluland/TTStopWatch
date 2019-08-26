// Foreground Serviceの基本 - Qiita https://qiita.com/naoi/items/03e76d10948fe0d45597
package com.example.tetsuya.ttstopwatch

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.example.tetsuya.ttstopwatch.R

class ForegroundService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this).apply {
            mContentTitle = "通知のタイトル"
            mContentText = "通知の内容"
            setSmallIcon(R.mipmap.ic_launcher)
        }.build()

        Thread(
                Runnable {
                    (0..5).map {
                        Thread.sleep(1000)

                    }

                    stopForeground(true)
                    // もしくは
                    // stopSelf()

                }).start()

        startForeground(1, notification)

        return START_STICKY
    }
}