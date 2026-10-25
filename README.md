# AM Player 🎬🎵📺

<p align="center">
  <img src="screenshots/am_player_banner.png" alt="AM Player Logo" width="380" />
</p>

<p align="center">
  <b>The ultimate high-performance Android media player engineered with Jetpack Compose & AndroidX Media3</b>
</p>

<p align="center">
  <i>Made with ❤️ by <b>Arwinder Singh Malhi</b></i>
</p>

<p align="center">
  <a href="apk/AMPlayer-v2.4.apk">
    <img src="https://img.shields.io/badge/Download-AM%20Player%20APK%20v2.4%20(30%20MB)-4D6BFE?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## 📥 Direct APK Download Link

You can download and install the latest compiled release APK directly on any Android device (Android 7.0+ / API 24 to Android 15 / API 35+):

- 🚀 **[Direct Download: AMPlayer-v2.4.apk (30 MB)](apk/AMPlayer-v2.4.apk)**
- 📦 **Alternative Path**: `.build-outputs/app-debug.apk`

---

## 📱 App Screenshots Gallery

| 📁 Home Video Library | 🎵 Advanced Music Player |
|:---:|:---:|
| <img src="screenshots/home_library.jpg" width="360" alt="Home Library Screen" /> | <img src="screenshots/music_player.jpg" width="360" alt="Music Player Screen" /> |
| *3-column folder grid, recent highlight & lightest pink play FAB* | *Rotating vinyl disc, real-time spectrum visualizer & controls* |

| 🔒 Secret Video Vault | 📺 IPTV Live Streamer |
|:---:|:---:|
| <img src="screenshots/secret_vault.jpg" width="360" alt="Secret Video Vault Screen" /> | <img src="screenshots/iptv_streamer.jpg" width="360" alt="IPTV Streamer Screen" /> |
| *5-tap unlock, 4-digit PIN/Biometric lock & private video manager* | *Multi-category M3U playlist streaming with live badges* |

| ⚙️ Minimal Settings Page |
|:---:|
| <img src="screenshots/settings_screen.jpg" width="360" alt="Settings Screen" /> |
| *"Made with love by Arwinder Singh Malhi" & 7 minimal categories* |

---

## 🔥 All Features Breakdown

### 1. 🔒 Secret Locked & Hidden Video Vault
Protect your personal and sensitive videos with a built-in security vault hidden right behind the app logo:
- **5-Tap Gesture Trigger**: Tapping the top-left AM Player main logo **5 times in a row** opens the secret vault.
- **Interactive Countdown**: Toasts guide the user on taps 2, 3, and 4 (*"Tap X more times to open Secret Vault"*).
- **First-Time Setup**: Prompts the user to set a 4-digit PIN with confirmation and optional system biometric authentication.
- **PIN & Biometric Unlock**: Keypad entry with dot indicators and one-tap fingerprint/face unlock.
- **Hide Any Video**: Browse and lock any video from device storage—hidden videos immediately vanish from the public library, folders, and search results.
- **Direct Playback**: Watch locked videos inside AM Player directly from the vault without unhiding them.
- **One-Tap Restore**: Restore any video back to the public library with a single tap.
- **PIN Management**: Reset or change vault PIN securely whenever needed.

---

### 2. 📺 IPTV Live Streamer & Multi-Playlist Folders
Stream live TV channels and IPTV feeds with zero buffering and organized folder management:
- **HLS & M3U8 Streaming**: Low-latency live video playback powered by AndroidX Media3 HLS engine.
- **Multi-Category Filtering**: Quick filter chips for *All*, *Sports*, *News*, *Movies*, and *Tech & 4K*.
- **Custom M3U Playlists**: Add custom M3U/M3U8 URLs with customizable folder names, icons, and accent colors.
- **Live Stream Badges**: Real-time *LIVE* indicator pills with resolution (1080p, 720p, 4K) and frame rate (60fps).
- **Player Screen Lock**: Lock controls during live streaming to prevent accidental touches.
- **Color Modes**: Switch between *Normal*, *Vivid*, *Cinema*, and *HDR Boost* for live video streams.

---

### 3. 🎵 Advanced Music Player & High-Fidelity Audio
An audio engine designed for audiophiles with instant responsiveness:
- **Instantaneous Play, Pause & Skipping**: Zero-latency optimistic UI state updates ensure 0ms delay when tapping play, pause, next, or previous.
- **Lock-Screen & Background Playback**: Native `WAKE_MODE_LOCAL` keeps the CPU active for uninterrupted music streaming even when the phone screen is locked.
- **Full-Screen Vinyl Visualizer**: Gorgeous rotating vinyl record disc with radial glow and real-time audio spectrum equalizer bars.
- **5-Band Equalizer & Bass Boost**: Hardware audio effect presets including *Balanced*, *Bass Boost*, *Rock*, *Pop*, *Jazz*, *Electronic*, *Vocal Clarity*, and *Classical*.
- **Volume Boost (+10 dB)**: Native `LoudnessEnhancer` boosts volume up to **200%** for quiet recordings.
- **Open-Source Music Streaming**: Opt-in streaming of royalty-free classical and ambient masterpieces from public archives.
- **Persistent Mini-Player**: Bottom bar with progress indicator, play/pause, and next controls that stays accessible across tabs.

---

