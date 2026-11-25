You are working on Lumina, my Android gallery app (Kotlin, Jetpack Compose, Material 3).
`design.md` describes the current UI. Restyle the app to the "Liquid Glass" direction below.
This is a visual and UX refresh. Do not change functionality.

## Hard constraints
- Keep every route, ViewModel, data layer, gesture (pinch-zoom columns, long-press drag-select, fast scroller, swipe-to-dismiss viewer), vault flow (including the 5-tap secret gesture and FLAG_SECURE), and shared-element transitions.
- Keep every existing `testTag` and its meaning.
- Keep vault atmospheres (L1 teal, L2 crimson/amber) and the viewer's Palette-based accent. Reuse the existing `LuminaGlassColors` and theme plumbing; extend them rather than replacing them.
- Keep the Oceanic Midnight accent palette (#32F5EA etc.). Optionally add one faint violet orb (#7C5CFF at ~20% alpha) and one faint rose orb to the GALLERY mesh background only, behind a single constant so it is easy to remove.
- Typography: use Plus Jakarta Sans for headings too (weights 700/800, tight letter spacing -0.02em). Drop Sora from display/headline styles but keep the font file available.

## 1. Glass material (rebuild `GlassSurface`)
Real backdrop blur, not just a translucent gradient:
- Use a backdrop-blur approach that blurs the content behind the surface (e.g. the Haze library, `dev.chrisbanes.haze`, with a `hazeSource` on the grid/photo content and `hazeEffect` on glass chrome; check the current API and version before using it). Note that `Modifier.blur` blurs the element itself, not what is behind it, so it is not enough.
- Each surface is: blur ~24-30dp with saturation boost, plus a dark tint layer (~34-50% `glassFillBottom`) so white text stays readable, plus a diagonal white gradient overlay (top-left 20% to bottom-right 5%).
- Add a 1dp border at ~30% white, plus a top inner highlight line (1dp, ~55% white) and a faint bottom inner line (~10%), and a soft drop shadow (black ~30%).
- Add a `strongFill` variant (blur ~34dp, tint ~50%) for sheets and the dock.
- Fallbacks: below API 31, or when the user enables "reduce transparency/animations", fall back to the current opaque `glassStrongFill` style.
- Keep body text on glass at 4.5:1 contrast or better (secondary text white at >= 84% alpha).

## 2. Floating dock (`FloatingGlassBottomBar`)
- One pill (height 70dp, full width minus 32dp margins, radius 35dp, `strongFill`).
- Active tab becomes a labelled pill: icon + label ("Photos", "Albums", "Favorites", "Videos"), 54dp tall, with a glass-highlight fill (white 42% to 16% vertical gradient, 1dp border at 50% white, inner top highlight). Keep the spring bounce.
- Inactive tabs: 54dp circular icon-only targets, no fill, icon at ~92% white.
- Animate the active pill's width and position between tabs (`animateContentSize` or `animateDpAsState`), so it feels like liquid moving.
- Keep haptics and `nav_tab_*` tags. Content must scroll visibly underneath the dock (bottom content padding stays ~110dp).

## 3. Gallery / Photos screen
- Header: small date line ("Thursday, 19 Nov") above a large "Gallery" title (36sp, weight 800), replacing the 2-line "Lumina Gallery" title. Search and overflow stay as 46dp circular glass buttons; keep the count capsule.
- Replace the filter pill row with a glass segmented control (single rounded glass track, 48dp tall, the selected segment shown as a raised glass pill with highlight). Segments: All, Photos, Videos plus a "More" overflow for album filters. Keep the `filter_pill_<KEY>` tags and filtering behaviour.
- Add a Featured card above the grid: 236dp tall, radius 32dp, full-bleed image (most recent favorite, or newest photo), a "FEATURED" glass chip top-left, and a glass caption plate at the bottom (title, "N photos · date", circular favorite button). Tapping opens the viewer.
- Grid: keep date section headers and adjustable columns. Radius per column count stays. Softer shadows. Favorited tiles show a small glass circle with a heart (top-right) instead of the bare heart.

## 4. Albums tab
- Add a hero album card (largest or most recent album) with fanned/rotated thumbnails, plus a glass caption plate with name, count and a circular arrow button.
- Album cards: full-bleed cover (radius 28dp) with a floating glass label plate inset 8dp at the bottom (name + "N items") instead of a separate footer row. Keep `album_card_<bucketId>`.
- Below the grid, add smart rows (Favorites, Videos, Places if available): 60dp glass rows with a tinted circular icon, label and count.
- Add a glass search field at the top that opens `SearchRoute` on tap.

## 5. Media viewer
- Keep the black backdrop and gestures. Chrome becomes floating glass instead of gradient scrims: circular glass Back and More buttons, plus a centred glass title pill (filename + "12 of 48").
- Replace the bottom action pill with a glass sheet (radius 40dp, `strongFill`): drag handle, title + date line, a favorite button (rose tint when active), metadata chips (location, camera/lens, file size), a thumbnail filmstrip of neighbouring photos (current one enlarged with a white ring), and action buttons: Share (primary glass), Edit, Delete (icon).
- The sheet collapses to a slim action pill on single tap (chrome toggle) and expands on swipe-up into the existing Details content. Keep all `viewer_*` tags and the vault variants of the actions.
- Video scrubber stays above the sheet, restyled with the same glass material.

## 6. Other screens
- Search, Trash, Settings, Vault screens, Editor: apply the new glass material, header style and segmented control/chips only. No layout or logic changes.

## 7. Motion
- Mesh orbs keep their slow drift.
- Glass elements animate in with a short fade + 8dp upward settle (200-260ms).
- Active tab pill and segmented selection use spring animations.

## Deliverable and process
1. Before coding, list the files you will touch and your plan for the backdrop-blur approach. Wait for my OK only if you find a blocker.
2. Implement in small steps: glass material, dock, Photos screen, Albums, Viewer, then the remaining screens. Make sure the project builds after each step.
3. Add `@Preview`s for the glass components in dark, light and vault L2 themes.
4. At the end, summarise what changed and list any places where behaviour might have shifted.