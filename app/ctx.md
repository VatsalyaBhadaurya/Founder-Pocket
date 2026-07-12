# Founder Pocket — Full Build Plan (Vibe-Coding Edition)

> Every great notebook in history — Leonardo's codices, Darwin's field books, Edison's 3,500 volumes — was easy to *write into* and nearly impossible to *search*. The friction was never capture. It was recall. Founder Pocket exists to end that 700-year-old problem: capture in a tap, retrieve by meaning and context, entirely on your own device. Build it in that order, and every feature has a home. Build it feature-first, and you'll ship a prettier Google Keep.

---

## 0. Read This Before You Prompt a Single Line

You are vibe coding an **offline-first app with on-device ML**. That combination is the exact spot where AI coding assistants are most dangerous, because they are most confident where they are most wrong.

### Where vibe coding will carry you (trust it, move fast)
Jetpack Compose UI, Room entities/DAOs, navigation, ViewModels/state, the 15 typed forms, Material 3 theming, WorkManager boilerplate, share-intent scaffolding.

### Where it *will* lie to you (verify everything, spike first)
- **ONNX Runtime Mobile / LiteRT model loading** — hallucinated method names, wrong artifact coordinates, missing native `.so` handling.
- **Vector / semantic search** — it'll invent a "similarity" API that doesn't exist; you write cosine yourself.
- **On-device Gemma via LiteRT-LM** — wrong model format (`.task`/`.litertlm`), wrong init options, device-specific failures.
- **CameraX, FusedLocationProvider, SpeechRecognizer offline** — permission and lifecycle traps; offline STT pack may be absent on your device.
- **Encryption (Jetpack Security / SQLCipher)** — the most dangerous place to trust generated code. You're storing Aadhaar/PAN. **Do not invent crypto. Use the documented path verbatim.**

### The five non-negotiable disciplines
1. **Spike every hard integration in a throwaway project first** (Phase 0). Never build UI on an unproven integration.
2. **Paste the real library docs into the AI's context** before asking it to use ONNX/LiteRT/Security. Reject any answer that imports a dependency you haven't added to Gradle yourself.
3. **One vertical slice per prompt.** Give the AI the `Capture` entity as context every time. Keep the app runnable after each slice.
4. **Commit after every green build.** Vibe coding drifts; git is your undo.
5. **Two truths time can't change:** clipboard background-capture is OS-blocked (paste-in only); never put a model where a regex works.

---

## 1. Finalized Architecture

**One entity. Fifteen types. One capture pipeline.** This is what makes the full feature set tractable instead of 15 separate mini-apps.

```
Capture
├─ id: Long, createdAt: Instant            // always
├─ type: CaptureType                        // the 15 below
├─ body: String                             // text / transcript / note
├─ payload: String (JSON)                   // type-specific fields
│
├── Context Capture (opt-in, per capture) ──
├─ location: LatLng?, placeLabel: String?
├─ sourceApp: String?                        // from share intent
├─ photoUri: Uri?, audioUri: Uri?
├─ tags: List<String>
│
├── Retrieval ──
├─ ftsText: String                           // Room FTS4 — keyword, instant
└─ embedding: FloatArray                      // MiniLM vector — semantic, the moat

enum CaptureType {
  NOTE, VOICE, LINK, MEETING, IDEA, TASK,
  CONTACT, EXPENSE, WIN, PARKING, FOLLOWUP, DOC
}
```

`payload` per type (JSON, so you never migrate the schema for a new field):
- **MEETING**: `{ with, keyPoints, actionItems[], deadline? }`
- **IDEA**: `{ problem, whoHasIt, solution }`
- **TASK**: `{ due?, done }`
- **FOLLOWUP**: `{ subject, remindAt }`
- **CONTACT**: `{ name, metAt, org, note }`
- **EXPENSE**: `{ amount, category }`
- **PARKING**: `{ lat, lng, savedAt }`
- **DOC**: `{ docType, encryptedRef }`  ← encrypted, see §6
- **LINK**: `{ url, category }`  ← category by URL regex, not a model
- **WIN / NOTE / VOICE**: body only

