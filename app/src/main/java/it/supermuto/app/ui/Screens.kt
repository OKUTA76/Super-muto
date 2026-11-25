package it.supermuto.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Sfondo = Color(0xFFFFF6E5)
val Inchiostro = Color(0xFF1C2333)
val InchiostroTenue = Color(0xFF4A5368)

@Composable
private fun Testo(
    testo: String,
    size: TextUnit,
    colore: Color = Inchiostro,
    bold: Boolean = true,
    modifier: Modifier = Modifier,
) = Text(
    text = testo,
    fontSize = size,
    lineHeight = size * 1.15f,
    fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium,
    color = colore,
    textAlign = TextAlign.Center,
    modifier = modifier,
)

@Composable
private fun Pagina(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) { content() }
}


/** Schermata iniziale: un solo, grande pulsante rosso. */
@Composable
fun HomeScreen(onSilenzia: () -> Unit, onVibrazione: () -> Unit, onIconaHome: () -> Unit) = Pagina {
    Testo("Super Muto", 40.sp)
    Testo("🔔  Ora il telefono SUONA", 26.sp, InchiostroTenue, bold = false)
    Spacer(Modifier.height(8.dp))
    Button3D(
        onClick = onSilenzia,
        colori = Rosso,
        shape = CircleShape,
        spessore = 18.dp,
        modifier = Modifier.size(240.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Testo("🔕", 80.sp, Color.White)
            Testo("SILENZIA", 38.sp, Color.White)
        }
    }
    Testo("Premi il pulsante rosso\nper mettere il telefono muto", 22.sp, InchiostroTenue, bold = false)
    Button3D(
        onClick = onVibrazione,
        colori = Turchese,
        spessore = 10.dp,
        modifier = Modifier.fillMaxWidth().height(110.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Testo("📳  VIBRAZIONE", 28.sp, Color.White)
            Testo("niente suono, ma il telefono vibra", 17.sp, Color.White.copy(alpha = 0.92f), bold = false)
        }
    }
    Spacer(Modifier.height(16.dp))
    Button3D(
        onClick = onIconaHome,
        colori = Grigio,
        spessore = 8.dp,
        modifier = Modifier.fillMaxWidth().height(84.dp),
    ) {
        Testo("📌  Metti l'icona sulla Home", 22.sp)
    }
}

/** Seconda schermata: per quanto tempo? */
@Composable
fun ChooseScreen(vibrazione: Boolean, onScelta: (Int?) -> Unit, onIndietro: () -> Unit) = Pagina {
    Testo(if (vibrazione) "📳 Vibrazione" else "🔕 Silenzio", 26.sp, InchiostroTenue)
    Testo("Per quanto tempo?", 36.sp)
    Testo("Tocca un pulsante", 22.sp, InchiostroTenue, bold = false)

    @Composable
    fun Durata(minuti: Int, sotto: String, colori: Colori3D, modifier: Modifier) = Button3D(
        onClick = { onScelta(minuti) },
        colori = colori,
        modifier = modifier.height(150.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Testo("$minuti", 54.sp, Color.White)
            Testo("minuti", 22.sp, Color.White)
            Testo(sotto, 17.sp, Color.White.copy(alpha = 0.9f), bold = false)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Durata(30, "mezz'ora", Blu, Modifier.weight(1f))
        Durata(60, "1 ora", Blu, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Durata(90, "1 ora e mezza", Blu, Modifier.weight(1f))
        Durata(120, "2 ore", Blu, Modifier.weight(1f))
    }
    Button3D(
        onClick = { onScelta(null) },
        colori = Viola,
        modifier = Modifier.fillMaxWidth().height(130.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Testo("✋  FINCHÉ LO DICO IO", 26.sp, Color.White)
            Testo("si riattiva solo col pulsante verde", 17.sp, Color.White.copy(alpha = 0.9f), bold = false)
        }
    }
    Spacer(Modifier.height(4.dp))
    Button3D(
        onClick = onIndietro,
        colori = Grigio,
        spessore = 8.dp,
        modifier = Modifier.fillMaxWidth().height(84.dp),
    ) {
        Testo("⬅  Indietro", 24.sp)
    }
}

/** Telefono muto: grande pulsante verde per riattivare. */
@Composable
fun MutedScreen(vibrazione: Boolean, orarioFine: String?, mancano: String?, onRiattiva: () -> Unit) = Pagina {
    Testo(if (vibrazione) "📳" else "🔕", 72.sp)
    Testo(if (vibrazione) "Telefono in\nVIBRAZIONE" else "Telefono in\nSILENZIO", 38.sp)
    if (orarioFine != null) {
        Testo("Il suono torna da solo\nalle $orarioFine", 26.sp, InchiostroTenue, bold = false)
        if (mancano != null) Testo("(mancano $mancano)", 22.sp, InchiostroTenue, bold = false)
    } else {
        Testo("Resta muto finché non premi\nil pulsante verde", 24.sp, InchiostroTenue, bold = false)
    }
    Spacer(Modifier.height(12.dp))
    Button3D(
        onClick = onRiattiva,
        colori = Verde,
        shape = CircleShape,
        spessore = 18.dp,
        modifier = Modifier.size(230.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Testo("🔔", 70.sp, Color.White)
            Testo("RIATTIVA\nSUONO", 32.sp, Color.White)
        }
    }
}

/** Primo avvio: spiega con parole semplici il permesso "Non disturbare". */
@Composable
fun PermissionScreen(onApriImpostazioni: () -> Unit) = Pagina {
    Testo("Benvenuto!", 38.sp)
    Testo(
        "Per mettere il telefono muto, Android chiede un permesso speciale.",
        24.sp, InchiostroTenue, bold = false,
    )
    Testo(
        "1. Premi il pulsante blu\n2. Cerca «Super Muto»\n3. Accendi l'interruttore\n4. Torna indietro",
        24.sp,
    )
    Spacer(Modifier.height(8.dp))
    Button3D(
        onClick = onApriImpostazioni,
        colori = Blu,
        modifier = Modifier.fillMaxWidth().height(130.dp),
    ) {
        Testo("⚙️  DAI IL PERMESSO", 28.sp, Color.White)
    }
}
