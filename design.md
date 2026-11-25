# Lumina Gallery — UI/UX & Design System Specification

> Complete blueprint of the current **Lumina** Android application UI/UX, design tokens, navigation topology, buttons, gestures, animations, and screen-by-screen layouts.

---

## 1. Visual Identity & Aesthetic Direction

- **App Name:** Lumina (`app_name = "Lumina"`)
- **Theme Style:** **Oceanic Midnight Glassmorphism** (inspired by deep-sea cyan/teal glass interfaces).
- **Default Mode:** Dark Mode (`ThemeMode.DARK`), with user-selectable Light (`ThemeMode.LIGHT`) and System (`ThemeMode.SYSTEM`) options in Settings.
- **Vault Atmosphere Switching:** The theme dynamically shifts its accent palette and background mesh orbs depending on route context:
  - `GALLERY` — Oceanic Midnight Teal + Electric Cyan (`#32F5EA`)
  - `VAULT_LEVEL_1` ("Secret Folder") — Deep Emerald / Midnight Teal + Electric Cyan (`#32F5EA`)
  - `VAULT_LEVEL_2` ("Deep Vault") — Crimson Velvet (`#12060B`) + Crimson Rose (`#F43F5E`) & Amber Gold (`#F59E0B`)
  - **Dynamic Photo Accent (MediaViewer only):** When viewing an image in `MediaViewerScreen`, `Palette` API extracts `vibrantSwatch ?: lightVibrantSwatch ?: dominantSwatch` to temporarily override `MaterialTheme.colorScheme.primary` and `glass.accentGlow`. Clears back to the default accent when exiting the viewer or viewing a video.
- **Edge-to-Edge & Window Security:**
  - `MainActivity` enables `installSplashScreen()` (`Theme.Lumina.Splash`, `#0A0918` background, `@mipmap/ic_launcher_round`) and `enableEdgeToEdge()`.
  - Screens respect `statusBarsPadding()` and `navigationBarsPadding()` across floating top/bottom glass chrome.
  - `WindowManager.LayoutParams.FLAG_SECURE` is dynamically added when entering any Vault screen or Vault viewer (`VAULT_L1`, `VAULT_L2`, `VaultLevel1Route`, `VaultPasscodeRoute`, `VaultLevel2Route`) and cleared upon exit.

---

## 2. Color System (`Color.kt` & `Theme.kt`)

### 2.1 Core Palette Tokens
| Token Name | Hex | Role |
|---|---|---|
| `IndigoDeep` | `#061114` | Dark canvas base ("Oceanic Midnight Teal") |
| `IndigoSurface` | `#0C1D22` | Dark elevated card surface |
| `IndigoAccent` | `#32F5EA` | Primary Electric Cyan hero accent (dark mode) |
| `VioletGlow` | `#14B8A6` | Secondary Teal Aurora glow |
| `VioletDeep` | `#0A3A44` | Deep oceanic shadow accent |
| `TealAurora` | `#2CE6DC` | Tertiary Cyan mist |
| `TealDeep` | `#0E4D59` | Mesh orb #1 teal bloom |
| `ObsidianInk` | `#061114` | Dark ink used on solid white/cyan CTA buttons & active pills |
| `PearlBackground` | `#F0F7F8` | Light mode frosted aqua background |
| `PearlSurface` | `#FFFFFF` | Light mode surface |
| `IndigoLightPrimary` | `#0891B2` | Primary cyan-teal accent (light mode) |
| `VioletLightSecondary` | `#0D9488` | Secondary accent (light mode) |
| `TealLightTertiary` | `#14B8A6` | Tertiary accent (light mode) |
| `SuccessEmerald` | `#10B981` | Semantic success |
| `WarningAmber` | `#F59E0B` | Semantic warning / Deep Vault secondary |
| `DangerRose` | `#EF4444` | Semantic destructive / error |

