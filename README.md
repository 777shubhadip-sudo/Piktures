# Lumina Gallery

A native Android gallery app built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM + Hilt + Room + Coroutines/Flow**, **Coil**, and **Media3 ExoPlayer**, featuring a glassmorphic design system and an encrypted **Double Hidden Folder** vault.

## Architecture Overview
- **UI & Design System (`ui/theme`, `ui/components`)**:
  - Procedural Canvas gradient mesh background with fine diagonal / dot grid overlay (`MeshGradientBackground`).
  - Floating pill-shaped glassmorphic bottom navigation with spring-animated sliding indicator blob.
  - Bundled local Google Fonts (**Sora** headings + **Plus Jakarta Sans** body) via `res/font/`.
  - Shared-element transitions (`SharedTransitionLayout`), pinch-to-zoom grid columns (`2×–5×`), fast-scroll scrubber with floating date bubble, long-press drag multi-select, Palette API dynamic accent extraction, and custom `SplashScreen`.
- **MediaStore & Editor (`data/repository/MediaStoreRepository.kt`, `ui/screens/PhotoEditorScreen.kt`)**:
  - Reactive `ContentObserver` query flow handling Android 13+ (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`) and Android 14+ partial access (`READ_MEDIA_VISUAL_USER_SELECTED`).
  - Full-screen `HorizontalPager` viewer with pinch/double-tap zoom, swipe-down dismiss, Media3 ExoPlayer video playback, EXIF details, rename, set wallpaper, 30-day MediaStore Trash/Restore, and Crop/Rotate/Filter editor.

## Security Design (Hidden & Double Hidden Folders)
1. **App-Private Storage & Obfuscation**:
   - Vault media and thumbnails are stored strictly in `context.filesDir/vault/level1` and `context.filesDir/vault/level2` using random UUID filenames (`v_<hex>.enc`, `t_<hex>.enc`) with no media extensions or external storage `.nomedia` tricks.
   - `android:allowBackup="false"` plus `backup_rules.xml` / `data_extraction_rules.xml` exclusions keep vault data, Room metadata, and secret preferences out of cloud backups and device transfers.
2. **AES-256-GCM Chunked Encryption**:
   - A 256-bit Data Encryption Key (DEK) is generated via `SecureRandom`, wrapped using an Android Keystore AES-256-GCM Master Key (`AndroidKeyStore`), and stored in `EncryptedSharedPreferences`.
   - Every media file and thumbnail is encrypted in 512 KB chunks (`AES/GCM/NoPadding`) with a unique 12-byte random IV per chunk and the 64-bit chunk index bound in GCM AAD (preventing chunk reordering or truncation).
   - Decryption occurs strictly **in memory** via a custom Coil `Fetcher` (`DiskCache.Policy.DISABLED`) and a custom Media3 `VaultMediaDataSource` supporting random-access video seeking without writing plaintext to disk.
3. **Safe Move ("Cut") State Machine**:
   - Room (`vault_media` table) tracks `COPYING -> VERIFIED -> COMMITTED`.
   - Original files are never deleted until every encrypted chunk's GCM authentication tag and the full SHA-256 checksum are verified.
   - On Android 11+, `MediaStore.createDeleteRequest` requests single-prompt batch deletion; cancelling rolls back and securely wipes the staged encrypted files. Startup recovery reconciles incomplete moves after crashes.
4. **Authentication & Level 2 Stealth Access**:
   - **Level 1 ("Secret Folder")**: Protected by `BiometricPrompt` (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`), re-locking automatically when the app enters the background (configurable timeout, default Immediate).
   - **Level 2 ("Deep Vault")**: No visible UI hints or buttons. Unlocked by tapping the `"Secret Folder"` title 5 times within 3 seconds (only active when an Extra Secret Folder Pass is set), followed by verification against a salted **PBKDF2WithHmacSHA256** (120,000 iterations) hash stored in `EncryptedSharedPreferences`. Five failed attempts trigger an increasing lockout (`30s`, `60s`, `120s`, `300s`).
   - Managing the `"Extra Secret Folder Pass"` in Settings requires a fresh biometric/screen-lock confirmation every time.
   - `WindowManager.LayoutParams.FLAG_SECURE` is enabled across all vault screens to block screenshots and recents thumbnails.

## Known Limitations
- **Device Lock Requirement**: Level 1 authentication requires a device PIN, pattern, password, or biometric credential enrolled on the device.
- **Root / Physical Forensics**: While files are encrypted with Keystore-wrapped AES-256-GCM and filenames are randomized, a rooted device with compromised TEE or active memory inspection while unlocked poses inherent OS-level risks.
- **Pre-Android 11 Trash**: System MediaStore 30-day Trash (`IS_TRASHED`) requires Android 11 (API 30+); on Android 8–10, delete actions remove the file directly.
- **Large Photo Editing**: Very large images are downsampled to max 2048 px on the longest edge during editing to avoid out-of-memory crashes on lower-RAM devices.
