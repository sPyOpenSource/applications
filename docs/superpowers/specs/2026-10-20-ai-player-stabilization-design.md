# AI Player Stabilization — Design Spec

**Project:** JavaRA (C&C: Red Alert 1 clone in Java)  
**Scope:** `cr0s.javara.ai` package — `AIPlayer`, `BaseBuilder`, `Squad`, `StateMachine`, and `ai/*.yaml` rule files  
**Date:** 2026-10-20  
**Status:** Draft

## Goal

Stabilize and wire up the JavaRA AI player without adding new subsystems or changing its overall architecture. Fix clear bugs and make already-implemented but unused features functional.

## Scope (in)

The following items are in scope:

1. **Clear bug fixes** in `AIPlayer.java`:
   - `thereIsNoMcv()` currently always returns `true`, causing the AI to attempt to rebuild an MCV forever even when one exists.
   - `findEnemyConstructionYards()` condition is inverted — it skips enemy-owned yards and returns friendly ones.
   - `findClosestEnemy(Pos)` calls `enemies.get(0)` unconditionally and crashes when no enemies exist.
   - Unit common names are written into `buildingCommonNames` instead of `unitCommonNames`.
   - Typos: `buidUnits` → `buildUnits`, `shoudlRepairBuildings` → `shouldRepairBuildings`.
   - `unitsToBuild` YAML weights are loaded but never used; units are picked uniformly at random instead of weighted.

2. **Wire up unused pieces:**
   - The `Squad` class has a `StateMachine` field (`fsm`) but no states are ever assigned, so squads never act on their targets.
   - `StateMachine.IState` implementations exist only as an interface; add concrete `IdleState` and `AssaultState`.

The following are **out of scope**: adding difficulty tiers, adding air/naval unit logic beyond what YAML already declares, changing the build-order algorithm, adding scouting/exploration, or adding new unit/building types.

## Non-goals

- No new subsystems, modules, or packages.
- No changes to `World`, `Base`, `ProductionQueue`, combat, pathfinding, or rendering.
- No UI changes.
- No AI difficulty settings or personalities beyond what the YAML rule files already encode.

## Architecture

Keep the existing tick-driven controller:

```
AIPlayer.update()   (every game tick)
  ├── findAndDeployAllMcv()          (tick 1 only)
  ├── productUnits()                  (every 30 ticks)
  │     └── buildUnits()              → weighted random unit from unitsToBuild
  ├── BaseBuilder.update()            → place buildings by YAML fractions
  └── assignRolesToIdleUnits()
        ├── giveOrdersToIdleHarvesters()
        ├── findNewUnits()            → harvesters harvest; others join idle pool
        ├── createAttackForce()       → form ASSAULT squads from idle units
        └── findAndDeployAllMcv()
              └── squads[i].update()  → StateMachine.fsm.update(squad)
```

Each responsibility has one home; no logic moves between classes.

## Components

### AIPlayer (`cr0s.javara.ai.AIPlayer`)
- Fix `thereIsNoMcv()` to scan own entities for a live `EntityMcv` and return `false` if one exists.
- Fix `findEnemyConstructionYards()` to return *enemy-owned* construction yards only.
- Guard `findClosestEnemy(Pos)` against an empty enemy list (return empty list instead of crashing).
- Write unit common names to `unitCommonNames`, not `buildingCommonNames`.
- Fix `buildUnits()` to pick a unit weighted by the loaded `unitsToBuild` percentages instead of uniform random.
- Rename `buidUnits` → `buildUnits`; `shoudlRepairBuildings` → `shouldRepairBuildings`.

### Squad (`cr0s.javara.ai.Squad`)
- Keep existing fields/methods unchanged.
- `setTarget()` transitions the squad's `StateMachine` into `AssaultState`.
- `update()` already calls `fsm.update(this)`; no change needed beyond wiring states.

### StateMachine (`cr0s.javara.ai.StateMachine`)
- Add concrete `IState` implementations:
  - `IdleState` — tick does nothing; squad holds position.
  - `AssaultState` — each tick, move every unit in the squad toward the squad target.
- `activate`/`deactivate` are no-ops for these states.

### BaseBuilder (`cr0s.javara.ai.BaseBuilder`)
- No changes. Building placement and build-order already work via YAML fractions.

## Data flow

1. Game tick → `AIPlayer.update()`.
2. On tick 1: deploy all MCVs near base or move to a build location.
3. Every 30 ticks: `productUnits()` checks economy (`hasAdequateProc()`), rebuilds MCV if none exists (`thereIsNoMcv()`), then `buildUnits()` picks a unit weighted by `unitsToBuild` YAML and starts production.
4. Every tick: `BaseBuilder.update()` places buildings by economy priority (power → refinery → production → silo → other).
5. Idle units are assigned roles: harvesters get harvest orders; others join `unitsHangingAroundTheBase`. When enough idle units exist, an ASSAULT squad is formed and given an enemy target via `chooseEnemyTarget()`.
6. Each squad `update()` drives its `StateMachine`; `AssaultState` moves units toward the target each tick.

## Error handling

- Empty enemy list → `chooseEnemyTarget()` / `findClosestEnemy()` return empty list; rush/attack skips gracefully.
- No MCV found → `thereIsNoMcv()` returns `true`; MCV build proceeds once.
- Null buildables / production queue busy → `buildUnits()` returns without starting production.
- Logging follows the existing pattern (`System.out` / `System.err` with `[AI]` prefix). No new exception types.

## Testing

Add unit tests under `test/test/`:

- `AIPlayerTest.thereIsNoMcv` — returns `false` when a live owned MCV exists, `true` otherwise.
- `AIPlayerTest.buildUnitsWeightedSelection` — over many rolls, unit distribution roughly matches `unitsToBuild` YAML percentages.
- `AIPlayerTest.findEnemyConstructionYards` — returns only enemy-owned construction yards, excludes friendly yards.

Tests use existing test conventions in `test/test/` (JUnit-style plain Java tests; see `PosTest`, `AStarPathFinderTest`).

## Acceptance criteria

- AI no longer attempts to rebuild an MCV indefinitely.
- Enemy construction yards are correctly identified as enemy targets.
- No crash when no enemies are present.
- Unit production reflects YAML `unitsToBuild` weights (not purely uniform).
- ASSAULT squads move toward their assigned target each tick (FSM wired).
- Existing game loop remains runnable; no regressions in building placement.