### 2.2 Vault-Specific Tokens
- **Level 1 (`Secret Folder`):** `VaultL1Accent = #32F5EA`, `VaultL1Glow = #0D9488`, `VaultL1MeshSecondary = #064E3B`, `meshBase = #051114`.
- **Level 2 (`Deep Vault`):** `VaultL2Accent = #F43F5E`, `VaultL2Secondary = #F59E0B`, `VaultL2MeshPrimary = #2A0817`, `VaultL2MeshSecondary = #4C0519`, `meshBase = #12060B`, `background = #12070D`, `surface = #1C0C15`.

### 2.3 Text Hierarchy
- **Dark:** Primary `#F8FAFC`, Secondary `#CBD5E1`, Tertiary `#8CA6AD`
- **Light:** Primary `#061114`, Secondary `#334E54`, Tertiary `#5D7A82`
- **Overlay/Viewer White:** `#FFFFFF` (secondary `#FFFFFF` @ 72%–78% alpha), Heart icon `#F43F5E`, Viewer Trash icon `#FB7185`.

### 2.4 Glassmorphism Surface Tokens (`LuminaGlassColors`)
| Property | Gallery Dark | Gallery Light | Vault L1 | Vault L2 |
|---|---|---|---|---|
| `glassFillTop` | `White @ 9%` | `White @ 80%` | `#32F5EA @ 11%` | `#F43F5E @ 14%` |
| `glassFillBottom` | `#07161A @ 72%` | `White @ 58%` | `#061519 @ 68%` | `#1F0A14 @ 66%` |
| `glassStrongFill` | `#DE0A1B1F` (~87% alpha) | `#EBFFFFFF` (~92% alpha) | `#DF07181D` | `#DF1B0810` |
| `glassBorder` | `White @ 14%` | `White @ 90%` | `#32F5EA @ 28%` | `#F43F5E @ 34%` |
| `glassHighlight` | `Accent @ 38%` | `#0891B2 @ 18%` | `#32F5EA @ 38%` | `#F59E0B @ 38%` |
| `patternOverlay` | `White @ 4.8%` | `#061114 @ 4.5%` | `#32F5EA @ 5.5%` | `#F59E0B @ 5.5%` |
| `shimmerBase` | `#0E2127` | `#D8E6E9` | `#0D2329` | `#28111B` |
| `shimmerHighlight` | `#18363F` | `#EBF4F6` | `#163840` | `#3D1928` |

---

## 3. Typography (`Type.kt`)

Bundled local TTF fonts (`res/font/sora.ttf` & `res/font/plus_jakarta_sans.ttf`):
- **Headings / Display / Titles:** `Sora` (`SoraFontFamily`)
- **Body / Labels / Captions:** `Plus Jakarta Sans` (`PlusJakartaFontFamily`)

| Material Style | Font | Weight | Size / LineHeight | Letter Spacing |
|---|---|---|---|---|
| `displayLarge` | Sora | Bold | 40sp / 48sp | -0.5sp |
| `displayMedium` | Sora | Bold | 32sp / 40sp | -0.25sp |
| `displaySmall` | Sora | SemiBold | 28sp / 36sp | 0sp |
| `headlineLarge` | Sora | SemiBold | 26sp / 34sp | -0.2sp |
| `headlineMedium` | Sora | SemiBold | 22sp / 28sp | 0sp |
| `headlineSmall` | Sora | Medium | 18sp / 24sp | 0sp |
| `titleLarge` | Sora | SemiBold | 20sp / 26sp | 0sp |
| `titleMedium` | Sora | Medium | 16sp / 22sp | 0.1sp |
| `titleSmall` | Sora | Medium | 14sp / 20sp | 0.1sp |
| `bodyLarge` | Plus Jakarta Sans | Normal | 16sp / 24sp | 0.15sp |
| `bodyMedium` | Plus Jakarta Sans | Normal | 14sp / 20sp | 0.15sp |
| `bodySmall` | Plus Jakarta Sans | Normal | 12sp / 16sp | 0.2sp |
| `labelLarge` | Plus Jakarta Sans | SemiBold | 14sp / 20sp | 0.1sp |
| `labelMedium` | Plus Jakarta Sans | Medium | 12sp / 16sp | 0.3sp |
| `labelSmall` | Plus Jakarta Sans | Medium | 11sp / 14sp | 0.3sp |