**Layering:** `data` (Room, DataStore, ML) → `domain` (use-cases, repository interfaces) → `ui` (Compose, ViewModels). **There is no network module.** Omitting it entirely is how you *enforce* the privacy thesis — you can't leak what you can't send.

---

## 2. Finalized Tech Stack (corrections locked in)

| Concern | Choice | Note |
|---|---|---|
| UI | Jetpack Compose + Material 3 | |
| Persistence | Room (+ FTS4) | keyword search for free |
| Prefs | DataStore | settings, onboarding flags |
| Background | WorkManager | embed-on-save, reminders |
| Embeddings | **MiniLM (`all-MiniLM-L6-v2`) via ONNX Runtime Mobile** | ~80MB, runs on mid-range. Alt: LiteRT/MediaPipe Text Embedder |
| Semantic search | Cosine similarity, hand-written | no library does this for you |
| Speech | **Android on-device `SpeechRecognizer`** (offline) | **NOT ML Kit** — it has no general STT. Fallback: Vosk / whisper.cpp |
| Link category | **URL host regex** | no model |
| Generative LLM | **LiteRT-LM (Kotlin) + Gemma 3 1B** | MediaPipe LLM Inference is deprecated. Flagship-smooth only. Optional/deferred |
| Camera | CameraX | doc scan / photo attach |
| Location | FusedLocationProvider | one-shot, opt-in |
| Encryption | Jetpack Security `EncryptedFile` / SQLCipher | PII only, standard path |
| Auth lock | BiometricPrompt | app + doc lock |

---

## 3. The Phased Plan

You have time, so this is the *complete* build, sequenced so risk dies early and the moat lands before the trimmings.

### Phase 0 — Risk Spikes (throwaway projects, prove the hard 30% first)
Do **not** skip this. Each spike is a tiny app that does one thing on your **actual test device**.
- **Spike A — Embeddings:** load MiniLM via ONNX RT Mobile, embed a string, print the vector. *(highest risk — if this fails, the whole moat is at stake)*
- **Spike B — Offline STT:** `SpeechRecognizer` offline → transcript. Confirm the offline pack exists on your device.
- **Spike C — On-device LLM:** load Gemma 3 1B via LiteRT-LM, generate, measure tokens/sec on *your* hardware. This decides go/no-go on Phase 4.
- **Spike D — Share intent:** receive text + URL from another app.
- **Spike E — Encryption:** `EncryptedFile` round-trips a file; you can read it back.
- ✅ **Gate:** all five green on the target device. A red spike now is a saved week later.

### Phase 1 — The Spine
- `Capture` entity + `CaptureType` + Room FTS4; DAO with insert/query; repository.
- Single **capture surface**: one text field, save in ≤1 tap, type picker appears *after* text (never a 15-tile launcher — that would be the "giant dashboard" your own philosophy forbids).
- **Home**: greeting, "Today's Focus" (a *query*: TASK/FOLLOWUP due today), one Capture FAB.
- Navigation + list view of all captures.
- ✅ **Gate:** capture any text → persists → appears in list. (Pure vibe-code territory — move fast.)

### Phase 2 — Context Capture + Retrieval (the moat)
- **Context envelope:** always timestamp; opt-in one-shot location, source app (from intent), photo (CameraX/picker), tags. Attach to every capture.
- **Embed-on-save:** WorkManager job runs Spike A's pipeline off the main thread; store vector. *Capture never waits on inference.*
- **Keyword search:** Room FTS, instant.
- **Semantic search:** query → embed → cosine rank → **fuse with metadata filter** (location/date/type). Ship the headline query: *"that robotics idea near Kochi airport."*
- ✅ **Gate:** recall a capture by meaning + context, not by keyword. This is the whole product.

