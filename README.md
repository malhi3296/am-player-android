# AM Player 🎬🎵📺

**AM Player** is a modern, lightweight, lightning-fast, and feature-packed Android media player application built 100% in Kotlin and Jetpack Compose with Material Design 3. It combines the gesture-driven ergonomics of **MX Player** and the minimalist, precision controls of **mpvEx Player**, engineered from the ground up on AndroidX Media3 (ExoPlayer).

![AM Player Banner](app/src/main/res/drawable/am_player_logo.png)

---

## 🌟 Key Highlights

- **Aesthetic UI & Pitch-Black OLED Mode**: Built according to exact Material 3 specifications with `#000000` true OLED pitch black background, modern card elevations, and `#4D6BFE` signature AM accent blue.
- **Top Brand Logo**: Clean top bar with original brand logo, light/dark mode switcher, and instant sort options.
- **Floating Action Resume Button**: Squircle FAB with active folder indicator that instantly resumes your last played video from its exact millisecond location.
- **4-Section Bottom Navigation**:
  1. 📁 **Home (Videos)**: 3-column folder grid, folder badges, duration tags, sort by Date/Name/Size/Duration.
  2. 🎵 **Music Player**: Full-featured audio engine with queue management, persistent mini-player, full-screen player sheet with seekbar, shuffle, repeat, and volume boost.
  3. 📺 **IPTV Streamer**: Live TV streaming for HLS/M3U streams, category filtering (News, Sports, Entertainment, Tech, 4K), and custom M3U stream URL adder.
  4. ⚙️ **Settings**: Dark/Light mode toggle, HW+/SW decoder selector, background playback, cache cleaner, and library re-indexing.
- **Multi-Skin Customization**: Choose between 5 player skins on the fly:
  - 🔵 **AM Royal Blue** (Default Signature)
  - 💠 **OLED Neon Cyan**
  - 🟠 **MX Classic Amber**
  - 🟣 **Cyberpunk Neon**
  - 🟢 **Emerald Cinema**

---

## 🚀 Media Engine & Features

### 1. Hardware Acceleration (HW+) & Audio Boost
- Low-latency seek buffers eliminate black screen delays during fast timeline scrubbing.
- Built-in `LoudnessEnhancer` audio boost up to **200% (+10 dB)** for quiet video tracks.
- Dynamic decoder switching between Hardware (HW+) and Software (SW).

### 2. Advanced Touch Gestures
- **Left Edge Swipe**: Adjust screen brightness (0% – 100%).
- **Right Edge Swipe**: Adjust media volume and audio boost (0% – 200%).
- **Horizontal Swipe / Drag**: Exact frame-accurate timeline seeking with time offset HUD.
- **Double Tap Left / Right**: Instant -10s rewind or +10s fast forward.
- **Double Tap Center**: Toggle play / pause with smooth ripple.
- **Two-Finger Tap**: Instantly cycle aspect ratios (Fit, Fill, Stretch, 16:9, 4:3, Zoom).
- **Long Press**: 2.0x Fast-forward boost.

### 3. Global Hardware Keyboard Shortcuts
For external keyboards, Samsung DeX, ChromeOS, and Android TV:

| Key | Action |
|:---|:---|
| **Space** / **K** | Toggle Play / Pause |
| **Left Arrow** / **J** | Seek backward (-10s) |
| **Right Arrow** / **L** | Seek forward (+10s) |
| **Up Arrow** | Volume Up (+5%) |
| **Down Arrow** | Volume Down (-5%) |
| **M** | Toggle Mute / Unmute |
| **F** | Cycle Aspect Ratio mode |
| **[** | Decrease Playback Speed (-0.25x) |
| **]** | Increase Playback Speed (+0.25x) |
| **C** | Capture Screenshot / Frame |
| **Back / Esc** | Exit Player or Unlock Screen |

### 4. Extra Tools & Utilities
- **A-B Repeat Looping**: Set Point A and Point B to practice tutorials, analyze sports, or study video loops.
- **Screenshot Frame Capture**: Capture exact high-res video frame directly to storage.
- **Subtitles & Multi-Track Audio**:
  - Auto-subtitles search with 64-bit OpenSubtitles video hash.
  - Manual subtitle picker for `.srt`, `.ass`, `.vtt` files.
  - Subtitle synchronization delay adjustment (-5.0s to +5.0s).
- **Picture-in-Picture (PiP)**: Smooth transition to floating window when leaving the app.
- **System Navigation Insets**: Controls and bottom buttons respect window insets (`navigationBarsPadding()`) so no buttons hide behind system navigation buttons.

---

## 🛠️ Architecture & Tech Stack

```
com.example/
├── data/
│   ├── local/              # Room Database (AppDatabase, MediaDao, SubtitleDao)
│   ├── model/              # MediaItem, AudioTrack, IptvChannel, SubtitleCandidate
│   └── repository/         # MediaRepository, MusicRepository, IptvRepository, SubtitleRepository
├── player/
│   ├── ExoPlayerManager.kt # Media3 ExoPlayer Engine + HW+ + LoudnessEnhancer
│   ├── MusicPlayerManager.kt # Dedicated background audio player service manager
│   ├── AspectRatioMode.kt  # Fit, Fill, Stretch, 16:9, 4:3, Zoom
│   └── PlayerEnums.kt      # PlayerSkin, AudioPreset, OrientationLock
├── ui/
│   ├── library/            # 3-Column Folder Grid, Sort Dialogs, Resume FAB
│   ├── music/              # Music Library, Mini-Player, Fullscreen Audio Sheet
│   ├── iptv/               # IPTV Live Streamer, Category Filter, M3U Dialog
│   ├── settings/           # SettingsScreen, Theme Switcher, Decoder Options
│   ├── player/             # Video Player Screen & Gestures
│   │   └── components/     # ControlsOverlay, Hud, Dialogs, SkinPicker, SubtitleSheet
│   └── theme/              # OLED Black Theme, AmAccentBlue, Typography
└── MainActivity.kt         # Edge-to-edge Compose root container
```

- **Language**: Kotlin 100%
- **UI Framework**: Android Jetpack Compose + Material Design 3
- **Media Engine**: AndroidX Media3 (ExoPlayer 1.5.1) + `media3-ui`
- **Database**: AndroidX Room with Coroutines Flow support
- **Image Loading**: Coil 3 (Compose + SVG)
- **Navigation**: Type-safe Compose navigation & tab crossfades
- **Architecture**: MVVM + Clean Architecture + Unidirectional Data Flow (UDF)

---

## 📦 Building the APK

### Prerequisites
- JDK 17+
- Android SDK 36 (targetSdk 36, minSdk 24)
- Gradle 8.11+

### Build Debug APK
To assemble the standalone debug APK:
```bash
gradle assembleDebug
```
The generated APK will be available at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Application ID
```kotlin
defaultConfig {
    applicationId = "com.aistudio.amplayer.kzpvmq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
}
```

---

## 📄 License
This project is licensed under the Apache 2.0 License.