---

## 4. Shapes & Elevation

- **`LuminaShapes`:**
  - `extraSmall`: `8.dp`
  - `small`: `14.dp`
  - `medium`: `18.dp`
  - `large`: `24.dp`
  - `extraLarge`: `30.dp`
- **Specific Corner Radii Used:**
  - Floating bottom dock, selection top bar, action bars, search bar, filter pills, circular icon buttons: `CircleShape` (50% pill/circle)
  - Album cards: `22.dp`
  - Settings cards: `22.dp`
  - Empty/onboarding showcase hero frame: `30.dp` with `1.5.dp` bright white border (`White @ 82%` in dark mode)
  - Photo grid thumbnails (adaptive to column count): `2 -> 20.dp`, `3 -> 16.dp`, `4 -> 14.dp`, `5 -> 12.dp`
  - Trash grid cards: `18.dp`
  - Media Viewer video seek capsule: `24.dp`

---

## 5. Core Reusable UI Components (`GlassComponents.kt` & `MediaGrid.kt`)

### 5.1 `MeshGradientBackground`
- Drawn with Compose `Canvas` behind the entire `NavHost`.
- **Base layer:** Solid `glass.meshBase` (`#061114`).
- **3 Animated Breathing Radial Gradient Orbs** (`16000ms` infinite reverse `FastOutSlowInEasing` drift):
  1. Top-left bloom (`0.20..0.26w, 0.14..0.18h`, radius `0.72 * max(w, h)`)
  2. Mid-right highlight (`0.82..0.76w, 0.44..0.49h`, radius `0.56 * max(w, h)`)
  3. Bottom-left depth glow (`0.25..0.33w, 0.88..0.84h`, radius `0.62 * max(w, h)`)
- **Overlay Texture Pattern:**
  - Standard Gallery & Vault L1: **Fine Diagonal Lines** (`30.dp` spacing, `0.75.dp` stroke width, ~4.8–5.5% opacity).
  - Vault L2 (`VAULT_LEVEL_2`): **Dot Grid** (`24.dp` grid spacing, `1.dp` dot radius, `VaultL2Secondary @ 5.5%`).

### 5.2 `GlassSurface`
- Vertical gradient fill (`glassFillTop -> glassFillBottom`, or `glassStrongFill -> glassStrongFill @ 94%` when `strongFill = true`).
- `1.dp` perimeter border (`glassBorder`).
- Soft drop shadow tinted by `glass.accentGlow @ 18%` ambient + `Black @ 50%` spot.

### 5.3 `CircularGlassIconButton`
- `44.dp` (default, customizable e.g. `54.dp`) circle with `minimumInteractiveComponentSize()` (48dp touch accessibility).
- Frosted vertical glass gradient + `1.dp` `glassBorder`.
- Centered `20.dp` Material Rounded icon.

### 5.4 `CollapsingGlassTopBar`
- Translucent when idle (`0%` fill, `0%` bottom divider), smoothly animating (`220ms`) to `90%` `glassStrongFill` with a `1.dp` bottom glass border when scrolled (`firstVisibleItemIndex > 0 || scrollOffset > 20`).
- **Layout:**
  - Optional leading circular `44.dp` glass `navigationIcon` container.
  - Title column (up to 2 lines; expands from `headlineLarge Bold` to `titleLarge Bold` on scroll or when `navigationIcon != null`; optional `1-line bodySmall` subtitle; supports `onTitleClick` without ripple).
  - Trailing `Row` (`8.dp` gap) for action badges & `CircularGlassIconButton`s.

### 5.5 `FloatingGlassBottomBar` (Main Navigation Dock)
- **Position:** Centered at `Alignment.BottomCenter` with `navigationBarsPadding()` + `24.dp` horizontal / `14.dp` vertical outer padding.
- **Outer Pill:** Compact centered `GlassSurface` (`widthIn(max = 286.dp)`, `height = 66.dp`, `CircleShape`, `strongFill = true`, `elevation = 20.dp`), `10.dp` horizontal inner padding.
- **4 Circular Tab Buttons (`SpaceBetween`):**
  1. `PHOTOS` (`nav_tab_photos`) — Icon: `PhotoLibrary` (Rounded when active / Outlined when inactive)
  2. `ALBUMS` (`nav_tab_albums`) — Icon: `Folder` (Rounded / Outlined)
  3. `FAVORITES` (`nav_tab_favorites`) — Icon: `Favorite` / `FavoriteBorder`
  4. `VIDEOS` (`nav_tab_videos`) — Icon: `Videocam` (Rounded / Outlined)
