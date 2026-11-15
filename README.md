# 🔊 Codka

**Codka** ("the voice" in Somali) is an Android app for market sellers who can't read. When a Zaad or eDahab payment SMS arrives, the phone says the amount out loud in Somali, twice, so the seller knows right away if she was paid in full.

> **Status:** In development. Setup, SMS → voice, and the home and history screens work. Still to come: the fake-SMS check and a pilot with sellers in Borama.

## Screens

| Setup (for the helper) | Home | History |
|:---:|:---:|:---:|
| <img src="screenshots/setup.png" width="240" alt="Setup step 1: allow SMS"> | <img src="screenshots/home.png" width="240" alt="Home: last payment in big digits and a replay button"> | <img src="screenshots/history.png" width="240" alt="History: today's payments with play buttons; a suspicious one in red"> |
| One step per screen: SMS, battery, App Launch (Tecno/Infinix/itel), voice test | The last payment in big digits, and one big button to hear it again | Today's payments with a play button each; red means it may be fake |

The screens work **without reading**: green is Zaad, gold is eDahab, red means "careful", and the buttons speak.

## What it does

- **Speaks every payment, twice**: *"Lacag ayaa kuu timid: shan iyo labaatan doolar. Mar kale: …"*. It takes about 20 seconds, and the amount is never rounded.
- **Works with the app closed and the screen locked.**
- **Saves every payment**, so it can be replayed. The same SMS is never saved or spoken twice.
- **Says *"Fariin aan la garanayn"*** (unknown message) for a Zaad / eDahab payment SMS it can't read, instead of guessing.
- **Ignores every other SMS**, and keeps nothing about them.
- *Planned:* a warning for fake payment SMS, checked against the real sender and the balance.

Everything runs **on the phone**: no internet, no account, no server.

## How it works

```
SMS ─► listener ─► parser (assets/parsers.json) ─► save (Room) ─► Somali voice (recorded clips, ExoPlayer)
                                                                └► home and history update by themselves
```

The voice is built from 36 short recorded Somali clips (numbers, *boqol*, *kun*, *doolar*, *shilin*, phrases) joined without gaps. Phones have no Somali text-to-speech voice, so recorded clips sound right and work offline.

## Build and test

You need Android Studio (its bundled JDK 17 is enough).

```bash
./gradlew ktlintCheck testDebugUnitTest assembleDebug
```

- The tests run every anonymised SMS sample in `app/src/test/resources/sms-samples` through the real parser, database and announcer.
- The debug APK is in `app/build/outputs/apk/debug/`.
- **New SMS wording?** Add an anonymised sample to `sms-samples`, then update the pattern in `app/src/main/assets/parsers.json`.

## Project structure

```
app/src/main/java/com/codka/app/
├── sms/      # SMS listener and the pipeline: parse → check → save → speak
├── parser/   # Zaad + eDahab parsers
├── voice/    # Somali number words and the announcer
├── data/     # Room database: payments and settings
└── ui/       # Setup, home and history screens
```

## Privacy

Codka reads only Zaad and eDahab payment SMS. It stores a fingerprint of each payment SMS (never its text), and nothing leaves the phone.
