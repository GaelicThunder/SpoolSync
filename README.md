# SpoolSync

**SpoolSync** is an Android app for managing 3D printer filament profiles and syncing them directly to Bambu Lab printers via MQTT.

> This project is **not** a replacement for [OpenSpool](https://github.com/spuder/OpenSpool). It is a **software-only companion** for users who prefer a mobile workflow without building dedicated hardware readers.

## Features

### Core Features
- 📦 **Browse Filaments**: Search thousands of filament profiles from [SpoolmanDB](https://github.com/Donkie/SpoolmanDB)
- 🎨 **Color Browser**: Visual gallery of filament colors with high-quality images from Filament Colors API
- ⭐ **Favorites**: Quick access to your most-used materials
- 🔍 **Advanced Filtering**: Filter by brand, material type, and custom search queries
- 🎯 **Custom Profiles**: Create and save profiles for generic/unlisted filaments
- 📤 **Share Profiles**: Export as JSON to share with friends or backup
- 🔌 **Direct MQTT Sync**: Push filament settings (type, color, temps) to specific AMS slots

### Data & Storage
- 💾 **Local Database**: All favorites and custom profiles stored locally with Room
- ☁️ **Google Drive Backup**: Sign in with Google to backup/restore profiles to Drive
- 📊 **Profile Management**: Full CRUD operations on custom filament profiles

### Scanning & Import
- 📷 **QR Code Scanner**: Generate and scan OpenPrintTag-compatible QR codes
- 🏷️ **NFC Support**: Read/write OpenSpool-compatible NFC tags (implementation in progress)
- 📥 **Profile Import**: Import shared JSON profiles from other users

### Printer Integration
- 🖨️ **Bambu Lab MQTT**: Direct connection to Bambu Lab printers via MQTT over TLS
- 🎛️ **AMS Slot Selection**: Choose specific AMS unit (0-3) and tray (0-3) for sync
- 🔄 **Real-time Sync**: Instant filament configuration updates to printer
- 📡 **Connection Status**: Visual indicator of printer connection state

### User Experience
- 🌙 **Material 3 Design**: Modern UI with dark/light theme support
- 📱 **Responsive Layout**: Optimized for phones and tablets
- 🔐 **Google Sign-In**: Optional authentication for cloud features
- 🎨 **Visual Filament Cards**: Color swatches and detailed temperature info

## Why SpoolSync?

**OpenSpool** is excellent if you want tap-and-go hardware automation with NFC readers. **SpoolSync** targets a different workflow:

- You buy generic/custom filaments not in Bambu's database
- You don't want to build/buy additional hardware
- You already have your phone in hand when loading filament
- You want a searchable database + favorites list for quick AMS configuration
- You need cloud backup for your custom profiles

Many users will run **both**: OpenSpool for tagged spools, SpoolSync for everything else.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Networking**: Retrofit + Moshi
- **MQTT**: Eclipse Paho Android
- **Image Loading**: Coil
- **Camera**: CameraX for QR scanning
- **QR Generation**: ZXing
- **Cloud**: Google Drive API, Google Sign-In
- **Target**: Android API 24+ (Android 7.0+)

## Project Structure

```
app/src/main/java/dev/gaelicthunder/spoolsync/
├── MainActivity.kt
├── data/
│   ├── FilamentProfile.kt          # Room entity
│   ├── AppDatabase.kt              # Room database
│   ├── FilamentRepository.kt       # Business logic layer
│   ├── local/
│   │   └── FilamentProfileDao.kt   # Database access
│   └── remote/
│       ├── SpoolmanDbApi.kt        # Retrofit interface for SpoolmanDB
│       ├── SpoolmanDbModels.kt     # Data models with converters
│       ├── FilamentColorsApi.kt    # Filament Colors API
│       └── ApiClient.kt            # Retrofit singleton
├── ui/
│   ├── SpoolSyncApp.kt             # Main Compose UI
│   ├── FilamentDetailScreen.kt     # Detail view with actions
│   ├── ColorBrowserScreen.kt       # Visual color gallery
│   ├── ScannerScreen.kt            # QR/NFC scanner
│   ├── SettingsScreen.kt           # Printer configuration
│   ├── Dialogs.kt                  # Reusable dialog components
│   ├── SpoolSyncViewModel.kt       # UI state management
│   └── theme/
│       └── Theme.kt                # Material 3 theming
├── mqtt/
│   └── BambuMqttClient.kt          # MQTT client for printer communication
├── drive/
│   └── DriveManager.kt             # Google Drive backup/restore
└── models/
    └── UserProfile.kt              # Google user data
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Bambu Lab printer with LAN Mode enabled
- (Optional) Google Cloud project for Drive backup features

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/GaelicThunder/SpoolSync.git
cd SpoolSync
```

2. Open in Android Studio

3. Sync Gradle dependencies

4. (Optional) Configure Google Sign-In:
   - Create a project in [Google Cloud Console](https://console.cloud.google.com)
   - Enable Google Drive API
   - Create OAuth 2.0 credentials for Android
   - Add SHA-1 fingerprint of your signing key
   - Update `google-services.json` (if using)

5. Run on device/emulator (API 24+)

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
1. Search for your filament brand/material in the search bar
2. Tap star icon to favorite
3. Favorites appear at top for quick access
4. Apply brand/material filters for refined searches

### Browse Colors
1. Navigate to Color Browser from drawer menu
2. Browse visual gallery of filament colors
3. Tap any color to see details and add to favorites
4. Use filters to narrow by brand or material

### Custom Filament
1. Tap **+** button on main screen
2. Enter brand, material, color hex, temperatures, density, diameter
3. Save → auto-added to favorites
4. Custom profiles can be edited or deleted

### Sync to AMS
1. Select filament from list or detail screen
2. Tap **Sync to AMS** button
3. Choose target AMS unit (0-3) and tray (0-3)
4. Confirm → Printer updates immediately via MQTT

### Share Profile
1. Tap share icon on any filament card or detail screen
2. Profile exports as JSON
3. Send via Telegram/Email/Messaging app
4. Recipient can import via QR code or file

### QR Code Generation
1. Open filament detail screen
2. Tap **Generate QR Code**
3. QR code appears with OpenPrintTag format
4. Scan with another device to import
5. Print and attach to spool for quick access

### Scan QR/NFC Tags
1. Navigate to Scanner from drawer menu
2. Point camera at QR code or tap NFC tag
3. Profile automatically imports
4. Add to favorites or sync directly to AMS

### Cloud Backup
1. Sign in with Google from drawer menu
2. Tap **Backup to Drive** to save all profiles
3. Profiles saved to app-specific Drive folder
4. Restore on new device by signing in

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
- **Filament Colors API**: High-quality color images and detailed specifications
- **OpenPrintTag**: QR code format for standardized profile sharing
- **Local Room DB**: All favorites and custom profiles stored locally
- **Google Drive**: Optional cloud backup for profile data

## Roadmap

### Completed ✅
- [x] Basic UI with favorites
- [x] SpoolmanDB integration
- [x] Profile sharing (JSON export)
- [x] MQTT client implementation
- [x] AMS slot selection UI
- [x] Custom profile creation
- [x] Color browser with visual gallery
- [x] Advanced filtering (brand/material)
- [x] QR code generation and scanning
- [x] Google Sign-In integration
- [x] Google Drive backup/restore
- [x] Detail screen with full profile info
- [x] Profile deletion (custom only)

### In Progress 🚧
- [ ] NFC read/write support
- [ ] Profile import from JSON/QR
- [ ] MQTT connection stability improvements

### Planned 📋
- [ ] Batch sync multiple slots
- [ ] Filament usage tracking
- [ ] Spool weight/remaining tracking
- [ ] Print history integration
- [ ] Multiple printer support
- [ ] Prusa/Klipper support
- [ ] Community profile repository
- [ ] Translation support (i18n)
- [ ] Tablet-optimized layouts
- [ ] Wear OS companion app

## Contributing

Pull requests welcome! Priority areas:
- MQTT testing on different Bambu models (P1P, P1S, X1C, X1E)
- NFC tag reading/writing implementation
- UI/UX improvements and animations
- Translation support for multiple languages
- Documentation improvements

### Development Setup

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details

## Acknowledgments

- [OpenSpool](https://github.com/spuder/OpenSpool) - Inspiration for hardware integration
- [SpoolmanDB](https://github.com/Donkie/SpoolmanDB) - Community filament database
- [Filament Colors](https://www.filamentcolors.xyz) - High-quality filament color images
- [OpenPrintTag](https://openprinttag.org) - Open NFC standard initiative
- Bambu Lab community for MQTT protocol documentation
- Google Material Design team for Material 3 components

## Support

- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/GaelicThunder/SpoolSync/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/GaelicThunder/SpoolSync/discussions)
- 📧 **Contact**: [gaelicthunder@proton.me](mailto:gaelicthunder@proton.me)

---

**Version**: 0.3.0-alpha  
**Status**: Active Development  
**Maintained by**: Gaël (GaelicThunder)