### Phase 3 — The Full Typed Feature Set
All 15, now as thin typed screens over the proven spine:
- **Meeting Notes** (with / key points / action items → each action item can spawn a TASK).
- **Idea Vault** (problem / who has it / solution).
- **Tasks** + **Daily Dashboard** (derived: 3 priorities AM, completed/reflection PM — a query, not new storage).
- **Follow-up Reminder** (WorkManager scheduled notification: "remind me in 7 days").
- **Contact Notes** (lightweight CRM row).
- **Expenses** (amount + category, one line).
- **Daily Wins** (one sentence, nightly).
- **Parking** (save LatLng + "Navigate back" maps intent).
- **Focus Timer** (dumb countdown, no gamification, as you specified).
- **Link Vault** (view + URL-regex category).
- **Paste-in** (the honest clipboard: a button that reads the clipboard *while the app has focus* and saves it — legal, unlike background monitoring).
- ✅ **Gate:** every capture type round-trips and shows in unified search.

### Phase 4 — On-Device Generative AI (only if Spike C was green)
- **AI Assistant** ("what should I work on?"): Gemma 3 1B via LiteRT-LM reasons over TASK/FOLLOWUP/deadlines/recent captures and suggests. Runs on demand, off the capture path.
- Optional: auto-summarize long voice notes; suggest tags. Never block capture on it.
- If Spike C was slow/red: ship a **rules-based** assistant (sort by due date + urgency). A fast heuristic beats a laggy model.
- ✅ **Gate:** assistant answers from *your real captured data*, fully offline.

### Phase 5 — Security, Documents, Hardening
- **Documents** (Aadhaar/PAN/passport/resume/deck): store via Spike E's `EncryptedFile`; metadata in Room, bytes encrypted on disk.
- **App + doc lock** via BiometricPrompt.
- Flag sensitive clipboard content; handle permission denials gracefully; no crashes on missing location/mic/camera.
- ✅ **Gate:** an Aadhaar image is stored encrypted and the app locks behind biometrics.

### Phase 6 — Polish, the <1-Minute Audit, Release
- Haptics on save, empty states, dark theme, 20-second onboarding.
- **The discipline audit:** stopwatch every capture flow. Anything over 60 seconds gets cut or simplified. This is the constitution of the app — enforce it.
- Play Store prep: signing, and a **privacy policy** (yes, even a fully offline app needs one to list — and "nothing leaves your device" is your strongest marketing line).

---

## 4. The On-Device AI Layer (detail)

**Embeddings (required, the moat).** On save → WorkManager → MiniLM → `FloatArray` → Room. Query time: embed the query, cosine-compare against all stored vectors (fine up to thousands of captures; add ANN indexing only if you cross ~10k). Fuse with metadata: semantic score × (location match ? boost : 1) × recency. That fusion is what makes *"robotics idea near Kochi"* work — semantics find "robotics idea," metadata finds "Kochi."

**Categorization (no model).** `github.com → repo`, `arxiv.org|*.edu → paper`, `linkedin.com → post`, `youtube.com → video`, else `web`. Ten lines, instant, never wrong the way a model is.

**Generative (optional).** Gemma 3 1B is the *only* place a real LLM earns its RAM: open-ended reasoning over your data. Everything else is retrieval + rules.

---

## 5. Security & PII (§6 expanded — don't vibe-code this blind)

You are storing government IDs. Treat it like it matters:
- Encrypt document bytes with Jetpack Security `EncryptedFile` (or SQLCipher for the DB). **Copy the documented setup exactly** — generated crypto is a liability, not a feature.
- Biometric gate on the Documents section at minimum.
- Never log capture bodies. Never add a network dependency "just for sync" without re-reading the privacy thesis.
- Cloud sync (your "later") is where this whole model gets dangerous — when you do it, end-to-end encrypt or you've thrown away your entire differentiator.

---

## 6. Testing & the One-Minute Constitution
- Unit-test the DAO, the cosine ranker, and the URL categorizer (deterministic, easy wins the AI writes well).
- Manual: the stopwatch audit on every capture path.
- Dogfood it for a week on your own founder chaos before you show anyone.

---

## 7. Explicitly Out (don't let scope creep back in)
Background clipboard monitoring (**OS-impossible**, forever — paste-in only) · cloud sync until E2E-encrypted · gamified focus trees · third-party calendar integration in v1 · social/sharing features (they contradict the private-vault thesis).

---

