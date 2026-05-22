---
name: Debbie
description: A terminal-native Android client for Real-Debrid, kept tidy.
colors:
  paper: "#F5F3EF"
  bone: "#FFFCF6"
  ink: "#191713"
  char: "#2A2721"
  mute: "#8A8783"
  faint: "#D9D6D0"
  dust: "#ECEAE5"
  signal: "#D94A2B"
  sap: "#3A6B3F"
  amber: "#C68A1E"
  dark-paper: "#12140F"
  dark-bone: "#1A1D17"
  dark-ink: "#F2F1EB"
  dark-mute: "#8F948D"
  dark-faint: "#2B302B"
  dark-signal: "#FF8C66"
  dark-sap: "#8ACB87"
  dark-amber: "#E2B65A"
typography:
  display:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "34sp"
    fontWeight: 700
    lineHeight: "40sp"
  headline:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "30sp"
    fontWeight: 700
    lineHeight: "36sp"
  title:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "17sp"
    fontWeight: 700
    lineHeight: "23sp"
  body:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  caption:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
  label:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "12sp"
    fontWeight: 700
    letterSpacing: "normal"
  micro:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "11sp"
    fontWeight: 700
rounded:
  square: "0dp"
  pill: "999dp"
spacing:
  micro: "6dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "20dp"
  xxl: "24dp"
  xxxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.square}"
    padding: "12dp 16dp"
    typography: "{typography.label}"
  button-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.square}"
    padding: "12dp 16dp"
    typography: "{typography.label}"
  input:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.square}"
  card:
    backgroundColor: "{colors.bone}"
    textColor: "{colors.ink}"
    rounded: "{rounded.square}"
    padding: "16dp"
  list-row:
    backgroundColor: "{colors.bone}"
    textColor: "{colors.ink}"
    rounded: "{rounded.square}"
    padding: "12dp"
  pill:
    rounded: "{rounded.pill}"
    padding: "6dp 12dp"
    typography: "{typography.micro}"
---

# Design System: Debbie

## 1. Overview

**Creative North Star: "The Field Instrument"**

Debbie looks like a precision tool, not an app. A multimeter, a label printer, a terminal: something with one job, a fixed typeface, and no opinion about delighting you. Everything is monospace. Everything is square-cornered. Surfaces are flat sheets of warm paper stacked on darker paper, separated by hairline rules rather than shadow. There is exactly one loud color, and it is used for warnings, focus, and the current selection, never decoration. The whole system reads as competent and unhurried, the way good instrumentation does.

This is a deliberate rejection of four things. It is **not a generic Material 3 app**: no rounded cards, no tonal elevation, no dynamic-color purple, no FABs, no stock Material controls. It is **not a torrent-site UI**: no neon, no clutter, no ad-shaped buttons, even though it handles the same payload class. It is **not a consumer streaming app**: no poster art, no carousels, no gradients. And it is **not a SaaS dashboard**: no hero-metric template, no chart filler, no identical icon-heading-text card grids. Debbie states facts in monospace and gets out of the way.

**Key Characteristics:**
- One typeface, JetBrains Mono, for everything: titles, data, prose, labels.
- Zero corner radius on every container, button, field, and control. Pills and the progress bar are the only rounded shapes.
- Flat by default: depth comes from hairline borders and warm tonal layering, never shadow.
- A single Signal-orange accent, rationed to one element per screen.
- Warm neutral palette (ink on paper), with a true dark inverse.
- Hierarchy carried by weight and opacity tiers first, size second.

### The Hierarchy Mechanism

Because the system has one typeface and effectively two weights (Regular and Bold), hierarchy is built primarily from **weight and opacity**, with **size** as the secondary lever. Memorize the opacity tiers; they replace the color variety a multi-hue system would use.

**The Four-Tier Rule.** All text and iconography is Ink (`onSurface`) at one of exactly four opacities. Snap to these; do not invent intermediate alphas.

