# AI Player Bug Fixes — Design

Date: 2026-10-21
Project: JavaRA (cr0s.javara.ai)
Scope: Approach A — targeted surgical fixes + small cleanups. No new features, no rearchitecture.

## Goal

Eliminate defects in `AIPlayer` and its helpers that cause crashes (NPE/IndexOutOfBounds) or obviously wrong behavior, while preserving the existing overall AI strategy. Keep changes minimal and localized so behavior is otherwise unchanged.

## Files in scope

- `JavaRA/src/cr0s/javara/ai/AIPlayer.java`
- `JavaRA/src/cr0s/javara/ai/Squad.java` (minor: `isValid` already filters dead units via caller; no change beyond consistency)
- `JavaRA/src/cr0s/javara/ai/BaseBuilder.java` (one empty branch)

## Defects and fixes

### 1. NPE when no enemies (`findClosestEnemy(Pos)`)
Location: `AIPlayer.findClosestEnemy(final Pos center)`.
Problem: `enemies.get(0)` is called without checking emptiness; throws `IndexOutOfBoundsException` (and NPE downstream) when there are no enemy players.
Fix: if `enemies` is empty, return an empty `ArrayList<EntityActor>`.

### 2. NPE when no enemies (`chooseEnemyTarget`)
Location: `AIPlayer.chooseEnemyTarget()`.
Problem: `enemies.get(0)` without empty check; also `findClosestEnemy(..., enemy, ...)` result used without emptiness guard in callers.
Fix: if `enemies` is empty, return an empty list. Callers already guard with `isEmpty()`.

### 3. Crash in defensive build placement (`chooseBuildLocation`, DEFENSIVE)
Location: `AIPlayer.chooseBuildLocation(..., BuildingType.DEFENSIVE)`.
Problem: `this.findClosestEnemy(this.defenseCenter)` (single-arg) may return empty; `.get(0)` crashes.
Fix: store result; if empty use `this.getPlayerSpawnPoint()` as target cell (existing fallback), else use first enemy's cell position.

### 4. Wrong set returned (`findEnemyConstructionYards`)
Location: `AIPlayer.findEnemyConstructionYards()`.
Problem: continue condition skips enemy-owned construction yards, so it returns allied/own yards instead of enemy ones.
Fix: invert the ownership test so enemy-owned yards are collected (`!e.owner.isEnemyFor(this)` → skip).

### 5. Enemy units hijacked (`findNewUnits`)
Location: `AIPlayer.findNewUnits()`.
Problem: iterates all `MobileEntity` in `entities` without an ownership check, so enemy units get absorbed into this AI's `activeUnits` and receive orders.
Fix: require `a.owner == this` before considering a unit new.

### 6. Empty assault squads registered (`createAttackForce`)
Location: `AIPlayer.createAttackForce()`.
Problem: registers a squad even when no hanging unit is an `ICanAttack`, leaving empty squads in the list.
Fix: collect attack units first; only register/add/clear when at least one attack unit exists.

### 7. Crash on empty squad list (`tryToRushAttack`)
Location: `AIPlayer.tryToRushAttack()`.
Problem: `squads.get(0)` without emptiness check.
Fix: if `squads` is empty, return early (already returns if empty attack target). Also prefer an ASSAULT squad if present; fall back to first squad.

### 8. Unit common names never loaded (`loadAIRules`)
Location: `AIPlayer.loadAIRules(String)`, "UnitsCommonNames" loop.
Problem: writes into `this.buildingCommonNames` instead of `this.unitCommonNames`, clobbering building names and leaving unit names empty.
Fix: assign to `this.unitCommonNames`.

### 9. MCV repeatedly rebuilt (`thereIsNoMcv`)
Location: `AIPlayer.thereIsNoMcv()`.
Problem: body commented out, always returns `true`, so `productUnits` keeps building MCVs.
Fix: restore the loop checking for an owned, non-dead `EntityMcv`; return `false` if found.

### 10. Neutral vehicles never added (`buidUnits`, Vehicles case)
Location: `AIPlayer.buidUnits(boolean)`, case 1.
Problem: the `neutralVehicles` branch has an empty body, so neutral vehicles are never added to `buildables`.
Fix: add `buildables.add(name)` in that branch.

### 11. Empty-state robustness (`Squad.isValid`)
Location: `Squad.isValid()`.
Problem: only checks `units` non-empty; callers expect dead-unit handling. `removeDeadAndNotOwnUnits` already filters, but `isValid` should also treat an all-dead squad as invalid to avoid operating on dead squads between cleanups.
Fix: return `!units.isEmpty() && units.stream().anyMatch(u -> !u.isDead())`.

## Out of scope (Approach A)

- Wiring `StateMachine` state transitions (dormant).
- Air/naval production logic beyond the empty-branch fix.
- `Formation`/`maintainCohesion`/`adoptFormation` (stubs).
- Aggro ordering semantics in `chooseEnemyTarget`.
- Performance/profiling.

## Testing

No automated tests cover AI. Verification is compile-only plus manual smoke check that the game still launches and AI acts (builds base, harvests, attacks) without exceptions in the console. Compile the JavaRA module with the existing Ant/NetBeans setup (Java 21, JavaFX 25).

## Risks

- Restoring `thereIsNoMcv` logic changes MCV production frequency; intended correction.
- Inverting `findEnemyConstructionYards` ownership test changes which yards are considered; only used internally (currently unused caller), low risk.
- Ownership filter in `findNewUnits` prevents enemy-unit absorption; purely a correctness fix.
}