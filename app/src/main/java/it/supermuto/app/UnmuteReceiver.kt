package it.supermuto.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Riceve: fine del timer, pulsante "Riattiva suono" della notifica, riavvio del telefono. */
class UnmuteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UNMUTE -> MuteController.unmute(context)
            Intent.ACTION_BOOT_COMPLETED -> MuteController.restoreAfterBoot(context)
        }
    }

    companion object {
        const val ACTION_UNMUTE = "it.supermuto.app.UNMUTE"
    }
}
