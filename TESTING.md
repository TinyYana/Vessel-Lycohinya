# Testing

## What's automated

`./gradlew test` runs 21 JUnit tests covering the parts of the versioned-storage/migration/
GriefPrevention-mapping logic that don't require a live Bukkit server:

- `ChecksumUtilTest` — CRC32 determinism and corruption detection.
- `PayloadMigrationsTest` — migration chain pass-through at current schema, rejection of an
  unregistered source schema version.
- `VesselPayloadStoreEnvelopeTest` — envelope decode round-trip, checksum mismatch, unknown future
  schema, missing/empty required fields, idempotency.
- `EntitySnapshotAdapterSizeTest` — the PDC-string-size guard (60,000-byte safe threshold, under the
  65,535-byte vanilla NBT string cap) at, under, and over the boundary.
- `GriefPreventionProtectionAdapterTest` — config-string → `ClaimAction` parsing (case-insensitivity,
  fallback-with-warning on invalid input) and the `ClaimAction` → GriefPrevention `ClaimPermission`
  mapping, including the version-drift trap (`CONTAINER` → `Inventory` on the pinned 16.18.2 jar).

What's deliberately **not** unit tested: anything that has to call into Paper's actual
`EntityFactory`/`UnsafeValues` (parsing a real `EntitySnapshot`, reading the live Minecraft
DataVersion) or into a live GriefPrevention `DataStore`/`Claim`. Those need a running Paper server —
see below.

## What was verified live (this session)

### Paper 26.2

An isolated Paper 26.2 test server (build 26.2-87, the same jar already validated in Lycohinya's own
`s01-testbed`) was started with Vessel's built jar plus GriefPrevention 16.18.1-38-g04e7bc7 (the exact
version resolved by the `com.griefprevention:GriefPrevention:16.18.2-SNAPSHOT` dependency pinned in
`build.gradle`) installed alongside it:

- Both plugins loaded and enabled with **zero warnings or errors** in the console log — in
  particular, `GriefPreventionProtectionAdapter` registered successfully (no "adapter failed to
  load" warning), confirming `ProtectionService.create()` correctly detects GriefPrevention and the
  adapter's constructor (parsing `config.yml`'s `griefprevention.capture-permission`/
  `release-permission` and resolving them against GriefPrevention's actual `ClaimPermission` enum)
  runs without throwing against the real dependency.
- `/vessel give <player> <type> <amount> -s` was exercised via RCON multiple times and correctly
  granted items (server log: `[Vessel] Gave <player> 1 consumable vessel(s) silently.`).
- GriefPrevention's own claim-creation flow (`/adjustclaimblocks`, golden-shovel corner-claiming,
  `/claimlist`) was exercised via a real mineflayer bot player and functioned normally.
- **A real client-driven capture and a real client-driven denied capture both completed
  end to end**, confirmed via the reliable signal (entity present/absent — see the note on
  `bot.heldItem` below, which is not reliable): a player in the open right-clicked a live cow with a
  consumable vessel and the cow was removed (capture succeeded); a second, untrusted player then
  right-clicked a cow standing inside the first player's GriefPrevention claim and the cow was
  **not** removed (GriefPrevention correctly denied the capture via `Claim#checkPermission`, exactly
  as `GriefPreventionProtectionAdapter` is designed to consume it). This exercises the full path —
  `PlayerInteractEntityEvent` → `EntitySnapshotAdapter` → `VesselPayloadStore` →
  GriefPrevention's `Claim#checkPermission` → entity remove/keep — driven by an actual client action,
  not a synthetic call.
- Caveat: `bot.heldItem`/`bot.inventory` in mineflayer did **not** reliably reflect the server-side
  item change immediately after these interactions in this session (it kept reporting the pre-capture
  material even when the entity-removal signal proved the capture had gone through) — this looks like
  a mineflayer-side inventory-cache staleness issue at this MC 26.2-via-ViaBackwards protocol
  distance, not a Vessel bug. Treat entity presence/absence, not `bot.heldItem`, as the reliable
  signal in any future bot-driven test of this plugin.

### Folia / Lophinya 26.2

A second isolated server was built from Lophinya's own bench runtime (`Lophinya_Dev/bench/lophinya`,
a Luminol-based Folia fork, MC 26.2 protocol 776, `io.papermc.paper.threadedregions` classes
confirmed present) with the same Vessel jar and GriefPrevention 16.18.2 installed:

- The server **starts successfully** and Vessel **loads and enables with zero errors** on it.
- **GriefPrevention 16.18.2 does not work on this Folia fork at all.** By default it's rejected
  outright at load time ("not marked as supporting Folia"). Disabling that check (Luminol's own
  `luminol_global_config.toml` → `[unsupported.disable_check_for_folia_supported]` →
  `disable_for_paper = true`, which exists specifically for this scenario) lets it load, but it then
  **crashes during `onEnable()`**:
  ```
  java.lang.UnsupportedOperationException: sync Bukkit scheduler task from GriefPrevention
  (CraftScheduler.scheduleSyncRepeatingTask, delay=12000, period=12000) is not supported under
  regionised threading
  ```
  GriefPrevention calls the legacy non-region-aware `Bukkit.getScheduler().scheduleSyncRepeatingTask`
  internally, which Folia's threading model rejects outright — this is a real limitation of
  GriefPrevention 16.18.2 itself, not something Vessel's adapter can work around.
