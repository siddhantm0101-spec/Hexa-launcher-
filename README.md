# HEXA Launcher 1.1 — Refined One UI-inspired edition

This is a native Android launcher project, not a replacement Android OS.

## Design direction
Inspired by the usability principles of Samsung One UI (large headings, comfortable spacing,
rounded cards, bottom-friendly actions) while using a distinct HEXA visual identity:
deep graphite/navy surfaces, electric blue/violet accents, restrained gradients and custom layout.

## Features
- Large, readable home-screen typography and spacious cards
- Custom HEXA hero card, clock/date and favourite dock
- Real installed app icons and searchable app drawer
- Swipe-up app drawer
- Long-press app menu: open, favourite, hide, app info
- Hidden-app management in settings
- Edge quick-access panel (swipe inward from right edge of home screen)
- Dark/light appearance
- Four accent colours
- 3/4/5-column app drawer grid
- Wallpaper picker shortcut
- Default Home app settings shortcut
- Reset and About settings
- GitHub Actions debug APK build

## Build without a PC
1. Create GitHub repository `HEXA-Launcher`.
2. Extract this ZIP and upload all files and folders to repository root.
3. Open Actions > Build HEXA Launcher APK > Run workflow.
4. Download `HEXA-Launcher-1.1-debug` from the completed run's Artifacts.
5. Extract and install `app-debug.apk`; allow install unknown apps if Android asks.
6. Press Home and select HEXA Launcher > Always.

## Build in Android Studio
Open project, sync Gradle, then Build > Build APK(s).

## Important limits
A normal launcher cannot replace Samsung's lock screen, notification shade, Quick Panel,
Always On Display, system gestures, Secure Folder, Modes and Routines, DeX, or Samsung system apps.
Those require Samsung firmware/system privileges or separate app integrations. This project
implements launcher-level equivalents only; it does not claim to clone or control those features.

Package: `com.hexa.launcher`
Version: 1.1
