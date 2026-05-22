# Product

## Register

product

## Users

People who already pay for Real-Debrid and want to drive it from their phone instead of the website. They are technically comfortable: they know what a magnet link is, what unrestricting a link means, and what a torrent's status codes imply. They reach for Debbie in short, purposeful bursts: paste a magnet, check whether a transfer is ready, grab a direct link, leave. They are not browsing for entertainment and they do not want to be onboarded, upsold, or walked through anything.

## Product Purpose

Debbie is a focused Android client for Real-Debrid. It does four things and refuses the rest: add torrents (magnet links and `.torrent` files), watch active transfers, open or copy ready downloads, and manage the account session. Authentication is OAuth device flow or a pasted API token, and the bearer token lives in encrypted on-device storage and nowhere else. Success is the app disappearing into the task: the user accomplishes what they came for in a few taps and closes it without friction, without ads, and without their credentials ever leaving the device.

## Brand Personality

Technical, blunt, fast. Debbie speaks in short declarative lines, often broken across rows like a terminal prompt ("Real-Debrid, kept tidy.", "Sign in. We stay out of your way."). It states facts, not benefits. It assumes competence rather than explaining basics. The emotional payload is trust through restraint: nothing is hidden, nothing is decorated, nothing wastes the user's time. Monospace everywhere is a deliberate signal, this is a tool, not a product trying to charm you.

## Anti-references

- **Generic Material 3 apps.** No rounded cards, no elevation shadows, no dynamic-color purple theming, no floating action buttons. The default Android look is the thing Debbie is reacting against.
- **Piracy and torrent-site UI.** No dark-neon clutter, no ad-laden download buttons, no dense sketchy tables. Debbie handles the same payload class but reads as a clean instrument, not a warez page.
- **Consumer streaming apps.** No poster art, no carousels, no glossy gradients, no entertainment-app polish. Debbie manages transfers, it does not sell content.
- **SaaS dashboard clichés.** No hero-metric template, no gradient accents, no identical icon+heading+text card grids, no chart-heavy admin filler.

## Design Principles

- **Get out of the way.** Every screen serves one task. If an element does not help the user add, watch, or retrieve, it does not belong.
- **State facts, not benefits.** Copy reports what is true (status, size, speed, ETA, days left). It never persuades.
- **Assume competence.** Surface raw, useful detail (hashes, hosts, status codes) rather than dumbing it down. The user knows what they are doing.
- **Privacy is structural, not a feature.** Tokens stay encrypted on-device by design; the UI quietly reflects that posture instead of advertising it.
- **The medium is the message.** Monospace, square corners, and flat surfaces are not decoration; they declare that this is a precise, no-nonsense tool.

## Accessibility & Inclusion

Target WCAG 2.1 AA. The high-contrast ink-on-paper palette (and its dark inverse) carries most of the burden; never rely on the signal/sap/amber status hues alone, always pair color with a text label (the status pills already do this). Honor the system dark-theme preference. Respect reduced-motion: motion is confined to functional state transitions, never decorative choreography. Keep tap targets at comfortable sizes despite the dense monospace type, and ensure status and progress information is exposed to TalkBack as text, not just color.
