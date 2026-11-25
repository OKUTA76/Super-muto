package it.supermuto.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build

/** Stato attuale letto dal telefono e dalle preferenze salvate. */
data class MuteState(
    val hasPermission: Boolean = true,
    val muted: Boolean = false,
    /** Orario (millisecondi) in cui il suono torna da solo, 0 = solo manuale. */
    val untilMillis: Long = 0L,
    /** true = muto ma con vibrazione attiva. */
    val vibrate: Boolean = false,
)

/**
 * Tutta la logica per mettere e togliere il silenzio.
 * "Riattiva suono" riporta la suoneria normale, spegne "Non disturbare"
 * e alza il volume se è troppo basso.
 */
object MuteController {
    private const val PREFS = "super_muto"
    private const val KEY_MUTED = "muted"
    private const val KEY_UNTIL = "until"
    private const val KEY_VIBRATE = "vibrate"
    private const val REQ_ALARM = 10

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun audio(ctx: Context) = ctx.getSystemService(AudioManager::class.java)
    private fun notif(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun hasPermission(ctx: Context): Boolean = notif(ctx).isNotificationPolicyAccessGranted

    fun read(ctx: Context): MuteState {
        val p = prefs(ctx)
        var muted = p.getBoolean(KEY_MUTED, false)
        val until = p.getLong(KEY_UNTIL, 0L)
        if (muted) {
            val ringerBack = audio(ctx).ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                notif(ctx).currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
            val expired = until > 0 && System.currentTimeMillis() >= until
            if (expired) {
                // Il timer è scaduto ma l'allarme non è ancora arrivato: riattiviamo noi.
                unmute(ctx)
                muted = false
            } else if (ringerBack) {
                // L'utente ha riattivato il suono dai tasti del telefono: ci adeguiamo.
                clear(ctx)
                muted = false
            }
        }
        return MuteState(
            hasPermission(ctx), muted, if (muted) until else 0L,
            vibrate = muted && p.getBoolean(KEY_VIBRATE, false),
        )
    }

    /**
     * Mette il telefono in silenzio.
     * @param minutes durata in minuti, oppure null per "finché non lo riattivo io".
     * @param vibrate true = nessun suono ma vibrazione attiva.
     * @return true se è andato a buon fine.
     */
    fun mute(ctx: Context, minutes: Int?, vibrate: Boolean = false): Boolean {
        if (!hasPermission(ctx)) return false
        val p = prefs(ctx)
        val am = audio(ctx)
        val nm = notif(ctx)
        try {
            if (vibrate) {
                // Se prima era attivo "Non disturbare", lo spegniamo: altrimenti non vibrerebbe.
                if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } else {
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            // Alcuni telefoni ignorano la modalità silenziosa: usiamo "Non disturbare",
            // lasciando passare solo le sveglie (così la sveglia del mattino suona comunque).
            if (!vibrate && am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            }
        } catch (e: SecurityException) {
            return false
        }
        val until = if (minutes != null) System.currentTimeMillis() + minutes * 60_000L else 0L
        p.edit().apply {
            putBoolean(KEY_MUTED, true)
            putLong(KEY_UNTIL, until)
            putBoolean(KEY_VIBRATE, vibrate)
        }.apply()

        cancelAlarm(ctx)
        if (until > 0) scheduleAlarm(ctx, until)
        StatusNotification.show(ctx, until, vibrate)
        return true
    }

    /** Riattiva il suono (pulsante verde, notifica, fine del timer). */
    fun unmute(ctx: Context) {
        restoreNormal(ctx)
    }

    /**
     * Qualunque sia la situazione (silenzioso, vibrazione, Non disturbare, volume a zero),
     * riporta il telefono a suonare a un volume regolare.
     * Suoneria e notifiche vengono portate almeno al 70% del massimo.
     */
    fun restoreNormal(ctx: Context): Boolean {
        val am = audio(ctx)
        val nm = notif(ctx)
        var ok = true
        try {
            if (hasPermission(ctx) && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            for (stream in intArrayOf(AudioManager.STREAM_RING, AudioManager.STREAM_NOTIFICATION)) {
                val regolare = (am.getStreamMaxVolume(stream) * 0.7f).toInt().coerceAtLeast(1)
                if (am.getStreamVolume(stream) < regolare) am.setStreamVolume(stream, regolare, 0)
            }
        } catch (_: SecurityException) {
            ok = false
        }
        clear(ctx)
        return ok
    }

    private fun clear(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_MUTED, false).putLong(KEY_UNTIL, 0L).putBoolean(KEY_VIBRATE, false).apply()
        cancelAlarm(ctx)
        StatusNotification.hide(ctx)
    }

    /** Dopo un riavvio: rimette l'allarme o riattiva se il tempo è già passato. */
    fun restoreAfterBoot(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_MUTED, false)) return
        val until = p.getLong(KEY_UNTIL, 0L)
        if (until > 0 && System.currentTimeMillis() >= until) {
            unmute(ctx)
        } else {
            if (until > 0) scheduleAlarm(ctx, until)
            StatusNotification.show(ctx, until, p.getBoolean(KEY_VIBRATE, false))
        }
    }

    private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQ_ALARM,
        Intent(ctx, UnmuteReceiver::class.java).setAction(UnmuteReceiver.ACTION_UNMUTE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun scheduleAlarm(ctx: Context, at: Long) {
        val alarm = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        if (canExact) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // Senza permesso "sveglie esatte" il suono torna con qualche minuto di tolleranza.
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx))
    }
}