- **Active vs Inactive State:**
  - Each item is a `48.dp` circle (`minimumInteractiveComponentSize()`).
  - **Active:** Solid `primary` (`#32F5EA`) background, `onPrimary` (`#061114`) icon tint, `1.08f` spring bounce scale (`DampingRatioMediumBouncy`), `12.dp` cyan glow shadow, `primary @ 65%` border.
  - **Inactive:** Translucent circular glass fill (`White @ 8%` dark / `ObsidianInk @ 6%` light), `1.dp` `glassBorder`, `onSurface @ 82%` icon tint.
  - Triggers haptic feedback (`TextHandleMove`) on tap.

### 5.6 `MediaSectionsGrid` (Shared Photo/Video/Vault Grid)
- **Columns:** `2..5` columns (`AppSettings.gridColumns` default `3`), `4.dp` vertical & horizontal item spacing, `10.dp` horizontal / `8.dp` top / `110.dp` bottom content padding.
- **Date Section Headers:** Full-span `titleMedium SemiBold` labels ("Today", "Yesterday", or `"MMMM yyyy"` / month-year).
- **Tiles (`MediaGridThumbnail`, 1:1 square):**
  - Shimmer base placeholder (`shimmerBase`) with `160ms` Coil crossfade.
  - `0.5.dp` subtle hairline white border (`8%` alpha); when selected: `2.5.dp` `primary` cyan border + spring scale to `0.88f`.
  - **Video Overlay:** Dark bottom gradient + bottom-end black translucent pill (`58%` alpha) with `12.dp` `PlayArrow` icon + formatted duration (`MM:SS`).
  - **Favorite Overlay:** Bottom-start `15.dp` rose heart icon (`#F43F5E`).
  - **Selection Checkbox Overlay (when selection mode active):** Top-end `22.dp` circle (`1.5.dp` white border; cyan fill + white check when selected, `Black @ 45%` when unselected).
- **Gestures:**
  - **Two-finger Pinch-to-Zoom:** Zooming in (`> 1.24x`) decreases columns down to `2`; zooming out (`< 0.80x`) increases columns up to `5` with haptic tick. Persisted to DataStore.
  - **Long-Press + Drag Multi-Select:** Long-press selects/deselects hit item and dragging across tiles toggles contiguous tiles cleanly.
- **Fast Scroller & Date Bubble (when `entries.size > 18`):**
  - Right-edge `28.dp` drag track with a `6.dp × 44.dp` pill thumb (`glassHighlight` idle, solid `primary` when scrubbing).
  - Dragging shows a floating `GlassSurface` `CircleShape` **Date Bubble** to the left of the thumb showing the current section header ("Today", "October 2026", etc.).

### 5.7 `AnimatedEmptyState` (Also Used as Onboarding Permission Screen)
- Centered full-screen layout with vertical floating animation (`±5.dp` gentle bobbing over `2600ms`).
- **Hero Showcase Frame:** `84%` width, `240.dp` height, `30.dp` rounded corners, `24.dp` shadow with cyan ambient tint, `1.5.dp` crisp white frame border (`White @ 82%`), bottom dark vignette gradient, displaying custom artwork (`img_empty_gallery_1794970584727` or `img_empty_vault_1794970584909`).
- **Copy:** `headlineMedium Bold` centered title + `bodyMedium` centered subtitle.
- **Action Row (when `ctaLabel != null`, e.g., Permission Denied screen):**
  - Left: `54.dp` `CircularGlassIconButton` with `PhotoSizeSelectActual` icon in primary cyan.
  - Right: Full-remaining-width `54.dp` tall `CircleShape` solid **White** (dark mode) or **ObsidianInk** (light mode) CTA `Button` (`empty_state_cta`) with centered label + trailing `ArrowForward` icon.

