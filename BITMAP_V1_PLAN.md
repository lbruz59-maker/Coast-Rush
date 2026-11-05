# Coast Rush — Bitmap V1

This branch is the isolated development line for the bitmap-rendered edition of Coast Rush.

## Safety rule
- The `main` branch remains the stable reference and is not modified by this work.
- All bitmap-rendering changes, asset imports, and APK builds for this edition stay on `coast-rush-bitmap-v1`.
- Do not merge into `main` until the new edition has been tested on Android and Fire TV and explicitly approved.

## Visual direction
Pre-rendered 3D arcade sprites with an OutRun-inspired presentation:
- Vehicles: separate transparent views for straight, left, right, and drift poses; never stretch one view to fake all directions.
- Characters: large, readable sprites with consistent proportions.
- Scenery: individual transparent building and roadside sprites; WebP for opaque backgrounds and large scenery where suitable.
- Effects such as smoke, wheel animation, and glow remain dynamic where that preserves motion.

## Rendering and performance
- Use sprite atlases and shared decoded bitmap caches.
- Keep image dimensions and memory budgets explicit; release unused decoded variants.
- Preserve the existing road horizon and 72 road segments.
- Keep a fallback to the current Canvas renderer until each bitmap family is validated.
- Avoid replacing a whole asset family in one step; migrate and test one family at a time.

## Migration order
1. Add the bitmap asset loader and fallback path.
2. Migrate the player car and verify all steering/drift poses.
3. Migrate traffic vehicles.
4. Migrate roadside people and characters.
5. Migrate buildings and scenery.
6. Profile on target hardware, then package a test APK.

## Acceptance checks
- Existing menu, touch controls, music toggle, and gameplay remain functional.
- No missing-image placeholders or broken transparent edges.
- No loss of road horizon, road detail, or vehicle readability.
- APK build succeeds before any test release is presented as ready.
