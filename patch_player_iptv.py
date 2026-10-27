#!/usr/bin/env python3
import sys

# Let's inspect ExoPlayerManager, PlayerViewModel, PlayerControlsOverlay, PlayerDialogs, IptvRepository
print("Inspecting files...")
for p in [
    'app/src/main/java/com/example/player/ExoPlayerManager.kt',
    'app/src/main/java/com/example/ui/player/PlayerViewModel.kt',
    'app/src/main/java/com/example/ui/player/components/PlayerControlsOverlay.kt',
    'app/src/main/java/com/example/ui/player/components/PlayerDialogs.kt',
    'app/src/main/java/com/example/data/repository/IptvRepository.kt',
    'app/src/main/java/com/example/ui/iptv/IptvScreen.kt'
]:
    try:
        with open(p, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            print(f"{p}: {len(lines)} lines")
    except Exception as e:
        print(f"Error {p}: {e}")
