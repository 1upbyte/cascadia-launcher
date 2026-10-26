# Typography and alphabet interaction

Cascadia uses an independent Compose rail implementation. Local reference
inspection established font identity and UI measurements; no proprietary code,
icons, branding, or layouts are included in the project.

## Redistributable font

`app/src/main/res/font/nunito_sans_variable.ttf` is **Nunito Sans Regular,
version 3.101**, a variable font with a 400–700 weight axis. It is bundled
unmodified under **SIL Open Font License 1.1**, independently of the reference
launcher's proprietary license. The font's embedded copyright and OFL metadata
were checked before inclusion. Its copyright matches the upstream license:

Copyright 2016 The Nunito Sans Project Authors
(https://github.com/Fonthausen/NunitoSans).

- License shipped inside every APK: `assets/licenses/NunitoSans-OFL.txt`.
- Upstream license: https://github.com/google/fonts/blob/main/ofl/nunitosans/OFL.txt
- This exact font binary was obtained from the locally installed reference APK;
  only this explicitly OFL-licensed font was extracted into the project.
- SHA-256: `ab6f5fdfe91ce5013fc0fd6ea0938cd7f9287103615ea6e3c577c7ebf7677e12`.
- No runtime font download or font-provider dependency.
- Nunito Sans is used throughout Material typography, including the clock and
  app labels. Labels use 20 sp and a 20 dp icon-to-label gap; row height remains
  at least 64 dp for comfortable interaction. These are Cascadia tuning values,
  not a claim that all reference settings were recovered.

## Rail tuning

Reference measurements: 16 sp regular text, 19 sp section spacing, a 64 dp pressed
offset, 150 ms press/release transition, and Android `CLOCK_TICK` haptics. Cascadia
uses these as visual/interaction targets, with its own spatial Gaussian and
Compose animation. The bell's width increases with rail length and pull depth;
its center follows the finger continuously rather than snapping to a letter.
Only horizontal translation is applied: glyph sizes and vertical slots remain
stable. While a letter is held, only its app section is shown; release restores
the full list at that section. The held section's heading is anchored near its
letter's normal vertical rail slot, shifted upward only when its rows need more
space above the bottom margin. This spacing is inside the section item, so the
clock/favorites stay offscreen. Oversized groups start at the top and can be
scrolled after release. The rail itself retains all occupied letters for
continuous dragging. Haptics fire when the selected letter changes and honor Android's system
settings, without a vibration permission or custom repeating waveform.

Existing user preferences are retained: only occupied letters, lower-right placement
with a 192 dp margin above the safe-area bottom, glyphs 14 dp from the right edge, a 68 dp invisible hitbox, deeper bend
when pulling left, and a **48 dp circular selected-letter preview beside the finger**. Leftward pull extends up to 280 dp, bounded by available screen space. Unlike the
reference's complete scrollbar UI, there is no special search/favorites handle
or dynamic rail repositioning. A subtle text shadow replaces the pill backdrop.

## Overall layout

`LauncherLayout.kt` shares geometry between home and browsing: 28 dp left inset,
48 dp icons, 20 dp icon/label gap, 20 sp labels, and 64 dp minimum rows. The clock
uses 48 sp with a 16 sp locale-aware weekday/date and a 24 dp gap to favorites.
The reference's 28 dp horizontal margin, 20 dp icon gap, and 48 dp default clock
size informed these choices; sp text units deliberately preserve Android font
scaling. Icon size, row sizing, and adaptive vertical placement are independently
tuned approximations, not a recovery of every reference preference.

The home group is vertically balanced with an upper bias (at most 28% top space,
at least 25 dp); more favorites shift the group upward. The brand/header and
recurring help are removed from populated homes. Settings is available in the app-list footer;
empty-state help and default-home onboarding still appear when needed. Smaller
section headings align to the icon column. Long labels ellipsize instead of
changing row heights; accessibility launch labels retain the full app name.

The rail keeps its previously approved placement/effect. There are no added
weather, notification, search, or proprietary widget elements. The phone was
locked during attempted reference capture, so exact custom placement could not
be compared visually. No attempt was made to bypass the lock screen.

Font/spacing/animation parity is approximate until compared hands-on on the
Pixel 8; device font scale and system haptic intensity also affect the result.

## Icon mask reference

The locally available Niagara APK distinguishes a continuous cubic squircle
from its rounded-square option. Cascadia's Squircle setting uses an independent
Compose path with four symmetric cubic segments: the endpoints sit at each
edge midpoint, and control points sit 10% of the width/height from the corners.
This matches the reference mask geometry at any icon size, replacing the earlier
30%-corner rounded rectangle. The same outline clips regular app icons and
shapes footer backgrounds. Square/Circle choices and saved preferences remain.

## Back gestures at the alphabet edge

Local inspection of the reference APK's gesture-exclusion manager found that
alphabet exclusion rectangles are registered only in `HOME` (`favorites`), and
cleared in `ALL` (`all_apps`). The relevant state gate is in the decoded
`TCi1D4c11KijghM.InBLWr22CpY`; `VgNmRrRI7OfRSat383MKj` identifies those states.
`M8R9Je4rNZrPTHVIZ6YAeTAkUfT` aggregates the rectangles and
`xgUdrIK7jFWDXl8DfnKCy` applies them to Android. These are inspection references;
no reference implementation is included in Cascadia.

Cascadia initially followed that behavior. At the user's request, it now leaves
the system Back edge available on favorites as well as browsing. The rail uses
no system-gesture exclusion. Its wide touch area still handles taps and letter
scrubbing, preserving the released list position. An inward edge swipe can invoke
system Back at the rail's height on either screen. Back from browsing restores
favorites; Back on favorites keeps the launcher open and scrolls to the top.
