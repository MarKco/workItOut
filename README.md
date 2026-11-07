# WorkItOut

App Android per tenere traccia delle ore di lavoro: timbri ingresso, pause e uscita, e ti dice a che ora puoi andare via. Ti avvisa con una notifica all'orario di uscita stimato e conserva lo storico delle giornate.

## Funzioni

- Ingresso, più pause (inizio/fine), uscita; ogni orario è modificabile a mano
- Uscita stimata = ingresso + ore giornaliere + durata delle pause
- Notifica all'orario di uscita (allarme esatto, serve il permesso "Sveglie e promemoria")
- Storico delle giornate e condivisione come testo
- Italiano e inglese

## Struttura

```
app/src/main/java/com/ilsecondodasinistra/workitout/
├── AppContainer.kt      DI manuale: repository e scheduler
├── alarm/               notifica di uscita (AlarmManager)
├── data/                modelli e HistoryRepository (Preferences DataStore)
├── domain/              calcoli e formattazione, senza dipendenze Android
└── ui/                  Compose: ViewModel + schermate (Home, Impostazioni)
```

Scelte principali: Jetpack Compose + Material 3, Navigation 3, un solo modulo, nessun framework di DI. Gli orari sono sempre epoch millisecondi; la conversione in testo avviene solo in `domain/TimeFormats`.

## Build

Servono JDK 17+ e l'Android SDK (compileSdk 37).

```
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # test unitari
./gradlew lintDebug            # lint
./gradlew assembleRelease      # release con R8 (da firmare)
```

Le versioni di plugin e librerie sono tutte in `gradle/libs.versions.toml`. La CI (`.github/workflows/ci.yml`) esegue test, lint e build a ogni push e pull request.

## Dati

I dati stanno in un Preferences DataStore (`work_history_datastore`). Il formato è retrocompatibile con le versioni precedenti: le pause della sessione in corso salvate come insieme non ordinato vengono lette e riordinate cronologicamente.
