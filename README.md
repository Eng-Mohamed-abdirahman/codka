# 🔊 Codka

**Codka** ("the voice" in Somali) is an Android app that reads mobile-money payment SMS out loud in Somali.

When a Zaad or eDahab payment arrives, the phone says the amount out loud, twice, so a seller knows right away if they were paid in full.

> **Status:** In development. Payment SMS are read, saved and spoken, and the home and history screens work. Still to come: the fake-SMS check, the setup flow for helpers, and a pilot with sellers.

---

## The problem

Many mothers who sell in Borama market can't read. When a customer pays with Zaad or eDahab, the confirmation only comes as a text message, so they can't check it themselves.

As a result:

- Some customers send **less than the agreed amount** and leave before anyone notices.
- Some customers show a **fake "payment received" SMS** sent from an ordinary phone number.
- The seller has to ask a stranger or a relative to read her messages for her.

## The solution

Codka listens for payment SMS in the background, even when the app is closed and the screen is locked. For each payment it:

1. **Reads it aloud in Somali**: the amount and the currency, spoken twice.
2. **Saves it in a history list**, so any payment can be replayed with one tap.
3. **Ignores the same SMS twice**, so a payment is never counted or spoken again.
4. **Says *"Fariin aan la garanayn"*** ("unknown message") when Zaad or eDahab send a payment message it can't read, instead of guessing.
5. *(Planned, task 13)* **Checks that it's real**: that the new balance matches the last one plus the amount. A payment that fails is saved in red, and the warning *"Digniin: been abuur"* is spoken instead of the amount.

Example of what the phone says:

> *"Lacag ayaa kuu timid: shan iyo labaatan doolar. Mar kale: Lacag ayaa kuu timid: shan iyo labaatan doolar."*
> ("Money has arrived: twenty-five dollars. Once more: money has arrived: twenty-five dollars.")

That takes about 20 seconds, or about 33 when there are cents. Amounts are said exactly as the SMS writes them and are never rounded: $7.50 is *"toddoba doolar iyo konton senti"*.

### Why only the amount?

Earlier versions also spoke the sender's name or phone number. Phones have no Somali text-to-speech voice, so names came out wrong. Reading the full 9-digit number made each payment last almost a minute, which is too long at a busy stall. The amount is what the seller needs to check. The sender's name and number are still saved, so a helper can look them up later.

Everything runs **on the phone**. There's no internet, no account and no server, and the SMS never leaves the device.

## Features

| Feature | Description | Status |
|---|---|---|
| 📩 SMS listener | Picks up Zaad and eDahab payment SMS in the background, even with the app closed and the screen locked | ✅ |
| 🧮 Parser | Reads the amount, currency (USD / SLSH), sender name and number, and new balance | ✅ |
| 🗣️ Somali voice | Offline voice built from 36 recorded Somali clips, not robotic TTS | ✅ |
| 💾 Payment storage | Every payment is saved on the phone; the same SMS is never saved twice | ✅ |
| 🔁 Home screen | The last payment in big digits on its provider's colour, and one huge button to hear it again | ✅ |
| 📜 History screen | Today's payments, newest first, each with a play button; suspicious ones in red | ✅ |
| 🚨 Fake-SMS warning | Checks the balance math; warns out loud and marks the payment red | Planned |
| 🛠️ Setup flow | Step-by-step setup for the helper: SMS permission, battery, volume test | Planned |

The screens are made for people who may not read. **Green** is Zaad, **gold** is eDahab, **red** means "careful, this may be fake", and the big speaker and play buttons do the talking.

**Planned for later:** a spoken end-of-day total, an "expected amount" check before the customer pays, and recorded names for regular customers.

**Out of scope:** iPhone (iOS blocks reading SMS), feature phones, sending money, accounts and logins, cloud sync, and languages other than Somali.

## How it works

```
Payment SMS arrives
      │
      ▼
SMS listener (BroadcastReceiver)  ──►  not Zaad / eDahab?  dropped, nothing kept
      │
      ▼
Parser (assets/parsers.json)  ──►  can't read it?  "Fariin aan la garanayn"
      │
      ▼
Fake check (planned)
      │
      ▼
Save (Room database)  ──►  same SMS again?  silent
      │
      ▼
Somali voice (stitched clips, ExoPlayer, short foreground service)
      │
      ▼
Speaker 🔊   ·   Home and History screens update by themselves
```

## Tech stack

- **Kotlin** + **Jetpack Compose**: native Android
- **BroadcastReceiver** + a short foreground service: background SMS handling
- **Media3 ExoPlayer**: plays the stitched Somali voice clips with no gaps
- **Room (SQLite)**: payment history and last known balance
- **`assets/parsers.json`**: the SMS formats, so new wordings can be added without code changes
- **JUnit 5** + **Robolectric**: tests for the parsers, Somali numbers, database and the whole SMS pipeline
- **GitHub Actions**: runs ktlint and the tests, and builds a debug APK on every push

### Why recorded voice instead of text-to-speech?

Android has no built-in Somali voice, and cloud voices need internet and money. Codka uses 36 short recorded clips (numbers, tens, *boqol*, *kun*, *malyuun*, *doolar*, *shilin*, *senti* and fixed phrases) and joins them without gaps, so it sounds natural and works fully offline.

## Project structure

```
app/src/main/java/com/codka/app/
├── sms/      # SMS listener, the pipeline (parse → check → save → speak), fake-check hook
├── parser/   # Zaad + eDahab parsers, read from assets/parsers.json
├── voice/    # Somali number words, clip list, announcer and its foreground service
├── data/     # Room database: payments and settings
└── ui/       # Compose screens: home, history, helper screen
```

## Building and testing

You need Android Studio. Its bundled JDK 17 is enough.

```bash
./gradlew ktlintCheck testDebugUnitTest assembleDebug
```

- The unit tests run every anonymised SMS sample in `app/src/test/resources/sms-samples` through the real parser, database and announcer.
- The database tests run on the computer with Robolectric.
- The debug APK ends up in `app/build/outputs/apk/debug/`.

**To add a new SMS wording:** add an anonymised sample and its expected result to `sms-samples`, then update the pattern in `app/src/main/assets/parsers.json`.

## Roadmap

- [x] Discovery: visit the market, talk to the sellers, collect real SMS samples
- [x] Design approved and prototype tested by the sellers
- [x] Sprint 0: Android project, ktlint, CI
- [x] SMS listener, parsers, Somali voice, payment storage
- [x] Home screen and history screen
- [ ] Fake-SMS check and warning screen
- [ ] Setup flow for helpers
- [ ] Tests on 3 phone brands
- [ ] Pilot: 3 sellers, 2 weeks, real payments
- [ ] Release APK + setup guide in Somali

## Privacy

Codka reads only payment SMS from Zaad and eDahab. It ignores every other message, stores only a fingerprint of each payment SMS (never its text), and nothing is uploaded anywhere.