| Tier | Opacity | Use |
|---|---|---|
| Primary | 100% | Headlines, titles, values, primary labels, the thing being read. |
| Secondary | 72% | Supporting body, the trailing value in a row, active-but-not-focal text. |
| Muted | 60% | Captions, key names, subtitles, placeholder-adjacent labels. |
| Faint | 40% | Disabled content, decorative meta (hashes), inert chevrons, placeholders. |

Disabled *fills* use Ink at 18%; disabled *content* uses the Faint tier (40%).

### The Spatial System

Spacing is a strict 4dp grid. Pick a step; never invent an intermediate value. **6dp is the one named sub-grid exception**, reserved for pill vertical padding and the bottom-nav top inset where 4 is too tight and 8 too loose.

| Token | Value | Where it's used |
|---|---|---|
| `micro` | 6dp | Pill vertical padding; bottom-nav top inset. The only off-grid value. |
| `xs` | 4dp | Icon-button cluster gaps; sub-label gaps; mode-card internal gap. |
| `sm` | 8dp | Inline metadata gaps; button pairs; intra-row vertical rhythm; list points. |
| `md` | 12dp | List-row inner padding; button vertical padding; detail / downloads / account list gaps; dialog field stacks. |
| `lg` | 16dp | Card inner padding; button horizontal padding; dashboard list gap; two-up column gaps. |
| `xl` | 20dp | Screen horizontal margins. |
| `xxl` | 24dp | Screen top and bottom margins, added to system-bar insets. |
| `xxxl` | 32dp | Major section breaks on tall layouts. |

**Screen frame.** Every primary screen is a `LazyColumn` filling the viewport, with content padding of 20dp horizontal (`xl`) and (system inset + 24dp `xxl`) top and bottom. Items are separated by a single `Arrangement.spacedBy`: **16dp on the dashboard, 12dp on detail / downloads / account**. Do not add ad-hoc `Spacer`s between list items; the arrangement owns vertical rhythm. Auth screens (welcome, login) are the exception: they are a full-height `Column`, not a list, and use 20dp side padding (16dp under 380dp width).

**Responsive auth.** Welcome and login adapt at two breakpoints: `tightHeight` (< 660dp tall) compresses spacers and steps the display headline down (welcome to 32sp, login to 24sp); `compactWidth` (< 380dp) takes a mid display step and tightens side padding. App screens do not restyle by size; they reflow as lists.

### Motion Discipline

Motion conveys state, never decoration. Two duration tokens, both ease-out (decelerate, no bounce, no elastic):

- **120ms** for instant feedback (press tint).
- **180ms** for state transitions (focus border, selection, color change).

The **skeleton pulse** (opacity 8% to 16% Ink, 900ms reverse loop) is the single ambient animation in the app and is held static under reduced-motion. There are no orchestrated entrances; a tool loads into its task, it does not perform.

## 2. Colors

A warm grayscale of inks and papers, lifted only by three functional status hues. The neutrals are tinted toward warm (never pure black or white); the accents exist to mean something, not to brighten the page.

### Role mapping (read this first)

Two words both want to be called "primary." Keep them separate:

- **Action color = Ink.** The primary *action* surface (solid buttons) is Ink, never the accent. This is the darkest neutral, not a hue.
- **Accent / state color = Signal.** Signal occupies Material's `primary` slot and is used only for focus, the current selection, in-progress indication, destructive intent, and error. It is a state signal, not an action color.

When code reads `colorScheme.primary`, that is Signal (the accent). When you want a primary button, fill with `onSurface` (Ink). Do not conflate them.