---

## 6. Navigation Architecture (`LuminaNavRoutes.kt` & `LuminaAppRoot.kt`)

Type-safe Kotlin Serialization routes inside `SharedTransitionLayout`:

| Route | Parameters | Purpose |
|---|---|---|
| `GalleryRoute` | *(none)* | Start destination. Houses the 4 main tabs (`Photos`, `Albums`, `Favorites`, `Videos`) + permission screen. |
| `AlbumDetailRoute` | `bucketId: Long`, `albumName: String` | Media grid filtered to a specific device folder. |
| `SearchRoute` | *(none)* | Live search across filenames, album names, dates, and media types. |
| `TrashRoute` | *(none)* | Recently deleted items with 30-day countdown, Restore, and Empty/Delete Forever. |
| `SettingsRoute` | *(none)* | Theme, default grid columns, vault re-lock timeout, trash retention link, and Extra Secret Pass setup. |
| `VaultLevel1Route` | *(none)* | Level 1 "Secret Folder" (gated by `BiometricPrompt` strong biometric or device credential). |
| `VaultPasscodeRoute` | *(none)* | Numeric/keyboard passcode gate for Level 2 Vault (accessed via hidden gesture only). |
| `VaultLevel2Route` | *(none)* | Level 2 "Deep Vault" (Crimson/Amber atmosphere, separate encrypted store). |
| `MediaViewerRoute` | `source: String`, `initialIndex: Int`, `bucketId: Long` | Full-screen photo/video viewer (`GALLERY_PHOTOS`, `GALLERY_FAVS`, `GALLERY_VIDEOS`, `ALBUM`, `SEARCH`, `VAULT_L1`, `VAULT_L2`). |
| `PhotoEditorRoute` | `uriString: String`, `displayName: String` | Non-destructive image editor (crop/rotate/flip/filters/adjustments). |

**Global Overlays:**
- `SnackbarHost` positioned `Alignment.BottomCenter` with `navigationBarsPadding()` and `bottom = 92.dp` clearance above the floating bottom dock.
- Shared Element transitions link grid thumbnails (`"${source}_${ref.key}"`) seamlessly into `MediaViewerScreen`.

---

## 7. Screen-by-Screen UI/UX Breakdown

### 7.1 `GalleryScreen` (`GalleryRoute`)
- **Permission Denied State:** Renders `AnimatedEmptyState` titled `"Dive Into\nYour Memories"` with `"Allow Media Access"` CTA.
- **Partial Access Banner:** When Android 14+ limited photo access is active, shows a `16.dp`-rounded `GlassSurface` banner below the top bar with `"Limited access enabled"` + `"Select more"` text button.
- **Top App Bar (Normal Mode):**
  - 2-line bold `CollapsingGlassTopBar` titles per tab:
    - `PHOTOS`: `"Lumina\nGallery"`
    - `ALBUMS`: `"Device\nAlbums"`
    - `FAVORITES`: `"Saved\nFavorites"`
    - `VIDEOS`: `"Motion\nVideos"`
  - Trailing actions:
    1. **Count Capsule Pill** (`GlassSurface CircleShape`, `38.dp` high, hidden if `"0"`): `8.dp` cyan dot + `labelMedium SemiBold` total count.
    2. **Search Button** (`CircularGlassIconButton`, `testTag = "search_button"`).
    3. **Overflow Menu Button** (`CircularGlassIconButton`, `testTag = "overflow_menu_button"`) opening a `glassStrongFill` `DropdownMenu`:
       - `"Hidden Folder"` (`VisibilityOff` icon, `menu_hidden_folder`) -> Triggers `BiometricPrompt`.
       - `"Trash"` (`DeleteOutline` icon, `menu_trash`)
       - `"Settings"` (`Settings` icon, `menu_settings`)
