# Overview

This repository contains an Android application named **Pointage**, built with Java and designed to manage exam invigilators (surveillants), their assignments, attendance scans, and sanction tracking. The app communicates with a Supabase backend to persist data about users, surveillants, exams, pointages (attendance logs), and sanctions.

# Modules & Structure

1. **app/** – Main Android application module.
   - **src/main/java/com/example/pointage/**
     - `MainActivity.java` – Hosts the navigation drawer and controls fragment navigation.
     - `LoginActivity.java` – Handles authentication and login state using Supabase.
     - `SupabaseClient.java` – OkHttp-based client encapsulating CRUD calls to Supabase REST endpoints with DNS-over-HTTPS resiliency.
     - `TestSupabaseConnection.java` / `TestConnectionActivity.java` – Utilities and screen to validate connectivity with Supabase.
     - `CaptureAct.java` – ZXing capture activity integration for QR code scanning.
     - **ui/** – Feature-specific fragments, view models, adapters, and models.
       - **home/** – Dashboard landing fragment (`HomeFragment`, `HomeViewModel`).
       - **surveillant/** – Listing and management of surveillants (`SurveillantFragment`, adapter, model, view model).
       - **historique/** – Attendance history (`HistoriqueFragment`, view model, adapter, `Pointage` model, `DateUtils`). Handles QR scan workflow and pointage insertion.
       - **sanction/** – Sanction reporting (`SanctionFragment`, `SanctionViewModel`, adapter, `SurveillantSanction` model) with periodic refresh logic aggregating data from surveillants, exams, and pointages.
   - **src/main/res/** – Layouts, navigation graph, menu definitions, and other Android resources.
   - `AndroidManifest.xml` – Application manifest registering activities and permissions.

2. **gradle/** – Gradle wrapper configuration (`gradle-wrapper.properties`, `libs.versions.toml`).

3. **Root files** – `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md`, Supabase SQL setup scripts, troubleshooting guides.

# Build & Dependencies

- **Languages**: Java-only Android project targeting SDK 35 (min SDK 24).
- **Build system**: Gradle with Kotlin DSL scripts (`build.gradle.kts`).
- **Key dependencies**:
  - AndroidX AppCompat, Material Components, ConstraintLayout.
  - AndroidX Lifecycle (LiveData, ViewModel) and Navigation components.
  - OkHttp (with DNS-over-HTTPS extension) for Supabase requests.
  - Gson for JSON parsing.
  - ZXing Android Embedded for QR code scanning.
- **Build features**: ViewBinding enabled; ProGuard disabled for release by default.

# Data & Backend

- Uses Supabase REST API to manage tables such as `surveillant`, `examen`, `pointage`, `sanction`, and `utilisateurs`.
- SQL setup scripts (`supabase_setup.sql`, `supabase_setup_updated.sql`) define schema and stored procedures.
- `SupabaseClient` centralizes all network calls with support for select/insert/update/delete operations and shared callbacks dispatched on the main thread.

# Key Workflows

1. **Authentication** – `LoginActivity` toggles Supabase `log` flag, stores session in shared preferences, and routes to `MainActivity`.
2. **Navigation** – `MainActivity` configures a navigation drawer with entries for Home, Surveillant, Historique, and Sanction
   fragments, plus logout handling.
3. **QR Pointage** – `HistoriqueViewModel` validates surveillant-room assignments, locates relevant exams, determines lateness, and inserts pointage records into Supabase.
4. **Sanction Aggregation** – `SanctionViewModel` fetches surveillants, exams, and pointages, aggregates lateness/absence data per surveillant and auto-refreshes every 15 seconds.
5. **Surveillant Management** – `SurveillantFragment` fetches and displays surveillant assignments from Supabase.

# Testing & Utilities

- **Unit tests**: Located under `app/src/test/java` (currently minimal/placeholder).
- **Instrumentation tests**: Under `app/src/androidTest/java`.
- **Troubleshooting docs**: `TROUBLESHOOTING.md`, `GUIDE_TEST_CONNEXION.md` offer environment setup guidance.

# Build & Run Instructions

1. Open the project in Android Studio (2024.3+ recommended).
2. Sync Gradle to fetch dependencies (requires internet access to reach Supabase and Maven repositories).
3. Configure Supabase URL/API key in `SupabaseClient` if deploying to another backend instance.
4. Use "Run" to deploy on an Android device/emulator (API level ≥ 24). Scanner features require camera permission and QR-capable hardware/emulator.

# Additional Notes

- Supabase credentials are hardcoded for development; consider moving them to secure storage for production.
- Auto-refresh in `SanctionViewModel` leverages a `Handler`; ensure lifecycle awareness if fragments are recreated frequently.
- QR scanning relies on ZXing's `CaptureAct`; adjust orientation/camera settings to match target hardware.