### Primary
- **Ink** (#191713): The near-black warm charcoal that carries primary text and is the fill of action buttons. The darkest thing on a light screen.
- **Char** (#2A2721): A softer ink for the pressed state of Ink surfaces and secondary structure.

### Accent / State
- **Signal** (#D94A2B): The one loud color. Input focus, the progress fill, the current selection, destructive actions, and error pills. Dark theme brightens it to coral (#FF8C66) to survive on dark paper.

### Status
- **Sap** (#3A6B3F): A muted forest green meaning "ready / done." Always paired with a text label.
- **Amber** (#C68A1E): A dark gold meaning "in progress." Always paired with a text label.

### Neutral
- **Paper** (#F5F3EF): The light background. Warm off-white, the sheet everything sits on.
- **Bone** (#FFFCF6): The slightly brighter surface for cards and rows, a half-tone above Paper.
- **Dust** (#ECEAE5): A faint fill tone for subtle separation and the pressed-state tint base.
- **Faint** (#D9D6D0): Hairline borders and dividers, the system's structural line.
- **Mute** (#8A8783): Reference value for the Muted text tier.
- **Dark Paper** (#12140F) / **Dark Bone** (#1A1D17) / **Dark Ink** (#F2F1EB) / **Dark Faint** (#2B302B) / **Dark Mute** (#8F948D): The dark-theme inverse, equally warm.

### Status Tone Mapping

Status hue is derived from the transfer state, never set by hand (mirrors `statusTone` / `statusLabel`):

| State | Tone | Pill label |
|---|---|---|
| `downloaded`, `ready` | Sap (green) | READY |
| `downloading` | Amber | DOWNLOADING |
| `uploading` | Amber | UPLOADING |
| `queued` | Mute | QUEUED |
| `waiting_files_selection` | Mute | NEEDS FILES |
| `magnet_resolved` | Mute | RESOLVING |
| `magnet_error`, `virus`, `dead`, `error` | Signal (orange) | MAGNET ERROR / VIRUS / DEAD / ERROR |

Mute is the default for any state without a strong signal. Only error states earn Signal.

### Named Rules
**The One Signal Rule.** Signal appears on at most a single conscious element per screen: a focused field, the active progress bar, the current selection, or an error. If two things are orange at once, one is wrong. Signal is **never decorative**: bullet points, dividers, and ornamental marks use Ink at a tier, not Signal.

**The Labeled Status Rule.** Sap, Amber, and Signal never communicate alone. Every status hue is bound to an uppercased text label. Color is reinforcement, never the only channel. This is a hard accessibility floor.

## 3. Typography

**The one font:** JetBrains Mono (with `ui-monospace, monospace` fallback), at Regular (400) and Bold (700). Medium (500) is used only for the unselected bottom-nav label.

**Character:** Monospace is the entire identity. There is no second family. Fixed-width type makes numbers, hashes, sizes, and speeds line up into columns and reads as instrumentation. The screen title and a file size come off the same machine. The handful of descriptive sentences in the app (welcome, login) are short enough that mono carries them without strain; the readability cost is paid back in coherence.

### Hierarchy

Sizes are a fixed sp scale (no fluid clamp). Two tiers: a **display tier** for rare screen-defining moments, and a tight **working tier** where weight and opacity, not size, do most of the separating.

- **Display** (Bold, 34sp / 40 line; responsive 24-44sp): Auth screen headlines only, hard-wrapped across lines like a prompt ("Sign in.\nWe stay out\nof your way.").
- **Headline** (Bold, 30sp / 36 line): In-app screen titles via `ScreenHeader`. May wrap to 2 lines.
- **Title** (Bold, 17sp / 23 line): Section and card headers ("Recent torrents", "Select files").
- **Body** (Regular, 14sp / 20 line): The default. All working text, data, and short prose. Replaces the former 14/15sp split.
- **Caption** (Regular, 13sp / 18 line): Subtitles, secondary metadata, the left label in a KeyValue row.
- **Label** (Bold, 12sp, UPPERCASE): Buttons, key names, section eyebrows.
- **Micro** (Bold, 11sp UPPERCASE; pills 10sp; nav 9sp): Stat captions, pill text, nav labels. The smallest readable instrument markings.

### Named Rules
**The One Font Rule.** Everything is JetBrains Mono. There is no prose exception, no display face, no system-sans fallback for "readability." If text needs to feel different, change its weight, opacity tier, or size, not its family.

**The Uppercase-Label Rule.** Every micro-label (button text, pill text, stat caption, nav label, key name) is uppercased. It signals "machine readout" and separates labels from the data they describe.

## 4. Elevation

Debbie is flat. There are no drop shadows anywhere; `elevation` is explicitly zeroed on buttons, the bottom bar, and dialogs. Cards are defined by a 1px border, not a shadow. Depth is communicated entirely through warm tonal layering (Bone surfaces sit a half-tone above the Paper background) and hairline Faint rules. A card is "raised" only in the sense that it is a brighter sheet outlined by a thin rule.

### The Hairline Vocabulary

Every structural line is 1px, drawn in Faint, distinguished only by opacity. Two values, no more:

- **Structure:** Faint at **72%** (`outline.copy(alpha = .72f)`). Cards, list rows, mode cards, the notice box, skeletons, the bottom-bar top rule. (The previous 68% row value is retired; everything structural is 72%.)
- **In-card divider:** Faint at **55%**. The rule between stacked direct links.
- **Selected / focused:** not a Faint value at all. Selection thickens the border to full Ink; focus is a 2dp Signal ring (see Components).

### Named Rules
**The No-Shadow Rule.** Nothing casts a shadow. To make a surface distinct, lift its tone (Paper to Bone) or outline it with a 1px Faint border. Shadows belong to Material apps, and this is not one.

**The Hairline Rule.** Structure is drawn at exactly 1px, never thicker, never as a colored accent stripe. A structural line's only variable is its Faint opacity (72% or 55%).

## 5. Components

Every interactive component defines, at minimum: **default, pressed, focused, disabled**, plus **selected** or **loading** where they apply. Values in parens are exact and normative.

### Universal interaction states

These apply to every custom-clickable surface (buttons, rows, cards, pills, toggles, nav cells). Do not fall back to Material defaults.

- **Pressed:** a flat Ink overlay at 8% (no Material round ripple, which is off-brand on square surfaces). 120ms ease-out.
- **Focused (keyboard / d-pad / switch-access):** a 2dp Signal ring drawn just inside the element's square edge. 180ms ease-out. Every clickable must show it; this is the one focus treatment.
- **Disabled:** fill drops to Ink 18%, content to the Faint tier (40%).
- **Touch target:** minimum 48dp by 48dp. Visually small controls (pills used as filters, the 15dp brand mark) must expand their clickable bounds to 48dp without growing visually.

### Buttons

Two button mechanisms. Both are square (0dp), flat, and label in uppercased Bold mono 12sp on a single line.

- **`DebbieButton` (primary action):** Solid Ink fill (`onSurface`) with background-colored text, inverting cleanly between themes. Padding 12dp vertical / 16dp horizontal. Pressed darkens to Char. Disabled: 18% fill, 40% text.
- **`DebbieOutlinedButton` (secondary):** Transparent fill, 1px Ink border at 80%, Ink text. Same padding. The **destructive** variant swaps border and text to Signal ("Delete torrent"). Disabled: 35% border and text.
- **`LoginPrimaryButton` (auth only):** A full-width Ink bar with a leading 22dp icon and centered label. Disabled drops the whole row to 42% alpha. Used on welcome and login only; never inside app screens.

**Rule:** Primary actions are solid Ink, never Signal. Signal on a button means destructive, nothing else.

### Pills (`Pill`)

- **Shape:** Fully rounded (999dp); with the progress bar, the only rounded shapes in the system.
- **Type:** Uppercased Bold mono, 10sp. Padding 12dp horizontal / 6dp vertical.
- **Border:** 1px Faint at 80%; full Ink when selected.
- **Status variant (read-only):** Background tinted to 11% of the status hue (Sap / Amber / Signal), text in the full hue. Mute tone uses the Bone surface with Muted (60%) text. No press/focus state; it is not interactive.
- **Filter variant (interactive):** Solid primary (Signal) fill, `onPrimary` text when selected. Carries the universal pressed/focus states and a 48dp hit target.

### Cards & List Rows

Two container weights. **Never nest one inside the other**, and never stack two cards deep.

- **`DebbieCard` (detail container):** Bone surface, 1px Faint border at 72%, square, **16dp** inner padding, **12dp** between children. Grouped detail: status block, account info, settings, link lists. Not clickable.
- **List row (`HomeTorrentRow`):** Bone surface, 1px Faint border at 72%, square, **12dp** inner padding, **8dp** between children, fully clickable with the universal pressed/focus states. The repeating unit in lists.

Lists are flat sequences of rows separated by the screen's `spacedBy` gap, not stacked cards.

### Inputs / Fields

Two treatments, both square (0dp):

- **`DebbieInput` (in-app):** Material `OutlinedTextField`, mono body, 1px Faint border at rest, **border switches to Signal on focus** (180ms), floating label. Search and the add-magnet field.
- **`LoginApiKeyInput` (auth):** A bordered box (1px Ink) with a Muted uppercased caption above a `BasicTextField`. Bold mono 14sp value, Faint-tier (40%) placeholder ("Paste token"), Ink cursor, Signal ring on focus. Login only.

### Toggle (`DebbieToggle`)

Replaces the Material `Switch`. A square track (0dp), 1px Ink border, that holds a square Ink thumb. **Off:** thumb left, transparent track. **On:** thumb right, Signal track fill with Paper thumb. Thumb slides on a 180ms ease-out. Carries focus ring and 48dp target. No rounded Material switch anywhere.

### Checkbox (`DebbieCheckbox`)

Replaces the Material `Checkbox`. A 20dp square box, 1px Ink border, 0dp radius. **Unchecked:** empty. **Checked:** Ink fill with a Paper checkmark (or Signal fill in selection-emphasis contexts like file selection). Pressed and focus states per universal rules. No rounded Material checkbox.

### Selectable Card (`LoginModeCard`)

The canonical mutually-exclusive choice (OAuth / API key). 1px border, 12dp padding, 4dp internal gap. **Unselected:** Faint border at 72%. **Selected:** full Ink border. Title Bold mono 14sp, subtitle mono caption at Muted. Reuse this instead of radio buttons.

### Navigation

- **Bottom bar (`AppScaffold`):** Three destinations (Home, Downloads, Account), flat, no elevation, on the background surface. A 1px Faint top rule at 72% separates it from content. Row padding 6dp top (`micro`) / 8dp bottom.
- **Nav cell:** 62dp tall. A 2dp Ink indicator (28dp wide) sits at the very top, shown only when selected. Icon 20dp, then an uppercased mono label at 9sp, 4dp below. Selected content is Primary tier and Bold; unselected is Muted tier and Medium. No pill, no fill, no Material ripple; uses the flat pressed tint.
- **Screen header (`ScreenHeader`):** Replaces the app bar. An optional caption subtitle at Muted (e.g. the date) sits 4dp above a Headline (30sp Bold mono) title, which may hard-wrap to 2 lines. An optional trailing `IconButton` is pushed right with a 12dp gap. Do not hard-wrap dynamic content (usernames, filenames) with literal `\n`; let it ellipsize.
- **Detail back-row:** A back `IconButton`, a weighted single-line filename (Bold mono 14sp), and a refresh `IconButton`, 4dp apart. Detail uses this instead of `ScreenHeader`.

### Signature Components

- **Stat block (`Stat`):** A `DebbieCard` with an uppercased micro caption (11sp, Muted) over a large Bold mono value (24sp / 28 line, single line). A gauge readout, never the SaaS hero-metric card.
- **KeyValue row (`KeyValue`):** Caption-tier label pushed left, Bold-mono value pushed right, full width, center-aligned. The workhorse for all detail data.
- **Progress bar (`ProgressBar`):** A 7dp fully-rounded track in Faint at 70% with a Signal fill clipped to the same radius.
- **Empty state:** A plain `Column` (home) or `DebbieCard` (elsewhere) with a Title heading and a Secondary-tier body line. No illustration, no centered art, no extra CTA beyond the screen's existing affordance.
- **Skeleton row:** Matches the real row's frame (Bone, 72% border, 12dp padding) filled with `SkeletonBlock`s pulsing 8%-16% Ink over 900ms reverse. Show 4 while loading; hold static under reduced-motion.
- **Add-torrent dialog:** A Material `AlertDialog` forced to square corners on the Surface color, holding a multi-line magnet `DebbieInput` and a full-width "Upload .torrent" `DebbieButton`. Confirm/dismiss are text buttons. Modals are reserved for this one create action; everything else is inline.

## 6. Do's and Don'ts

### Do:
- **Do** set everything in JetBrains Mono. No exceptions, no second family.
- **Do** build hierarchy from weight (Bold/Regular) and the four opacity tiers (100 / 72 / 60 / 40%) first, size second.
- **Do** keep every container, button, field, and control at 0dp radius. Reserve 999dp for pills and the progress bar only.
- **Do** pull every spacing value from the 4dp grid (4 / 8 / 12 / 16 / 20 / 24 / 32), with 6dp the single named exception.
- **Do** build screens as a `LazyColumn`, 20dp sides / 24dp ends, single `spacedBy` gap (16dp dashboard, 12dp elsewhere). Let the arrangement own rhythm.
- **Do** draw structure as 1px Faint hairlines at 72% (containers) or 55% (in-card dividers); selection thickens to Ink, focus is a 2dp Signal ring.
- **Do** give every clickable a pressed tint (Ink 8%), a focus ring, and a 48dp touch target.
- **Do** ration Signal to one element per screen (focus, current selection, active progress, destructive action, or error).
- **Do** derive status color and label from the state table; pair every hue with an uppercased label.
- **Do** reuse `LoginModeCard` for mutually-exclusive choices and `KeyValue` for label/value detail.
- **Do** write copy that states facts: status, size, speed, ETA, days left. Assume the user knows the domain.

### Don't:
- **Don't** introduce a second typeface, a system-sans fallback, rounded cards, tonal shadows, dynamic-color purple, or FABs.
- **Don't** ship stock Material `Switch` or `Checkbox`; use `DebbieToggle` and `DebbieCheckbox`.
- **Don't** rely on Material's round ripple on square surfaces; use the flat pressed tint.
- **Don't** invent intermediate spacing values or text opacities; snap to the grid and the four tiers.
- **Don't** nest containers: no card in a row, no row in a card, never two cards deep.
- **Don't** make a primary button Signal-colored, or use Signal decoratively (bullets, ornaments). Solid Ink for action; Signal only for state and destruction.
- **Don't** hard-wrap dynamic text (usernames, filenames) with literal newlines; ellipsize instead.
- **Don't** drift toward torrent-site UI: no neon, no clutter, no ad-shaped download buttons.
- **Don't** add poster art, carousels, or glossy gradients. Debbie manages transfers, it does not sell content.
- **Don't** build the SaaS dashboard cliché: no hero-metric template, no gradient accents, no identical card grids, no chart filler.
- **Don't** cast a shadow on anything; lift the tone or draw a 1px border.
- **Don't** use color as the only carrier of status meaning.
- **Don't** reach for a modal beyond the add-torrent dialog. Everything else is inline or a screen.
- **Don't** write persuasive or benefit-led copy. No marketing voice.
