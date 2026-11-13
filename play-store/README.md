# Screenshot per il Play Store

Screenshot dell'app generati su emulatore con dati di esempio (nessun dato reale), pronti da caricare
nella scheda Play Console.

```
screenshots/
├── en-US/            inglese
│   ├── phone/        1080×2160  (9:18)
│   ├── tablet-7/     1200×1920  (7")
│   └── tablet-10/    1600×2560  (10")
└── it-IT/            italiano, stessa struttura
```

Ogni cartella contiene 7 immagini, in ordine di presentazione consigliato:

| File | Schermata |
|------|-----------|
| `01-home.png` | Giornata in corso con pausa e uscita stimata |
| `02-history.png` | Storico delle giornate |
| `03-edit-day.png` | Modifica di una giornata (due pause) |
| `04-statistics.png` | Statistiche e grafico degli ultimi 14 giorni |
| `05-settings.png` | Ore giornaliere, giorni lavorativi, backup |
| `06-statistics-dark.png` | Statistiche in tema scuro |
| `07-home-dark.png` | Home in tema scuro |

Play Console accetta fino a 8 screenshot per tipo di dispositivo; per i tablet sono opzionali.

## Come sono stati generati

- Emulatori Pixel 9a (telefono, display forzato a 1080×2160 e 360 dpi), Tablet7_Store e Pixel_Tablet_Store (in verticale)
- Barra di stato pulita con la demo mode di Android (ore 10:09, batteria piena, nessuna notifica)
- Dati demo scritti direttamente nel DataStore dell'app: 21 giornate nelle ultime 4 settimane più una
  giornata in corso, tutte con orari plausibili. Nessun dato personale.
- Lingua dell'app cambiata con `adb shell cmd locale set-app-locales`; le schermate si ripetono
  identiche in ogni lingua
- Contenuto limitato a 640dp di larghezza sui tablet (vedi `WorkItOutAppEntry.kt`)

Per aggiungere una lingua basta tradurre `strings.xml` e ripetere la cattura nella nuova lingua.
