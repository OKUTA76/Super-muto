package it.supermuto.app

import android.Manifest
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.supermuto.app.ui.ChooseScreen
import it.supermuto.app.ui.HomeScreen
import it.supermuto.app.ui.MutedScreen
import it.supermuto.app.ui.PermissionScreen
import it.supermuto.app.ui.Sfondo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var stato by mutableStateOf(MuteState())
    private var adesso by mutableStateOf(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stato = MuteController.read(this)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Aggiorna lo stato ogni secondo mentre l'app è aperta
        // (conto alla rovescia, permesso appena concesso, timer scaduto...).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    stato = MuteController.read(this@MainActivity)
                    adesso = System.currentTimeMillis()
                    delay(1000)
                }
            }
        }

        setContent {
            MaterialTheme {
                var scegli by rememberSaveable { mutableStateOf(false) }
                var vibrazione by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = scegli) { scegli = false }

                Box(Modifier.fillMaxSize().background(Sfondo)) {
                    when {
                        !stato.hasPermission -> PermissionScreen(onApriImpostazioni = ::apriPermesso)

                        stato.muted -> MutedScreen(
                            vibrazione = stato.vibrate,
                            orarioFine = stato.untilMillis.takeIf { it > 0 }
                                ?.let { StatusNotification.formatTime(this@MainActivity, it) },
                            mancano = stato.untilMillis.takeIf { it > 0 }
                                ?.let { tempoMancante(it - adesso) },
                            onRiattiva = {
                                MuteController.unmute(this@MainActivity)
                                stato = MuteController.read(this@MainActivity)
                                Toast.makeText(this@MainActivity, "🔔 Suono riattivato", Toast.LENGTH_SHORT).show()
                            },
                            onRipristina = ::ripristina,
                            onIconaHome = ::iconaSullaHome,
                        )

                        scegli -> ChooseScreen(
                            vibrazione = vibrazione,
                            onScelta = { minuti ->
                                val ok = MuteController.mute(this@MainActivity, minuti, vibrazione)
                                scegli = false
                                stato = MuteController.read(this@MainActivity)
                                if (!ok) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Non riesco a silenziare: controlla il permesso",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            onIndietro = { scegli = false },
                        )

                        else -> HomeScreen(
                            onSilenzia = { vibrazione = false; scegli = true },
                            onVibrazione = { vibrazione = true; scegli = true },
                            onRipristina = ::ripristina,
                            onIconaHome = ::iconaSullaHome,
                        )
                    }
                }
            }
        }
    }

    private fun ripristina() {
        val ok = MuteController.restoreNormal(this)
        stato = MuteController.read(this)
        val msg = if (ok) "🔊 Volume ripristinato" else "Non riesco a cambiare il volume: controlla il permesso"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /** Chiede al telefono di aggiungere l'icona di Super Muto nella schermata Home. */
    private fun iconaSullaHome() {
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            Toast.makeText(
                this,
                "Il telefono non lo permette da qui: apri l'elenco delle app, tieni premuto Super Muto e trascinalo sulla Home",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        val info = ShortcutInfo.Builder(this, "super_muto_home")
            .setShortLabel(getString(R.string.app_name))
            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(
                Intent(this, MainActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            .build()
        sm.requestPinShortcut(info, null)
        Toast.makeText(this, "Premi «Aggiungi» per mettere l'icona sulla Home", Toast.LENGTH_LONG).show()
    }

    private fun apriPermesso() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    /** "1 ora e 12 minuti", "25 minuti", "meno di un minuto". */
    private fun tempoMancante(ms: Long): String {
        val minutiTot = ((ms + 59_999) / 60_000).toInt()
        if (minutiTot <= 1) return "meno di un minuto"
        val ore = minutiTot / 60
        val min = minutiTot % 60
        val oreTxt = when (ore) { 0 -> ""; 1 -> "1 ora"; else -> "$ore ore" }
        val minTxt = when (min) { 0 -> ""; 1 -> "1 minuto"; else -> "$min minuti" }
        return listOf(oreTxt, minTxt).filter { it.isNotEmpty() }.joinToString(" e ")
    }
}