### 4. 📁 Video Library & Folder Management
Organize, browse, and manage device videos with speed and flexibility:
- **3-Column Grid View & Vertical List View**: One-tap toggle between a spacious 3-column folder grid and a detailed vertical folder list (`SaFolderListItem`).
- **Recent Video Folder Highlight**: The folder containing your most recently played video is subtly highlighted with an electric blue border, background wash, and a **"RECENT ACTIVITY"** tag.
- **Lightest Pink Play Button**: Soft blush pink squircle floating action button (`#FFD8E4`) with electric blue play icon positioned right above Settings to instantly resume your last played video.
- **Cleaned Top Bar**: Removed redundant top tabs and duplicate icons, keeping only the primary top-left brand logo (with 5-tap vault access) and action buttons.
- **Live Search**: Instant reactive search filtering across folder names and video titles.
- **8 Bidirectional Sort Options**:
  - 🔤 **Name (A to Z)** & **Name (Z to A)**
  - ⏱️ **Date (Newest First)** & **Date (Oldest First)**
  - 📦 **File Size (Largest First)** & **File Size (Smallest First)**
  - 🎞️ **Video Count (Most Videos)** & **Video Count (Fewest Videos)**

---

### 5. ⚙️ Minimal, Categorized Settings
A clean, organized settings experience with persistent storage:
- **"Made with love by Arwinder Singh Malhi"**: Prominent banner with a red heart at the top of the Settings screen.
- **7 Minimal Categories**:
  1. 🎨 **Appearance**: OLED Pitch-Black Mode (`#000000`), AM Electric Blue accent palette (`#4D6BFE`), 50% opacity logo in dark mode.
  2. 🎬 **Media library**: Re-index media files button with feedback, cache cleaner with real-time feedback, startup auto-scan toggle.
  3. ▶️ **Player**: Double-tap seek interval selection (5s, 10s, 15s, 30s), auto-resume toggle, gesture controls overview.
  4. 💾 **Decoder**: GPU hardware acceleration (HW+) priority, software engine (SW) fallback, video color profile selector (*Normal*, *Vivid*, *Cinema*, *HDR Boost*).
  5. 🎵 **Audio**: Audio boost toggle (+10 dB / 200%), play music when phone is locked toggle, equalizer preset selector.
  6. 💬 **Subtitle**: Subtitle encoding dialog (*UTF-8*, *ISO-8859-1*, *Windows-1252*, *ASCII*), online providers toggle, subtitle text size selector.
  7. ℹ️ **About**: App version 2.4.0, technical specifications, and author credits.
- **Full SharedPreferences Persistence**: Backed by `SettingsRepository` so every toggle and preference is saved across app restarts.

---

### 6. 🎬 Video Player Engine & Touch Gestures
Built on AndroidX Media3 for fluid, low-latency playback:
- **Touch Gestures**:
  - **Left Edge Swipe**: Adjust screen brightness (0% – 100%).
  - **Right Edge Swipe**: Adjust media volume and audio boost (0% – 200%).
  - **Horizontal Swipe**: Frame-accurate timeline seeking with offset HUD.
  - **Double Tap (Left / Right)**: Instant seek backward / forward (configurable).
  - **Double Tap (Center)**: Quick play / pause.
  - **Long Press**: 2.0x Fast-forward boost.
  - **Two-Finger Tap**: Instantly cycle aspect ratios (Fit, Fill, Stretch, 16:9, 4:3, Zoom).
- **Subtitles Engine**: Automatic 64-bit hash lookup on OpenSubtitles, SubDL, and Subscene; manual subtitle file picker; and sync delay adjustment (-5.0s to +5.0s).
- **Picture-in-Picture (PiP)**: Seamless floating video window when multitasking.

---

## 🛠️ Architecture & Tech Stack

```
com.example/
├── data/
│   ├── local/              # Room Database (AppDatabase, MediaDao, SubtitleDao)
│   ├── model/              # MediaItem, AudioTrack, IptvChannel, SubtitleCandidate
│   └── repository/         # MediaRepository, MusicRepository, IptvRepository, VaultRepository, SettingsRepository
├── player/
│   ├── ExoPlayerManager.kt # AndroidX Media3 Video Engine + HW+ + Audio Boost
│   ├── MusicPlayerManager.kt # Dedicated background audio player service
│   ├── AspectRatioMode.kt  # Fit, Fill, Stretch, 16:9, 4:3, Zoom
│   └── PlayerEnums.kt      # ColorProfiles, EqualizerPresets, OrientationLock
├── ui/
│   ├── library/            # 3-Column Grid, List View, Sort Dialog, Resume FAB
│   ├── music/              # Vinyl Player, Spectrum Visualizer, Mini Player
│   ├── iptv/               # Live TV Streamer, Category Chips, M3U Dialog
│   ├── vault/              # Secret Video Vault, PIN Keypad, Biometric Unlock
│   ├── settings/           # Categorized Minimal Settings, Preferences Sheets
│   ├── player/             # Video Player Screen & Touch Gesture Overlays
│   └── theme/              # OLED Black Theme, AmAccentBlue, Typography
└── MainActivity.kt         # Edge-to-edge Jetpack Compose root
```

---

## 👨‍💻 Developer & Credits

- **Developer**: Arwinder Singh Malhi (`malhi3296@gmail.com`)
- **Version**: 2.4.0 Pro (Build 2026)
- **License**: Open Source
