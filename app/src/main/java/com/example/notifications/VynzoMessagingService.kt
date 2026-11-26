package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class VynzoMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val uid = Firebase.auth.currentUser?.uid ?: return
    runCatching {
      val dbId = applicationContext.getString(R.string.firestore_database_id)
      val db = FirebaseFirestore.getInstance(dbId)
      db.collection("users").document(uid)
        .collection("private").document("info")
        .set(
          mapOf(
            "userId" to uid,
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          SetOptions.merge()
        )
    }.onFailure {
      Log.w("VynzoFCM", "Unable to sync FCM token: ${it.message}")
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
    val body = message.notification?.body ?: message.data["body"] ?: "New activity on Vynzo"
    showLocalNotification(this, title, body)
  }

  companion object {
    const val CHANNEL_ID = "vynzo_social_alerts"

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Vynzo Activity & Messages",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Likes, comments, new followers, and direct messages on Vynzo"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
      }
    }

    fun showLocalNotification(context: Context, title: String, body: String) {
      ensureChannel(context)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
      }
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      NotificationManagerCompat.from(context)
        .notify(System.currentTimeMillis().toInt(), notification)
    }
  }
}
