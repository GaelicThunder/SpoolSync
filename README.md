# SpoolSync

**SpoolSync** is an Android app for managing 3D printer filament profiles and syncing them directly to Bambu Lab printers via MQTT.

> This project is **not** a replacement for [OpenSpool](https://github.com/spuder/OpenSpool). It is a **software-only companion** for users who prefer a mobile workflow without building dedicated hardware readers.

## Features

- 📦 **Browse Filaments**: Search thousands of filament profiles from [SpoolmanDB](https://github.com/Donkie/SpoolmanDB)
- ⭐ **Favorites**: Quick access to your most-used materials
- 🎨 **Custom Profiles**: Create and save profiles for generic/unlisted filaments
- 📤 **Share Profiles**: Export as JSON to share with friends or backup
- 🔌 **Direct MQTT Sync**: Push filament settings (type, color, temps) to specific AMS slots
- 🏷️ **NFC Support** (optional): Read/write OpenSpool-compatible NFC tags

## Why SpoolSync?

**OpenSpool** is excellent if you want tap-and-go hardware automation with NFC readers. **SpoolSync** targets a different workflow:

- You buy generic/custom filaments not in Bambu's database
- You don't want to build/buy additional hardware
- You already have your phone in hand when loading filament
- You want a searchable database + favorites list for quick AMS configuration

Many users will run **both**: OpenSpool for tagged spools, SpoolSync for everything else.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room (SQLite)
- **Networking**: Retrofit + Moshi
- **MQTT**: Eclipse Paho Android
- **Target**: Android API 24+ (Android 7.0+)

## Project Structure

```
app/src/main/java/dev/gaelicthunder/spoolsync/
├── MainActivity.kt
├── data/
│   ├── FilamentProfile.kt          # Room entity
│   ├── FilamentProfileDao.kt       # Database access
│   ├── AppDatabase.kt              # Room database
│   ├── FilamentRepository.kt       # Business logic layer
│   └── remote/
│       ├── SpoolmanDbApi.kt        # Retrofit interface for SpoolmanDB
│       └── ApiClient.kt            # Retrofit singleton
├── ui/
│   ├── SpoolSyncApp.kt             # Main Compose UI
│   ├── SpoolSyncViewModel.kt       # UI state management
│   └── theme/
│       └── Theme.kt                # Material 3 theming
└── service/
    └── BambuMqttClient.kt          # MQTT client for printer communication
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Bambu Lab printer with LAN Mode enabled

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/GaelicThunder/SpoolSync.git
cd SpoolSync
```

2. Open in Android Studio

3. Sync Gradle dependencies

4. Run on device/emulator (API 24+)

### Printer Configuration

1. Enable **LAN Mode** on your Bambu printer
2. Note the **Access Code** from printer settings
3. Find your printer's **IP Address** on local network
4. In SpoolSync settings, enter:
   - Printer IP
   - Serial Number
   - Access Code

## Usage

### Search & Favorite
1. Search for your filament brand/material
2. Tap star icon to favorite
3. Favorites appear at top for quick access

### Custom Filament
1. Tap **+** button
2. Enter brand, material, color, temps
3. Save → auto-added to favorites

### Sync to AMS
1. Select filament from list
2. Choose target AMS slot (A1-A4)
3. Tap **Sync**
4. Printer updates immediately via MQTT

### Share Profile
1. Long-press any filament
2. Tap share icon
3. Send JSON via Telegram/Email/etc.

## MQTT Protocol

SpoolSync communicates with Bambu printers using their internal MQTT broker:

- **Protocol**: MQTT 3.1.1 over TLS
- **Port**: 8883
- **Auth**: Username `bblp` + Access Code
- **Topic**: `device/{SERIAL}/request`

Example payload for AMS slot configuration:
```json
{
  "print": {
    "command": "ams_filament_setting",
    "ams_id": 0,
    "tray_id": 0,
    "tray_color": "FF5733FF",
    "nozzle_temp_min": 220,
    "nozzle_temp_max": 240,
    "tray_type": "PLA"
  }
}
```

## Data Sources

- **SpoolmanDB**: Community-maintained database with 1000+ filament profiles
- **OpenPrintTag**: Future integration planned for standardized NFC tags
- **Local Room DB**: All favorites and custom profiles stored locally

## Roadmap

- [x] Basic UI with favorites
- [x] SpoolmanDB integration
- [x] Profile sharing
- [ ] MQTT client implementation
- [ ] AMS slot selection UI
- [ ] NFC read/write support
- [ ] Import shared profiles
- [ ] Batch sync multiple slots
- [ ] Filament usage tracking
- [ ] Prusa/Klipper support

## Contributing

Pull requests welcome! Priority areas:
- MQTT testing on different Bambu models
- NFC tag reading/writing
- UI/UX improvements
- Translation support

## License

MIT License - see [LICENSE](LICENSE) for details

## Acknowledgments

- [OpenSpool](https://github.com/spuder/OpenSpool) - Inspiration for hardware integration
- [SpoolmanDB](https://github.com/Donkie/SpoolmanDB) - Community filament database
- [OpenPrintTag](https://openprinttag.org) - Open NFC standard initiative
- Bambu Lab community for MQTT protocol documentation
