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
  app labels. Labels use 22 sp and a 20 dp icon-to-label gap; row height remains
  at least 64 dp for comfortable interaction. These are Cascadia tuning values,
  not a claim that all reference settings were recovered.

## Rail tuning

Reference measurements: 16 sp regular text, 19 sp section spacing, a 64 dp pressed
offset, 150 ms press/release transition, and Android `CLOCK_TICK` haptics. Cascadia
uses these as visual/interaction targets, with its own spatial Gaussian and
Compose animation. The bell's width increases with rail length and pull depth;
its center follows the finger continuously rather than snapping to a letter.
Only horizontal translation is applied: glyph sizes and vertical slots remain
stable. Haptics fire when the selected letter changes and honor Android's system
settings, without a vibration permission or custom repeating waveform.

Existing user preferences are retained: only occupied letters, compact vertical
centering, glyphs 14 dp from the right edge, a 68 dp invisible hitbox, deeper bend
when pulling left, and **no separate magnified-letter bubble**. Unlike the
reference's complete scrollbar UI, there is no special search/favorites handle
or dynamic rail repositioning. A subtle text shadow replaces the pill backdrop.

Font/spacing/animation parity is approximate until compared hands-on on the
Pixel 8; device font scale and system haptic intensity also affect the result.
