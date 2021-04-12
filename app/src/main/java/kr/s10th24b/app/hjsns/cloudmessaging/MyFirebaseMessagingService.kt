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
import splitties.toast.toast

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("KHJ","From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("KHJ", "Message data payload : ${remoteMessage.data}")
            if (false) {
                scheduleJob()
            } else {
                handleNow()
            }
            // Check if message contains a notification payLoad
            if (remoteMessage.notification != null) {
                Log.d("KHJ", "Message Notification Title: ${(remoteMessage.notification as RemoteMessage.Notification).title}")
                Log.d("KHJ", "Message Notification Body: ${(remoteMessage.notification as RemoteMessage.Notification).body}")
            }
        }
        super.onMessageReceived(remoteMessage)
    }

    override fun onDeletedMessages() {
        Log.d("KHJ","onDeletedMessages. You need to sync to App Server!")
        super.onDeletedMessages()
    }

    override fun onNewToken(token: String) {
        Log.d("KHJ", "onNewToken token: $token")
        toast("new Token: $token")
        sendRegistrationToServer(token)
        super.onNewToken(token)
    }

    private fun sendRegistrationToServer(token: String) {
        Log.d("KHJ", "in sendRegistrationToServer")
    }

    private fun scheduleJob() {
        Log.d("KHJ", "scheduleJob in")

    }

    private fun handleNow() {
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