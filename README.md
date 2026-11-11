# 🔊 Codka

**Codka** ("the voice" in Somali) is an Android app that reads mobile-money payment SMS out loud in Somali.

When a Zaad or eDahab payment arrives, the phone says the amount and who sent it, twice, so a seller knows right away if they were paid in full. It also warns when an SMS looks like a fake payment message.

> **Status:** Planning. The scope is being confirmed with the market sellers. No app code yet.

---

## The problem

Many mothers who sell in Borama market can't read. When a customer pays with Zaad or eDahab, the confirmation only comes as a text message, so they can't check it themselves.

As a result:

- Some customers send **less than the agreed amount** and leave before anyone notices.
- Some customers show a **fake "payment received" SMS** sent from an ordinary phone number.
- The seller has to ask a stranger or a relative to read her messages for her.

## The solution

Codka listens for payment SMS in the background, even when the app is closed. For each payment it:

1. **Reads it aloud in Somali**: the amount, the currency and the **sender's name**, spoken twice.
2. **Checks that it's real**: it confirms the SMS came from the official Zaad / eDahab sender, and that the new balance matches the last one plus the amount.
3. **Warns out loud** if the message looks fake or suspicious.
4. **Saves it in a history list**, so any payment can be replayed with one tap.

Example of what the phone says:

> *"Lacag ayaa kuu timid: shan iyo labaatan doolar, waxaa soo diray Axmed Cali."*
> ("Money has arrived: twenty-five dollars, sent by Axmed Cali.")

The sender's name is taken from the payment SMS itself. If the SMS has no name, Codka reads the last 4 digits of the sender's number instead:

> *"…ka timid lambarka 4 5 6 7."* ("…from the number ending 4567.")

### How the name is spoken

Numbers and fixed phrases are pre-recorded clips, but names can't all be recorded in advance. The plan is:

1. **Saved customers** (after the MVP): the seller (or a helper) records a regular customer's name once, and Codka plays that recording every time that number pays.
2. **New senders**: the name from the SMS is spoken with the phone's text-to-speech voice. Somali spelling is very regular, so it can be read letter-by-sound.
3. **No name, or no usable voice on the phone**: Codka falls back to the last 4 digits of the number.

Everything runs **on the phone**. There's no internet, no account and no server, and the SMS never leaves the device.

## Features (MVP)

| Feature | Description |
|---|---|
| 📩 SMS listener | Picks up Zaad and eDahab payment SMS in the background |
| 🧮 Parser | Reads the amount, currency (USD / SLSH), sender name and number, and balance |
| 🗣️ Somali voice | Offline voice built from recorded Somali clips, not robotic TTS |
| 🚨 Fake-SMS warning | Checks the sender ID and the balance math |
| 🔁 Repeat button | One big button that replays the last payment |
| 📜 History | A list of past payments, each one replayable |

**Planned for later:** a spoken end-of-day total, an "expected amount" check before the customer pays, and recorded names for regular customers.

**Out of scope:** iPhone (iOS blocks reading SMS), feature phones, sending money, accounts and logins, cloud sync, and languages other than Somali.

## How it works

```
Payment SMS arrives
      │
      ▼
SMS listener (BroadcastReceiver)
      │
      ▼
Parser  ──►  Fake check (sender ID + balance)
      │
      ▼
Somali voice engine (stitched audio clips, ExoPlayer)
      │
      ▼
Speaker 🔊  +  History (Room database)
```

## Tech stack

- **Kotlin** + **Jetpack Compose**: native Android
- **BroadcastReceiver** + a short foreground service: background SMS handling
- **Media3 ExoPlayer**: plays the stitched Somali voice clips
- **Room (SQLite)**: payment history and last known balance
- **`assets/parsers.json`**: the SMS formats, so new wordings can be added without code changes
- **JUnit**: tests for the parsers and number-to-speech logic
- **GitHub Actions**: runs the tests and builds a debug APK on every push

### Why recorded voice instead of text-to-speech?

Android has no built-in Somali voice, and cloud voices need internet and money. Codka uses about 40 short recorded clips (numbers 1–9, tens, *boqol*, *kun*, *doolar*, *shilin*, fixed phrases) and joins them together, so it sounds natural and works fully offline.

## Project structure (planned)

```
app/src/main/java/.../codka/
├── sms/      # SMS listener
├── parser/   # Zaad + eDahab parsers, fake check
├── voice/    # Somali number-to-clips + audio playback
├── data/     # Room database (payments, settings)
└── ui/       # Compose screens: repeat button, history, setup
```

## Roadmap

- [ ] Discovery: visit the market, talk to the sellers, collect real SMS samples
- [ ] Design approved and prototype tested by the sellers
- [ ] Sprint 0: Android project, ktlint, CI
- [ ] SMS listener, parsers, voice engine, fake check
- [ ] Pilot: 3 sellers, 2 weeks, real payments
- [ ] Release APK + setup guide in Somali

## Privacy

Codka reads only payment SMS from Zaad and eDahab. It ignores every other message, and nothing is uploaded anywhere.