## 8. Imprint (optional)
"Founder Pocket" is fine. If you want the Vatsalya signature: the recall engine is the hero — consider naming *it* (the layer, not the app) something with your mark, e.g. **VaultRecall** or **VaSense**, and let the app be the humble shell around it. The moat deserves the name; the tiles don't




# Founder Pocket — Day-by-Day Build Schedule

> Cathedrals slipped their timelines by decades because the hard stonework was left for last. Sequence the load-bearing work first and the spires take care of themselves. This schedule proves the hardest integrations on Day 1, not Day 30.

**18 focused working days.** Reorder nothing — each block stands on the one before it. Days marked ⚠️ are high-variance (native ML / crypto integration); expect them to slip and don't treat the number as a contract. Add ~3 slack days across the schedule.

Each day: **Goal · Do · Watch · Gate.**

---

## Block 1 — Prove the Hard 30% (Days 1–3)
The phase you'll want to skip. Skip it and you'll rip out a week of UI later.

### ⚠️ Day 1 — Spike A: Embeddings (highest risk)
- **Do:** Throwaway Kotlin project. Load MiniLM (`all-MiniLM-L6-v2`) via ONNX Runtime Mobile. Embed two strings, print cosine similarity.
- **Watch:** Wrong artifact coordinates, missing native `.so`, hallucinated ONNX API. Paste the real ORT-Mobile docs into your AI's context before prompting.
- **Gate:** two related sentences score higher than two unrelated ones, on your device. If this fails, your entire moat is in question — better to know today.

### ⚠️ Day 2 — Spike C: On-device LLM (go/no-go)
- **Do:** Load Gemma 3 1B via LiteRT-LM, generate a paragraph, **measure tokens/sec on your actual device.** Afternoon: Spike B — offline `SpeechRecognizer` → transcript.
- **Watch:** Wrong model format (`.task`/`.litertlm`), device without the offline STT pack. LiteRT-LM is only smooth on flagships.
- **Gate:** you have a real tokens/sec number → this decides Phase 4. STT transcribes offline.

### Day 3 — Spikes D + E, and mop-up
- **Do:** Spike D (receive shared text/URL from another app) + Spike E (`EncryptedFile` round-trips a file). Both lighter. Absorb any slip from Days 1–2.
- **Watch:** Don't invent crypto — copy the Jetpack Security setup exactly.
- **Gate:** all five spikes green on the target device. Foundations proven.

---

## Block 2 — The Spine (Days 4–5)
Pure vibe-code territory. Move fast.

### Day 4 — Data foundation
- **Do:** Scaffold (Compose, Room, DataStore, Navigation, DI). Build the `Capture` entity + `CaptureType` enum + Room **FTS4** + DAO + repository.
- **Gate:** you can insert and query a capture in a test.

### Day 5 — Capture surface + Home
- **Do:** One-field capture screen (save in ≤1 tap, type picker *after* text). Home = greeting + "Today's Focus" (query for TASK/FOLLOWUP due today) + single Capture FAB. Unified list view.
- **Watch:** No 15-tile launcher on Home — that's the "giant dashboard" your philosophy forbids.
- **Gate:** capture any text → persists → appears in list.

---

## Block 3 — The Moat: Context + Retrieval (Days 6–8)
The three days that make this Founder Pocket and not Keep.

### Day 6 — Context Capture envelope
- **Do:** Always: timestamp. Opt-in: one-shot location (FusedLocation), source app (from intent), photo attach (CameraX/picker), tags. Wire onto every capture.
- **Watch:** Permission lifecycle traps; app must not crash when location/camera is denied.
- **Gate:** a capture carries where/when/what-from metadata.

### ⚠️ Day 7 — Embed-on-save + keyword search
- **Do:** WorkManager job runs Spike A's pipeline off the main thread on every save; store the vector. Build the instant Room-FTS keyword search screen.
- **Watch:** Capture must **never** block on embedding — it's async, always.
- **Gate:** every new capture gets a vector without the UI stalling.

