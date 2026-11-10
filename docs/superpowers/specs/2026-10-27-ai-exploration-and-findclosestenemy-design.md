# AI Exploration & findClosestEnemy Fix — Design Spec

**Project:** JavaRA (C&C: Red Alert 1 clone in Java)  
**Scope:** `cr0s.javara.ai` package — `AIPlayer`, `Squad`, `ai/states`, plus `cr0s.javara.gameplay.Player`  
**Date:** 2026-10-27  
**Status:** Draft

## Goal

Fix the `findClosestEnemy` bug (AI only considers one enemy player, missing the real nearest enemy; crashes when no enemies exist) and add a dedicated AI explorer squad that roams unexplored map areas so the AI actively scouts instead of only reacting to nearby enemies.

## Scope (in)

The following items are in scope:

1. **`findClosestEnemy(Pos)` fix in `AIPlayer.java`** — loop over *all* enemy players, merge results from `findClosestEnemy(center, p, radius)`, sort by distance to `center`, return closest-first. Return empty list when no enemies exist.
2. **Null-safe call sites** — `GroundStateBase.java` lines 27 and 62 call `.get(0)` on the possibly-empty result; add empty-list early returns.
3. **New `SquadType.EXPLORATION`** — an exploration squad type alongside ASSAULT, PROTECTION, etc.
4. **New `ExplorationState implements IState`** in `cr0s.javara.ai.states` — each tick, if the squad has no valid target, choose a destination cell (unexplored shroud cell if available, else a random far-from-base passable cell) and issue `Order("Move", ...)` to idle squad units.
5. **Explorer squad creation in `AIPlayer`** — one small exploration squad (~2–4 combat units) registered when idle units are available; maintained like other squads.

## Non-goals

- No shroud wiring: `playerShroud` is currently commented out in `Player`. The explorer must degrade gracefully (random cell) when no shroud is present; it must not require or create shroud state.
- No combat changes: exploration is secondary to assault/protection squads; enemies found by the explorer are handled by existing targeting logic.
- No new packages beyond `cr0s.javara.ai.states`.
- No UI, pathfinding, World, or Base changes.
- No difficulty tiers or AI personalities.

## Architecture

Keep the existing tick-driven controller, extending `Squad.update()`'s state-machine path:

```
AIPlayer.update()   (every game tick)
  ...
  └── assignRolesToIdleUnits()
        ├── createAttackForce()     → ASSAULT squads (existing)
        └── createExplorationSquad() → EXPLORATION squad (new)
              └── squads[i].update() → StateMachine.fsm.update(squad)
                    └── ExplorationState.tick(squad)  → pick cell, Order("Move")
```

Each responsibility has one home; no logic moves between classes.

## Components

### AIPlayer (`cr0s.javara.ai.AIPlayer`)
- **Fix `findClosestEnemy(Pos center)`:** iterate all enemy players returned by `world.getPlayers()`; for each, call `findClosestEnemy(center, p, radius)`; collect all results into one list; sort by `distanceToSq(center)` ascending; return closest-first (empty list if no enemies or no results).
- **Add `createExplorationSquad()`:** called from `assignRolesToIdleUnits()`; when idle units exist and no exploration squad is registered, register a small EXPLORATION squad (e.g., 2–4 `ICanAttack` units from `unitsHangingAroundTheBase`).
- Exploration squad maintenance is identical to other squads (registered once, cleaned via `cleanSquads()`, updated via `squad.update()`).

### Squad (`cr0s.javara.ai.Squad`)
- Add `SquadType.EXPLORATION` to the enum.
- No other changes: `setTarget()` / `update()` already route through `StateMachine`.

### `cr0s.javara.ai.states.ExplorationState` (new)
Implements `IState`:
- `activate(Squad)` — no-op.
- `tick(Squad)`:
  - If squad invalid or has no units → return.
  - If squad already has a valid target → return (combat targeting owns it).
  - Pick a destination cell:
    1. If AI player has a non-null shroud: gather unexplored cells (passable, within map); prefer cells far from base; pick one at random.
    2. Else: pick a random passable cell far from base.
  - If a destination is found: for each idle squad unit, issue `Order("Move", unit, destinationCell)`.
  - If the squad reached its last destination (target reached / no valid target): clear target; next tick picks a new cell.
- `deactivate(Squad)` — no-op.

### `cr0s.javara.ai.states.GroundStateBase` (existing)
- Guard both `.get(0)` call sites (`GroundUnitsIdleState.tick` line 27, `GroundUnitsAttackMoveState.tick` line 62): if `findClosestEnemy(...)` returns empty, `return` early instead of indexing.

### `cr0s.javara.gameplay.Player`
- No changes. `findClosestEnemy(center, p, radius)` keeps its existing behavior; `playerShroud` stays commented out (explorer handles null shroud gracefully).

## Data flow

1. Game tick → `AIPlayer.update()` → `assignRolesToIdleUnits()`.
2. `createExplorationSquad()` registers an EXPLORATION squad from idle combat units when none exists.
3. Each squad tick → `Squad.update()` → `StateMachine.update(squad)` → `ExplorationState.tick(squad)`.
4. `ExplorationState` picks an unexplored (or random far) cell → issues `Order("Move", ...)` to each idle squad unit.
5. Units move via the existing move/attack order system; when the target is reached, the squad has no target and picks a new cell next tick.

## Error handling

- No enemies → `findClosestEnemy` returns empty list; rush/attack skip gracefully; explorer continues (no enemy target needed).
- Empty squad result at call sites → `GroundStateBase` returns early; no `IndexOutOfBoundsException`.
- Shroud `null` → explorer falls back to random far-from-base cell; no NPE.
- No passable/unexplored cell found → explorer issues no order; retries next tick.
- Unreachable destination → pathfinder handles it; explorer picks a new cell next tick.

## Testing

Add unit tests under `test/test/`:

- `AIPlayerTest.findClosestEnemyConsidersAllEnemies` — with two enemy players at different distances, the nearest enemy overall is returned first.
- `AIPlayerTest.findClosestEnemyNoEnemies` — returns empty list, no crash.
- `GroundStateBaseTest.idleWithNoEnemy` — does not throw when `findClosestEnemy` returns empty.
- `ExplorationStateTest.picksUnexploredCell` — with a shroud, destination is an unexplored cell; without a shroud, no exception and a move order is still issued.

Tests use existing test conventions in `test/test/` (JUnit-style plain Java tests; see `PosTest`, `AStarPathFinderTest`).

## Acceptance criteria

- `findClosestEnemy` returns the true nearest enemy across all enemy players, sorted closest-first.
- No crash when no enemies exist (`IndexOutOfBoundsException` eliminated at both `GroundStateBase` call sites).
- AI fields a dedicated exploration squad that moves to unexplored/far cells instead of idling at base.
- Explorer degrades gracefully with no shroud present.
- Existing assault/protection behavior unchanged; no regressions in the game loop.
