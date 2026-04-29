# AshaSaathi — आशा साथी

**Offline-first digital assistant for ASHA workers in rural India.**

Built for the mind2i Hackathon (PS-19 · Full Stack · Hard).

> **Rewritten in native Android (Kotlin + Jetpack Compose)** for better performance, offline reliability, and on-device AI.

---

## What It Does

India's 1 million+ ASHA workers track pregnancies, vaccinations, and TB follow-ups on paper registers. AshaSaathi replaces that with:

- **Voice input** in Hindi / Kannada / English — worker speaks vitals, app fills the form (Whisper on-device STT + TinyLlama NLU)
- **Automatic risk flagging** using MoHFW clinical thresholds (BP, haemoglobin, GDM, TB) + on-device TFLite ML
- **Offline-first** — Room DB local cache, Firestore sync when connected
- **Full UIP vaccine tracker** — 31 vaccines, due-date alerts, MCP card
- **TB DOTS tracking** — adherence %, Nikshay ID, DBT calculation
- **Household management** — map all households, pregnancies, children under 5
- **Government reports** — one-tap HMIS PDF export with NHM incentive calculation
- **Daily planner** — visit schedule, priority queue by risk level
- **Diary** — voice or text notes per session

---

## Architecture

```
temp_asha/
├── app/src/main/java/com/ashasaathi/
│   ├── AshaSaathiApp.kt              Hilt + WorkManager setup
│   ├── data/
│   │   ├── model/Models.kt           All domain models
│   │   └── repository/               Firestore + Room repositories
│   ├── di/AppModule.kt               Hilt dependency graph
│   ├── service/
│   │   ├── ai/                       Whisper STT, TinyLlama NLU, TFLite risk engine
│   │   ├── notification/             FCM + WorkManager daily reminders
│   │   ├── sync/SyncWorker.kt        Background Firestore sync
│   │   └── tts/TTSService.kt         Hindi TTS
│   └── ui/
│       ├── screens/                  14 screens (home, diary, households, visit, MCP, vaccination, TB, reports, planner…)
│       ├── components/               Shared Compose components + VoiceFAB
│       ├── theme/                    Saffron + Teal palette, Material3
│       └── viewmodel/                Hilt ViewModels per screen
├── app/src/main/cpp/                 Native CMake for Whisper.cpp / llama.cpp
├── gradle/libs.versions.toml         Version catalog
└── app/build.gradle.kts
```

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material3 |
| DI | Hilt 2.52 |
| Database | Room 2.6 + Firestore offline cache |
| Auth | Firebase Auth — phone OTP |
| Background | WorkManager + Hilt Workers |
| On-device AI | Whisper.cpp (STT), TinyLlama (NLU), TFLite (risk) via JNI/CMake |
| Maps | OSMDroid (offline tiles) |
| Navigation | Navigation Compose |
| State | StateFlow + collectAsStateWithLifecycle |
| Build | AGP 8.13, Gradle 8.13, KSP |

---

## Setup

1. Clone repo
2. Add `app/google-services.json` from your Firebase project (not committed)
3. Enable Firebase Auth → Phone sign-in
4. For development: add test phone numbers in Firebase Console → Authentication → Phone → Test numbers
5. Build with Android Studio Ladybug or later

### Firebase services required
- Authentication (Phone)
- Firestore
- Cloud Messaging (FCM)
- Storage

---

## Screens

| Screen | Description |
|--------|-------------|
| Login | Phone OTP auth |
| Home | Dashboard — today's visits, risk alerts, quick stats |
| Households | List + map of all households |
| Add Household | Register new household |
| Household Detail | Members, pregnancies, children |
| Patient Detail | Full patient record |
| Visit Form | ANC/PNC/Immunisation visit with voice input |
| Vaccination | UIP schedule tracker, FIC count |
| TB DOTS | DOTS adherence, Nikshay tracking |
| MCP Card | Mother Child Protection card view |
| Diary | Voice/text notes |
| Planner | Daily visit schedule |
| Reports | HMIS export, NHM incentive summary |
| Settings | Language, profile |

---

## Previous Version

The previous React Native + Express.js version of this project (with web doctor dashboard) is in the git history prior to this commit.
