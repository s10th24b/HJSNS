package kr.s10th24b.app.hjsns.cloudmessaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kr.s10th24b.app.hjsns.MainActivity
import kr.s10th24b.app.hjsns.R

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("KHJ", "Message data payload : ${remoteMessage.data}")
            if (false) {
                scheduleJob()
            } else {
                handleNow()
            }
            // Check if message contains a notification payLoad
            if (remoteMessage.notification != null) {
                Log.d("KHJ", "Message Notification Body: ${remoteMessage.notification.toString()}")
            }
        }
        super.onMessageReceived(remoteMessage)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("KHJ", "onNewToken token: $token")
        sendRegistrationToServer(token)
    }

    fun sendRegistrationToServer(token: String) {
        Log.d("KHJ", "in sendRegistrationToServer")
    }

    fun scheduleJob() {
        Log.d("KHJ", "scheduleJob in")

    }

    fun handleNow() {
        Log.d("KHJ", "handleNow in")

    }

    fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val channelId = "1000"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBUilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        }
        notificationManager.notify(0 /* ID of Notification */, notificationBUilder.build())
    }
}