- **Quick Filter Pill Row (`OceanicFilterPillRow`, Photos tab only when media exists and not selecting):**
  - Horizontal `LazyRow` of capsule pills (`48.dp` touch target wrapping `38.dp` visual pills):
    - `"All"` (`ALL`), `"Photos"` (`IMAGES`), `"Videos"` (`VIDEOS`), plus up to the top 4 albums (`ALBUM_<bucketId>`).
    - **Selected Pill:** Solid `#FFFFFF` (dark) / `#061114` (light) with high-contrast inverted text.
    - **Unselected Pill:** Frosted glass gradient + `1.dp` `glassBorder`.
  - Filtering is hoisted in `GalleryViewModel` so tapping an item opens the exact filtered list in `MediaViewerScreen`.
- **Albums Tab (`AlbumsGrid`):**
  - **Columns:** Fixed `2` columns (`GridCells.Fixed(2)`) on phones (`maxWidth < 600.dp`), `GridCells.Adaptive(180.dp)` on tablets/foldables.
  - **Album Card (`GlassSurface`, `22.dp` radius):**
    - Top cover photo (`aspectRatio(1.08f)`) with subtle bottom dark gradient scrim.
    - Bottom footer row (`12.dp × 10.dp` padding): Album name (`titleSmall SemiBold`, 1 line) + `"<N> items"` (`bodySmall`), and a **`30.dp` glowing Electric Cyan circular arrow badge** (`ArrowForward` icon in `onPrimary`) on the right.
- **Multi-Select Mode (replaces Top & Bottom bars):**
  - **SelectionTopBar:** Floating `CircleShape` `GlassSurface` with Close (`clear_selection_button`), `"<N> selected"`, and Select All (`select_all_button`).
  - **SelectionActionPillBar:** Floating `CircleShape` `GlassSurface` (`64.dp` tall, `widthIn(max = 440.dp)`) with 4 icon buttons:
    - Share (`batch_share_button`)
    - Favorite (`batch_favorite_button`)
    - Move to Hidden Folder (`batch_hide_button`)
    - Delete to Trash (`batch_delete_button`, error tint)

### 7.2 `AlbumDetailScreen` (`AlbumDetailRoute`)
- `CollapsingGlassTopBar` with circular glass Back button (`album_back_button`), album name title, and `"<N> items"` subtitle.
- Shares `SelectionTopBar` and `SelectionActionPillBar` when items are selected.
- Uses `MediaSectionsGrid` with date section headers, pinch-to-zoom, drag-select, and fast scroller.

### 7.3 `SearchScreen` (`SearchRoute`)
- **Top Search Capsule:** `GlassSurface` (`CircleShape`, `strongFill = true`), containing Back button (`search_back_button`), Search icon, `BasicTextField` (`search_input_field`, placeholder `"Search date, album, name, or type…"`), and conditional Clear button (`search_clear_button`).
- **Filter Pill Strip:** `LazyRow` of `SearchFilterChip` pills (`All`, `Photos`, `Videos`, `Favorites`), styled identically to `OceanicFilterPillRow` (solid white active pill / frosted glass inactive pill, `48.dp` accessible height).
- **Results Grid / Empty State:** `MediaSectionsGrid` (or `AnimatedEmptyState` `"No Matches Found"`).

### 7.4 `MediaViewerScreen` (`MediaViewerRoute`)
- Full-screen pure black backdrop (`backdropAlpha` fades dynamically on vertical swipe-down dismiss; vertical drag `> 260f` closes viewer).
- `HorizontalPager` (`beyondViewportPageCount = 1`) supporting swiping when photo zoom `<= 1.03x`.
- **Photo Page (`ViewerPhotoPage`):**
  - Double-tap toggles `1x <-> 2.5x` zoom; pinch-to-zoom `1x..4x` with bounded panning; single-tap toggles chrome visibility.