### Day 8 — Semantic search + metadata fusion (the payoff)
- **Do:** Query → embed → cosine rank → **fuse with metadata** (location/date/type). Ship the headline: *"that robotics idea near Kochi airport."*
- **Gate:** you retrieve a capture by *meaning + context*, not keyword. The product is now itself.

---

## Block 4 — Full Feature Set (Days 9–12)
Eleven typed screens over the proven spine. Individually fast; there are just many. These days will likely run short — bank the time.

### Day 9 — Meeting Notes + Idea Vault
- **Do:** Meeting (with / key points / action items → each item can spawn a TASK). Idea (problem / who has it / solution).
- **Gate:** both round-trip and appear in search.

### Day 10 — Tasks, Daily Dashboard, Follow-ups
- **Do:** Tasks + Daily Dashboard (derived queries: 3 AM priorities, PM completed/reflection — no new storage). Follow-up reminders via WorkManager scheduled notifications ("remind me in 7 days").
- **Gate:** a follow-up fires a real notification at the right time.

### Day 11 — Contacts, Expenses, Wins, Focus Timer
- **Do:** Contact Notes (lightweight CRM row), Expenses (amount + category, one line), Daily Wins (one sentence), Focus Timer (dumb countdown, no gamification).
- **Gate:** all four capture in under a minute each.

### Day 12 — Parking, Links, Paste-in, Voice
- **Do:** Parking (save LatLng + "Navigate back" maps intent). Link Vault view + **URL-regex** category. **Paste-in** (button reads clipboard while app has focus — the *legal* clipboard). Voice capture wired via Spike B as a VOICE capture.
- **Watch:** Paste-in only — background clipboard monitoring is OS-blocked, permanently.
- **Gate:** all 15 types round-trip through unified search.

---

## Block 5 — Generative AI (Days 13–14) — only if Spike C was green
### ⚠️ Day 13 — The Assistant
- **Do:** Wire Gemma 3 1B via LiteRT-LM. "What should I work on?" reasons over tasks/follow-ups/deadlines/recent captures. Runs on demand, **off the capture path.**
- **Watch:** If Spike C measured slow, stop — a laggy assistant is worse than none.
- **Gate:** the assistant answers from your real captured data, fully offline.

### Day 14 — Enhance or fall back
- **Do:** If green: optional auto-summarize long voice notes, suggest tags. If Spike C was red: build a **rules-based** assistant (sort by due + urgency) in half a day and reclaim the rest.
- **Gate:** a working suggestion engine, model or rules.

---

## Block 6 — Security & Hardening (Days 15–16)
### ⚠️ Day 15 — Documents, encrypted
- **Do:** DOC type via Spike E's `EncryptedFile` — metadata in Room, bytes encrypted on disk (Aadhaar/PAN/passport/resume/deck).
- **Watch:** You're storing government IDs. Use the documented crypto path verbatim; never log capture bodies.
- **Gate:** an Aadhaar image is stored encrypted and reads back correctly.

### Day 16 — Lock + resilience
- **Do:** BiometricPrompt on app + Documents section. Harden every permission-denial path. No-crash audit on missing sensors.
- **Gate:** app locks behind biometrics; nothing crashes when a permission is refused.

---

## Block 7 — Polish, Audit, Release (Days 17–18)
### Day 17 — Feel + the One-Minute Constitution
- **Do:** Haptics on save, empty states, dark theme, 20-second onboarding. Then **stopwatch every capture flow** — anything over 60 seconds gets cut or simplified.
- **Gate:** every capture path is under a minute. No exceptions.

### Day 18 — Ship
- **Do:** Signing, **privacy policy** ("nothing leaves your device" — your strongest line), store listing, final week-long dogfood pass on your own founder chaos.
- **Gate:** a shippable build you actually use.

---

## Reading the Schedule
- **High-variance days (⚠️): 1, 2, 7, 13, 15.** These slip. Your ~3 slack days live here.
- **Likely-fast days: 9–12.** Vibe coding eats typed forms; bank the surplus.
- **The one day you cannot fake:** Day 8. If meaning-plus-context recall doesn't work, you don't have Founder Pocket yet — you have a to-do list. Everything before it is setup; everything after it is trimming.