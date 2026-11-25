# Super Muto

App Android semplicissima (pensata per persone anziane) per mettere il telefono in silenzio.

1. Grande pulsante rosso **SILENZIA**
2. Scegli: **30 / 60 / 90 / 120 minuti** oppure **Finché lo dico io**
3. Grande pulsante verde **RIATTIVA SUONO** (anche nella notifica): suoneria normale e volume regolare

Allo scadere del tempo il suono torna da solo, anche ad app chiusa o dopo un riavvio.
La sveglia continua a suonare.

## Come ottenere l'APK
- **GitHub**: carica questa cartella in un repository. Il workflow `.github/workflows/build.yml`
  compila l'app: in *Actions → Compila APK → Artifacts* trovi `SuperMuto-apk`.
- **Android Studio**: apri la cartella e premi ▶ Run.

Requisiti: Android 8.0 o superiore. Al primo avvio l'app chiede il permesso "Non disturbare".

## Struttura
- `MuteController.kt` – mette/toglie il silenzio, salva la modalità precedente, timer con AlarmManager
- `UnmuteReceiver.kt` – fine timer, tasto della notifica, riavvio del telefono
- `StatusNotification.kt` – notifica fissa con "Riattiva suono"
- `ui/Button3D.kt` – pulsante 3D che affonda alla pressione, con vibrazione
- `ui/Screens.kt` – le quattro schermate