- **Video Page (`ViewerVideoPage`):**
  - Background Coil thumbnail prevents blank frames while buffering or on adjacent pages.
  - Uses `R.layout.view_texture_player` (`PlayerView` with `app:surface_type="texture_view"`) and `DefaultRenderersFactory(context).setEnableDecoderFallback(true)`.
  - Allocates `ExoPlayer` strictly for `isActivePage == true` and releases immediately when swiping away.
  - Shows buffering spinner, inactive-page Play badge, or frosted glass error card with `"Retry"` button on `PlaybackException`.
  - **Floating Video Scrubber Bar** (positioned `84.dp` above bottom action bar): `GlassSurface` (`24.dp` radius) with Play/Pause (`video_play_pause`), elapsed time, `Slider` tinted with `primary`, total duration, and Mute/Unmute toggle (`video_mute_button`).
- **Top Chrome Overlay:** Dark top-down gradient scrim with Back button (`viewer_back_button`), filename + full date/time, and More overflow (`viewer_more_button`):
  - *Gallery items:* Hide to Secret Folder, Rename (dialog), Set as Wallpaper (Home / Lock / Both dialog), Details.
  - *Vault items:* Move to Double Hidden Folder (Level 1 only), Unhide / Restore.
- **Bottom Floating Glass Action Pill** (`CircleShape`, `strongFill = true`):
  - *Gallery items:* Share, Favorite (`#F43F5E` when active), Edit Photo (images only), Details, Move to Trash (`#FB7185`).
  - *Vault items:* Unhide / Restore, Move to Double Hidden Folder (Level 1 only, primary cyan tint), Details, Delete Permanently (`#FB7185` with confirmation dialog).
- **Details Bottom Sheet:** `ModalBottomSheet` (`glassStrongFill`) listing Filename, Location, Date, Size, Resolution, Format, Duration, Device make/model, and EXIF camera metrics (aperture, shutter speed, ISO, focal length).

### 7.5 `PhotoEditorScreen` (`PhotoEditorRoute`)
- Dark `#070711` studio canvas with `statusBarsPadding()` & `navigationBarsPadding()`.
- **Top Bar:** Close (`editor_cancel`), `"Edit Photo"` title, `"Save"` button (`editor_save`, shows progress spinner while rendering).
- **Center Preview Stage:** Live EXIF-oriented `Bitmap` preview with GPU `ColorMatrix` filter/adjustments applied + interactive 4-corner draggable `CropOverlay` (dimmed outside crop rect, white border & `8.dp` corner circles) when `CROP` tool is active.
- **Bottom Glass Tool Panel (`GlassSurface`, `24.dp` radius, `strongFill = true`):**
  - **Crop & Rotate:** Rotate Left (`editor_rotate_left`), Rotate Right (`editor_rotate_right`), Flip Horizontal (`editor_flip`), plus `FilterChip` aspect ratios (`Free`, `1:1`, `4:3`, `3:4`, `16:9`).
  - **Filters:** Horizontal pill strip (`Original`, `Vivid`, `Noir`, `Sunset`, `Frost`, `Film`, `Emerald`).
  - **Adjust:** Three `Slider` rows (`Brightness -50..50`, `Contrast 0.6..1.5`, `Saturation 0..1.8`).
  - **Tool Switcher Row:** `Crop & Rotate`, `Filters`, `Adjust` capsule tabs.

### 7.6 `TrashScreen` (`TrashRoute`)
- `CollapsingGlassTopBar` (`"Trash"`, subtitle `"Items permanently deleted after 30 days"`) with circular glass Back button (`trash_back_button`) and `"Empty"` error-colored button (`empty_trash_button`).
- Adaptive `2+` column grid of `18.dp` `GlassSurface` square cards:
  - Top-right dark pill badge showing `"<N>d left"`.
  - Bottom card footer with individual **Restore** (`restore_trash_<id>`) and **Delete Forever** (`delete_trash_<id>`) icon buttons.
- Bottom anchored dual pill CTA bar: Outlined `"Restore All"` (`restore_all_trash`) and solid error `"Delete All"` (`delete_all_trash`), `48.dp` tall.

