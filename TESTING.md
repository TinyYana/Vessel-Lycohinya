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

An isolated Paper 26.2 test server (build 26.2-87, the same jar already validated in Lycohinya's own
`s01-testbed`) was started with Vessel's built jar plus GriefPrevention 16.18.1-38-g04e7bc7 (the exact
version resolved by the `com.griefprevention:GriefPrevention:16.18.2-SNAPSHOT` dependency pinned in
`build.gradle`, confirmed via the server's own plugin list) installed alongside it:

- Both plugins loaded and enabled with **zero warnings or errors** in the console log — in
  particular, `GriefPreventionProtectionAdapter` registered successfully (no "adapter failed to
  load" warning), confirming `ProtectionService.create()` correctly detects GriefPrevention and the
  adapter's constructor (parsing `config.yml`'s `griefprevention.capture-permission`/
  `release-permission` and resolving them against GriefPrevention's actual `ClaimPermission` enum)
  runs without throwing against the real dependency.
- `/vessel give <player> <type> <amount> -s` was exercised via RCON multiple times and correctly
  granted items (server log: `[Vessel] Gave <player> 1 consumable vessel(s) silently.`).
- GriefPrevention's own claim-creation flow (`/adjustclaimblocks`, golden-shovel corner-claiming,
  `/claimlist`) was exercised via a real mineflayer bot player and functioned normally, confirming
  the test environment's GriefPrevention install itself is healthy.

### What was attempted but blocked by tooling, not by Vessel

A full automated capture/release test matrix (wilderness / owner's claim / outsider-denied, driven by
two mineflayer bots right-clicking real mobs) was attempted. Minecraft 26.2 isn't supported by
mineflayer yet (tracked upstream as issue #3893 — see `docs/MINEFLAYER_TESTING.md` in the Lycohinya
repo for the full writeup), so the test server also ran ViaVersion+ViaBackwards to let a 1.21.4
mineflayer bot connect through protocol translation, following that doc's documented workaround.

Through that setup, **entities created via RCON's `/summon` never reached the bot** (no
`entitySpawn` event, never appeared in `bot.entities`), while naturally-spawned passive mobs (from
the server's normal mob-spawning cycle) reached the bot fine and fired `entitySpawn` normally. This
was isolated with a minimal diagnostic bot script (summon-only, no Vessel involved, with and without
NBT, with and without a prior teleport) — confirming it's specific to how `/summon`-created entities
propagate through this RCON+ViaBackwards combination, not a Vessel defect, a general connectivity
problem, or something specific to teleporting. Given that, a bot can't be made to interact with a
mob it never learns exists, so the automated matrix below could not be completed in this session.

**This is an environment/tooling gap, not a code gap** — the actual capture/release logic exercised
above (loading, config parsing, permission-string resolution against the real GriefPrevention enum)
all passed against the real dependency; what's unverified is specifically the live player-interaction
path (`PlayerInteractEntityEvent`/`PlayerInteractEvent` → `EntitySnapshotAdapter` →
`VesselPayloadStore` → GriefPrevention's `Claim#checkPermission` → entity spawn), end to end, driven
by an actual client action.

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

Repeat the wilderness capture/release and the owner's-claim row on a Folia (or Lophinya) server with
multiple loaded regions. Watch console for any `IllegalStateException` containing "failed main
thread check" — none should appear. Specifically try releasing right at the boundary between two
regions (walk near the edge of loaded/active terrain) to exercise the
`Bukkit.isOwnedByCurrentRegion(...)` guard in `ReleaseListener`.
