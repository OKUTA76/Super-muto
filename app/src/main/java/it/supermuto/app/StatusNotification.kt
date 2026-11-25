package it.supermuto.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.text.format.DateFormat
import java.util.Date

/** Notifica fissa che ricorda che il telefono è muto, con il pulsante "Riattiva suono". */
object StatusNotification {
    private const val CHANNEL = "stato_silenzio"
    private const val ID = 1

    fun show(ctx: Context, until: Long, vibrate: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Telefono muto", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val unmute = PendingIntent.getBroadcast(
            ctx, 2,
            Intent(ctx, UnmuteReceiver::class.java).setAction(UnmuteReceiver.ACTION_UNMUTE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (until > 0) "Il suono torna alle ${formatTime(ctx, until)}"
        else "Premi «Riattiva suono» quando vuoi"
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (vibrate) "📳 Telefono in vibrazione" else "🔕 Telefono in silenzio")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(ctx, R.drawable.ic_notification),
                    "🔔 Riattiva suono",
                    unmute,
                ).build()
            )
            .build()
        nm.notify(ID, n)
    }

    fun hide(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID)
    }

    fun formatTime(ctx: Context, millis: Long): String =
        DateFormat.getTimeFormat(ctx).format(Date(millis))
}
