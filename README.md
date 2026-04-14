# Math Quiz — Gemini AI Android App

An Android app that generates random primary school math questions using the Gemini 2.5 Flash API. Questions are dynamically created by AI on each session — no local question bank required.

## Features

- AI-generated math questions via Gemini 2.5 Flash
- Multiple choice format with automatic scoring
- Restart to get a completely new set of questions
- Async API calls keep the UI responsive at all times

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Async | Kotlin Coroutines (Dispatchers.IO) |
| AI API | Google Gemini 2.5 Flash |
| Data Format | JSON (org.json) |

## Architecture

- State-driven UI using Jetpack Compose `remember` — only re-renders what changes
- Single-screen pattern simulating multi-screen navigation with conditional rendering
- `rememberCoroutineScope()` binds background tasks to the UI lifecycle, preventing crashes on exit
- API Key secured via `local.properties` and `BuildConfig` — never exposed in source code

## Getting Started

1. Clone the repo
2. Add your Gemini API key to `local.properties`:
```
   API_KEY=your_gemini_api_key_here
```
3. Open in Android Studio and run on an emulator or device

## License
MIT