### 7.7 `SettingsScreen` (`SettingsRoute`)
- `CollapsingGlassTopBar` (`"Settings"`, `settings_back_button`).
- Vertical `LazyColumn` of `22.dp` rounded `GlassSurface` cards (`16.dp` internal padding):
  1. **Appearance:** `FilterChip`s (`Dark`, `Light`, `System`).
  2. **Grid Density:** `FilterChip`s (`2×`, `3×`, `4×`, `5×`).
  3. **Vault Re-lock Timeout:** 2×2 `FilterChip` grid (`Immediate`, `30 seconds`, `1 minute`, `5 minutes`).
  4. **Trash Retention:** Clickable card showing `"30 days before permanent removal"` -> navigates to `TrashRoute`.
  5. **Extra Secret Folder Pass:** Clickable card (`setting_extra_pass`, requires fresh biometric/device auth before opening dialog to create/update/remove a salted PBKDF2 passcode for Level 2).

### 7.8 Hidden Vault Screens (`VaultScreens.kt`)
- **Level 1 (`VaultLevel1Route` — "Secret Folder"):**
  - Same `MediaSectionsGrid` + `SelectionTopBar` experience, with circular Back (`vault_back_button`) and Lock (`vault_lock_button`) buttons.
  - **Secret Gesture for Level 2:** No visible button or hint exists anywhere in the UI. Tapping the `"Secret Folder"` title text **5 times within 3 seconds** (when `hasExtraPass` is configured) silently opens `VaultPasscodeRoute`.
  - Contextual Vault selection bottom pill bar (`64.dp` tall) offers **Unhide** (`vault_batch_unhide`) and **Delete** (`vault_batch_delete` with permanent wipe confirmation).
- **Passcode Gate (`VaultPasscodeRoute` — "Deep Vault Access"):**
  - Top bar with Back (`passcode_back`) and Keyboard toggle (`passcode_toggle_keyboard` to switch between numeric pad and full alphanumeric password field).
  - Center `68.dp` circular glass Shield emblem + `"Deep Vault Access"` heading + animated horizontal shake (`±18f`) & attempt/lockout feedback.
  - Obscured glass dots indicator (4–10 dots) + 3×4 tactile `72.dp` circular glass keypad (`1..9`, `BACK`, `0`, `GO`).
- **Level 2 (`VaultLevel2Route` — "Deep Vault"):**
  - Crimson Rose & Amber Gold vault atmosphere with subtle dot-grid mesh overlay.
  - Header subtitle `"Level 2  •  <N> encrypted items"`.

---

## 8. Key Interactive `testTag` Inventory

- **Bottom Navigation:** `nav_tab_photos`, `nav_tab_albums`, `nav_tab_favorites`, `nav_tab_videos`
- **Gallery Top & Filters:** `search_button`, `overflow_menu_button`, `menu_hidden_folder`, `menu_trash`, `menu_settings`, `filter_pill_<KEY>`, `empty_state_cta`
- **Albums & Grid:** `albums_grid`, `album_card_<bucketId>`, `media_grid`, `media_tile_<index>`
- **Selection:** `clear_selection_button`, `select_all_button`, `batch_share_button`, `batch_favorite_button`, `batch_hide_button`, `batch_delete_button`
- **Viewer:** `viewer_back_button`, `viewer_more_button`, `viewer_menu_hide`, `viewer_menu_rename`, `viewer_menu_wallpaper`, `viewer_menu_details`, `viewer_menu_double_hide`, `viewer_menu_unhide`, `viewer_share`, `viewer_favorite`, `viewer_edit`, `viewer_details`, `viewer_delete`, `viewer_unhide`, `viewer_double_hide`, `viewer_vault_details`, `viewer_vault_delete`, `video_play_pause`, `video_mute_button`
- **Editor:** `editor_cancel`, `editor_save`, `editor_rotate_left`, `editor_rotate_right`, `editor_flip`, `filter_<name>`
- **Search / Trash / Settings / Vault:** `search_back_button`, `search_input_field`, `search_clear_button`, `search_chip_<type>`, `trash_back_button`, `empty_trash_button`, `restore_all_trash`, `delete_all_trash`, `settings_back_button`, `setting_theme_<mode>`, `setting_grid_<n>`, `setting_extra_pass`, `vault_back_button`, `vault_lock_button`, `vault_batch_unhide`, `vault_batch_delete`, `passcode_back`, `passcode_toggle_keyboard`, `keypad_<0-9|BACK|GO>`