- **Vessel handles that correctly**: with GriefPrevention crashed and self-disabled, Vessel still
  enables with zero warnings/errors, exactly as it does with GriefPrevention absent entirely — the
  `isPluginEnabled("GriefPrevention")` check in `ProtectionService.create()` correctly treats
  "installed but failed to enable" the same as "not installed," satisfying the soft-dependency
  requirement even under this real, adversarial failure mode.
- No `IllegalStateException`/"failed main thread check" (or any other thread-ownership violation)
  appeared anywhere in the console log across all of this session's activity on the Folia server
  (plugin enable, player join/leave, `/vessel give`, GriefPrevention's crash and disable).
- **Not completed**: a full client-driven capture/release cycle specifically on this Folia server.
  A mineflayer bot could connect, spawn, and receive items (`/vessel give` confirmed via server log
  for three separate test accounts), but the bot connection proved unstable on this particular
  experimental fork — it disconnected on its own within seconds to tens of seconds of normal play
  (walking, looking around) in every attempt, with and without RCON teleporting involved, independent
  of Vessel. This reproduced consistently across many attempts with different usernames and command
  orderings, ruling out anything specific to a single run. Given mineflayer already isn't officially
  MC 26.2-compatible and is being routed through ViaVersion/ViaBackwards protocol translation to reach
  even the *Paper* server, adding an experimental/dev-build Folia fork on top compounds into
  instability beyond what this session could resolve. This is a tooling gap, not a code gap — the
  code-level Folia audit (`Bukkit.isOwnedByCurrentRegion` re-validation, per-player `EntityScheduler`
  dispatch — see README's "Folia support" section) and the clean load/enable result stand on their
  own; what's specifically unverified is a live capture/release interaction on Folia.

## Manual test procedure (needs a real Minecraft client)

Run on a Paper 26.2 server with GriefPrevention installed. Two accounts needed (owner + outsider).

### Setup

1. `/vessel give <you> consumable 3 -s` and `/vessel give <you> reusable 2 -s`.
2. Stand in the open (no claim), right-click a passive mob with a consumable vessel → confirm it
   disappears and the item becomes a filled vessel (material changes per `vessels/consumable.yml`).
3. Right-click a block/air with the filled vessel → confirm the mob respawns at that point, alive,
   with the same name/equipment/variant it had before, and the item is consumed (consumable) or
   reverts to empty (reusable, if `return-empty-vessel: true`).

### GriefPrevention matrix

For each row: capture a mob while standing in that claim context, then release inside the same
context. Expected column is what should happen for the *default* config
(`capture-permission: CONTAINER`, `release-permission: BUILD`).

| Context | Capture (needs CONTAINER) | Release (needs BUILD) |
|---|---|---|
| Wilderness / GP-disabled world | Allowed | Allowed |
| Your own claim | Allowed (owner has everything) | Allowed |
| Claim where you have `/containertrust` only | Allowed | **Denied**, GP's reason shown |
| Claim where you have `/trust` (build) | Allowed | Allowed |
| Claim where you have `/accesstrust` only | **Denied**, GP's reason shown | **Denied**, GP's reason shown |
| Claim with no trust at all (outsider) | **Denied**, GP's reason shown | **Denied**, GP's reason shown |
| Subdivision (no explicit trust; parent has your trust) | Follows parent's trust (per GriefPrevention's own inheritance — confirm it matches the parent-claim row) | same |
| Admin Claim, you hold `griefprevention.adminclaims` | Allowed | Allowed |
| Admin Claim, you don't hold that permission | **Denied** | **Denied** |
| `/ignoreclaims` toggled on (admin) | Allowed everywhere | Allowed everywhere |

For each "Denied" case, confirm: the mob is **not** removed / **not** spawned, the vessel item is
**not** consumed, and a `<gray>` follow-up line with GriefPrevention's specific denial reason appears
under Vessel's own "you cannot capture/release here" message.

### Mob data fidelity (release should reproduce all of this)

Capture, then release, and confirm each survives the round trip:

- Villager: profession, trades, level.
- Tamed wolf/cat/horse: owner, tamed status, sitting state.
- Horse/donkey/llama: inventory contents, equipped saddle/armor, variant/color.
- Any mob: custom name, active potion effects, health (if damaged before capture).
- Restart the server between capture and release — confirm the vessel item (and its data) survived
  a full plugin reload.

### Folia / Lophinya

⚠ **GriefPrevention 16.18.2 does not run on Folia** (confirmed this session — see "What was verified
live" above): it crashes on `onEnable()` with `UnsupportedOperationException: sync Bukkit scheduler
task ... is not supported under regionised threading`, even with the fork's Folia-support check
disabled. On a real Folia/Lophinya deployment, GriefPrevention will not be available, and Vessel's
GriefPrevention integration simply never activates (verified: Vessel treats this the same as
GriefPrevention being absent, with a clean enable and no errors). Test the wilderness rows only; the
claim-permission matrix isn't reachable until GriefPrevention itself ships a Folia-compatible build.

Repeat the wilderness capture/release on a Folia (or Lophinya) server with multiple loaded regions.
Watch console for any `IllegalStateException` containing "failed main thread check" — none should
appear. Specifically try releasing right at the boundary between two regions (walk near the edge of
loaded/active terrain) to exercise the `Bukkit.isOwnedByCurrentRegion(...)` guard in
`ReleaseListener`.
