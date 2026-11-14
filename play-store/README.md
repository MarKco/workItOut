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

Con lo script `tools/capture.py`, su emulatore con l'app debug installata:

```
python3 play-store/tools/capture.py --serial emulator-5554 --device phone
python3 play-store/tools/capture.py --serial emulator-5556 --device tablet-7
python3 play-store/tools/capture.py --serial emulator-5558 --device tablet-10   # in verticale
```

- Emulatori Pixel 9a (telefono, display forzato a 1080×2160 e 360 dpi), Tablet7_Store e Pixel_Tablet_Store
  (quest'ultimo ruotato in verticale: `settings put system user_rotation 1`)
- Orologio dell'emulatore spostato sul fuso Pacific/Kiritimati (UTC+14), così la "giornata in corso" cade
  nel pomeriggio qualunque sia l'ora reale
- Barra di stato pulita con la demo mode di Android (batteria piena, nessuna notifica)
- Dati demo generati dallo script e scritti direttamente nel DataStore dell'app: 20 giornate lavorative
  negli ultimi 27 giorni, un sabato lavorato e una giornata in corso. Nessun dato personale.
- Lingua dell'app cambiata con `adb shell cmd locale set-app-locales`; le schermate si ripetono
  identiche in ogni lingua
- Contenuto limitato a 640dp di larghezza sui tablet (vedi `WorkItOutAppEntry.kt`)

Per aggiungere una lingua basta tradurre `strings.xml`, aggiungere le etichette in `LABELS` dello script
e ripetere la cattura.
