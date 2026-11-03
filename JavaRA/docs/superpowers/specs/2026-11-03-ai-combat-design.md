# AI Combat: Retreat/Regroup & Defense Placement — Design

Date: 2026-11-03
Project: JavaRA (`cr0s.javara.ai`)
Status: Proposed
Scope: One combat spec covering two related AI improvements:

1. Survivable attacks — squads retreat when losing, recover at base, absorb reinforcements, and re-attack.
2. Defense placement — turrets cover chokepoints and threat approaches with overlapping fields of fire instead of scattering or clustering.

## Problem

### Attacking

The attack machinery works, but its decisions are poor:

- `Squad.update()` ignores its `StateMachine`, so `ExplorationState` is assigned but never ticks and the planned `GroundUnitsFleeState` does not exist.
- `tryToRushAttack()` selects `squads.get(0)`, which is usually the EXPLORATION squad, while the ASSAULT squad sits idle.
- `Squad` never removes dead or foreign units during updates; `AIPlayer.cleanSquads()` is commented out, so strength is measured over stale rosters.
- `createAttackForce()` adds only `ICanAttack` units but clears the entire idle pool, silently dropping non-attack units. `findNewUnits()` does not check ownership and can absorb enemy units.
- A squad cannot disengage. Once committed, units fight until wiped out.
- There is no unit healing or repair; only buildings repair.
- YAML `SquadSize` is ignored (hardcoded 8). The existing profile values (Normal 40, Rush 20) also conflict with `idleBaseUnitsMaximum = 12`, which stops production before a squad can form.

### Defense placement

- `AIPlayer.chooseBuildLocation(DEFENSIVE)` builds 70% of the time at the free cell closest to the nearest enemy, producing isolated forward turrets; the other 30% is random anywhere in the base.
- There is no concept of coverage, overlap, chokepoints, spacing, or weapon range. Each turret is scored only by distance to an enemy or by randomness.
- `BaseBuilder` chooses which defense to produce randomly from `BuildingFractions`, with no role for range or threat.
- `Defensive.getRange()` returns 0 because `EntityFireTurret` and `EntityTeslaCoil` pass `0` to `super(...)`, so range data is unavailable to the AI.
- `defenseCenter` only reacts after a building is damaged; before that, placement has no threat orientation.

## Goals

- A squad that is losing disengages, returns to base, recovers, absorbs reinforcements, and re-attacks.
- Retreat is driven by casualties and damage, not a full enemy-threat simulation.
- Defense turrets are placed to cover chokepoints and the threat side, with overlapping fields of fire and without clustering or blocking corridors.
- Tuning lives in the per-profile AI YAML files.
- Behavior is testable without launching the game.

## Non-goals

- Focus fire, target prioritization, unit screening, formations, kiting, or wave timing.
- Adding new defensive building types or anti-air coverage.
- Changing `BaseBuilder` production priority or defense composition (placement only).
- Healing or repairing player-controlled units.
- Replacing the existing pathfinder or building a full threat map.
- Changes to the damage/armor system.

## Part 1 — Attack, retreat, regroup, re-attack

### State machine

Use the existing `StateMachine` and `ai.states` package. New states:

- `GroundCombatState`
- `GroundRetreatState`
- `GroundRecoverState`

`Squad.update()` changes to:

1. Return if `units` is empty.
2. Call `removeDeadAndNotOwnUnits()`.
3. If `fsm.getCurrentState() != null`, call `fsm.update(this)` and return.
4. Otherwise run the existing legacy engagement logic.

`PROTECTION` squads keep the legacy path (no FSM state). `EXPLORATION` squads now actually execute `ExplorationState`; this is an intentional correction.

#### `GroundCombatState`

- If `squad.shouldRetreat()`, transition to `GroundRetreatState` and return.
- Otherwise behave like the current `Squad.update()`:
  - If the current target is invalid, acquire the closest enemy to the squad's first living unit.
  - For each idle living member: issue `Attack` when an armament is valid for the target, otherwise `Move` toward the target cell.

#### `GroundRetreatState`

- On activate: clear the target and issue a `Move` order to a rally cell near the AI spawn. Rally cells are jittered per unit (up to 3 cells) and clamped to passable terrain; if no spawn exists, the state stays active without issuing orders.
- On tick:
  - Re-issue `Move` to idle members that are not yet within the base radius.
  - Transition to `GroundRecoverState` once every living unit is within the base radius.
- On deactivate: no special cleanup.

#### `GroundRecoverState`

- On activate: ask the bot to reinforce the squad.
- On tick:
  - Ask the bot to reinforce again (new units may have finished production).
  - If `healthRatio >= ReengageHealthPercent / 100` and living size `>= MinAttackSquadSize`, call `squad.markRegrouped()` and transition to `GroundCombatState`.
  - If living size `< MinAttackSquadSize` and the state has been active for `RecoverTimeoutTicks`, disband the squad: return survivors to the idle pool and remove the squad from the bot's list.
- Reinforcements are taken only from `unitsHangingAroundTheBase` and only if they implement `ICanAttack`.

### Squad changes

- Track `initialStrength` (the largest living count observed, set when units are added) and `desiredSize` (set from the bot's `SquadSize`, or the current size if larger).
- `healthRatio()` = sum of `hp` / sum of `maxHp` over living units; use `Math.max(1, maxHp)` per unit.
- `casualtyRatio()` = `1 - livingCount / initialStrength`, clamped to `[0, 1]`.
- `shouldRetreat()` = `healthRatio() <= RetreatHealthPercent / 100` OR `casualtyRatio() >= RetreatCasualtyPercent / 100`, unless the squad is already retreating or recovering.
- `markRegrouped()` resets `initialStrength` to the current living count so old casualties do not immediately trigger another retreat.
- `removeDeadAndNotOwnUnits()` runs every update.
- `clearTarget()` sets the target to an invalid/empty `Target`; `setTarget()` keeps its current order-issuing behavior for legacy PROTECTION squads.

### AIPlayer changes

- Load `SquadSize`, `CombatTuning`, and `Defense` from YAML. Missing values keep code defaults.
- Effective idle cap: `idleBaseUnitsMaximum = max(currentValue, SquadSize + 4)` so a squad can actually mass.
- `cleanSquads()` is called from `assignRolesToIdleUnits()`.
- `createAttackForce()`:
  - Collect `ICanAttack` units from `unitsHangingAroundTheBase`.
  - Wait until there are at least `SquadSize + random(0..3)` attack-capable idle units, so attacks leave as a mass rather than a trickle. Below that threshold, do nothing.
  - Register an ASSAULT squad, add those units, and remove only those units from the idle pool.
  - Run `changeState(squad, new GroundCombatState(), false)`.
- `tryToRushAttack()`:
  - Choose the first ASSAULT squad whose FSM state is `GroundCombatState`; return if none.
  - Never select EXPLORATION, PROTECTION, retreating, or recovering squads.
  - Set its target to the first result of `chooseEnemyTarget()`.
- `findNewUnits()` requires `a.owner == this`.
- Base regeneration:
  - Every `BaseRegenInterval` ticks, heal each own, mobile, idle, damaged unit inside `BaseRegenRadius` of the spawn by `BaseRegenHp`, capped at `maxHp`.
  - Units in combat, moving, dead, enemy-owned, or outside the radius are not healed.
- `reinforceSquad(Squad)`:
  - Compute missing = `desiredSize - livingCount`.
  - Move up to `missing` idle `ICanAttack` units from `unitsHangingAroundTheBase` into the squad.
  - Do not touch harvesters, MCVs, exploration units, or other squads.

### Attack data flow

```
AIPlayer.update()
 ├─ productUnits()                    (every 30 ticks)
 ├─ BaseBuilder.update()              → DefensePlanner on ready defense
 └─ assignRolesToIdleUnits()          (every frame)
     ├─ squad.update()                (every 30 ticks)
     │   ├─ prune dead/foreign units
     │   └─ FSM: COMBAT → RETREAT → RECOVER → COMBAT
     ├─ base regeneration             (every BaseRegenInterval ticks)
     ├─ reinforce recovering squads
     ├─ createAttackForce()
     ├─ tryToRushAttack()             (every rush interval)
     ├─ createExplorationSquad()
     └─ cleanSquads()
```

## Part 2 — Defense placement

### `DefensePlanner`

New class `cr0s.javara.ai.DefensePlanner`, owned by `AIPlayer`. `AIPlayer.chooseBuildLocation(..., DEFENSIVE)` delegates to it. `BaseBuilder` is unchanged.

#### Approach map (cached)

- Cache a list of weighted approach cells around the base center (`AIPlayer.getPlayerSpawnPoint()`).
- Annulus: `max(1, Defense.MinRadius - 2)` to `Defense.MaxRadius + 8` (8 = longest current defense range).
- Include only terrain-passable cells: in map, surface not water/river/rock.
- Weight each cell:
  - Chokepoint factor: count terrain-passable 8-neighbours. `<= 3` → ×3, `<= 5` → ×2, otherwise ×1.
  - Threat factor: cells on the side of the base facing the threat center get ×1.5; cells within 6 tiles of the threat center get ×2. When the threat center equals the base center, skip the threat factor entirely (symmetric scoring).
- Rebuild the cache when the base center changes or when it is first requested.

Threat center selection in `AIPlayer`:

1. If `defenseCenter` differs from the base center (a building was recently damaged), use `defenseCenter`.
2. Otherwise, use the nearest enemy's cell if it is within `Defense.ThreatRadius` of the base.
3. Otherwise, use the base center (symmetric scoring).

#### Candidate scoring

Candidates come from `world.chooseTilesInAnnulus(baseCenter, Defense.MinRadius, Defense.MaxRadius)` with a chooser that requires:

- `base.isPossibleToBuildHere(cell, defensePrototype)`.
- `base.checkBuildingDistance(cell, false)`.
- At least 5 terrain-passable 8-neighbours, so the defense covers a corridor instead of occupying a narrow one.

For each candidate:

1. **Existing coverage:** for every approach cell, count existing `Defensive` buildings (`IDefense` walls are ignored) whose `getRange()` covers it.
2. **Marginal coverage:** sum approach weights where existing coverage is 0 (full weight) or 1 (half weight). Cells already covered `OverlapTarget` or more times add nothing.
3. **Threat alignment:** coverage of cells on the threat side is already weighted, but add a small additional bonus if the candidate lies between the base and the threat center.
4. **Spacing:** reject candidates within `Defense.MinSpacing` of an existing defense; penalize candidates within `MinSpacing + 2`; add a mild bonus for 4–8 tile spacing.
5. **Type role:**
   - `EntityTeslaCoil` (range 8): ×1.25 on marginal coverage; extra weight for covering chokepoint cells.
   - `EntityFireTurret` (range 5): ×1.15 when its coverage overlaps an existing defense (layering); mild bonus for being closer to the base.
6. **Base distance:** mild bonus for candidates 6–12 tiles from the base center.

Choose the highest score; break ties randomly.

If the defense prototype is missing, has `getRange() == 0`, or no candidate passes the filters, fall back to the existing `findPosForBuilding` behavior. If that returns `null`, `BaseBuilder` cancels the build as it does today.

#### Range fix

`Defensive.getRange()` currently returns `0`. Fix the constructors:

- `EntityFireTurret`: pass range `5` to `super(...)`.
- `EntityTeslaCoil`: pass range `8` to `super(...)`.

The planner treats a non-positive range as unsupported and uses the legacy fallback.

### Defense data flow

```
BaseBuilder.update()
 └─ current building ready
     └─ AIPlayer.chooseBuildLocation(name, true, DEFENSIVE)
         └─ DefensePlanner.chooseCell(defensePrototype)
             ├─ cached approach map (weights)
             ├─ existing defense coverage
             ├─ candidate filtering + scoring
             └─ best cell, or legacy fallback, or null
```

## Tuning

Defaults in code; YAML overrides per profile.

```yaml
SquadSize: 16
CombatTuning:
  RetreatHealthPercent: 50
  RetreatCasualtyPercent: 40
  ReengageHealthPercent: 80
  MinAttackSquadSize: 4
  RecoverTimeoutTicks: 1800
  BaseRegenInterval: 30
  BaseRegenHp: 10
  BaseRegenRadius: 20
Defense:
  MinRadius: 5
  MaxRadius: 20
  MinSpacing: 3
  OverlapTarget: 2
  ThreatRadius: 35
```

Profile intent:

| Profile | SquadSize | Retreat health | Retreat casualties | Re-engage | Min size | Overlap | Spacing | Max radius |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Rush | 20 | 40 | 50 | 70 | 4 | 1 | 2 | 20 |
| Normal | 16 | 50 | 40 | 80 | 4 | 2 | 3 | 20 |
| Turtle | 10 | 60 | 30 | 90 | 5 | 3 | 3 | 16 |
| Naval | 16 | 50 | 40 | 80 | 4 | 2 | 3 | 20 |

The existing YAML `SquadSize` values are replaced by the table above. `idleBaseUnitsMaximum` becomes `max(12, SquadSize + 4)`.

## Edge cases and error handling

- Null spawn or rally cell: retreat state issues no move; the squad retries next tick.
- No enemies: combat state idles; `tryToRushAttack()` returns.
- All units dead: `Squad.isValid()` is false; `cleanSquads()` removes the squad.
- Casualty baseline reset on regroup prevents immediate re-retreat from old losses.
- Regen never exceeds `maxHp`; `maxHp <= 0` is treated as `1` for ratio math.
- Reinforcement iterates over a copy of the idle pool to avoid concurrent modification.
- Missing YAML sections or keys keep code defaults; test AIs with nonexistent profiles must still construct and update.
- Defense planner returns `null` gracefully; `BaseBuilder` already handles a null location by cancelling.
- `getRange() == 0` or unknown defense type skips scoring and uses legacy placement.
- Approach map depends only on terrain and base center; it is rebuilt only when the base center changes.

## Files

New:

- `JavaRA/src/cr0s/javara/ai/DefensePlanner.java`
- `JavaRA/src/cr0s/javara/ai/states/GroundCombatState.java`
- `JavaRA/src/cr0s/javara/ai/states/GroundRetreatState.java`
- `JavaRA/src/cr0s/javara/ai/states/GroundRecoverState.java`

Modified:

- `JavaRA/src/cr0s/javara/ai/Squad.java`
- `JavaRA/src/cr0s/javara/ai/AIPlayer.java`
- `JavaRA/src/cr0s/javara/ai/states/StateBase.java` (shared helpers, if needed)
- `JavaRA/src/cr0s/javara/entity/building/soviet/EntityFireTurret.java`
- `JavaRA/src/cr0s/javara/entity/building/soviet/EntityTeslaCoil.java`
- `JavaRA/ai/RushAI.yaml`
- `JavaRA/ai/NormalAI.yaml`
- `JavaRA/ai/TurtleAI.yaml`
- `JavaRA/ai/NavalAI.yaml`
- Tests under `JavaRA/test/test/`

## Testing

### Attack

1. `Squad.healthRatio()` and `casualtyRatio()` return expected values for mixed HP and dead units.
2. A squad below the health threshold transitions to `GroundRetreatState` and issues `Move` to the rally.
3. A squad below the casualty threshold retreats; a healthy squad does not.
4. Retreat transitions to recover when all living units are at base.
5. Recover absorbs idle attack units up to `desiredSize` and does not take harvesters or enemy units.
6. Recover returns to combat once health and size thresholds are met.
7. Recover disbands survivors after the timeout when below minimum size.
8. `markRegrouped()` resets the casualty baseline (no immediate re-retreat).
9. Regeneration heals only own, idle, damaged, in-radius mobile units and caps at `maxHp`.
10. `createAttackForce()` registers an ASSAULT squad with `GroundCombatState`, includes only `ICanAttack` units, and leaves non-attack units in the idle pool.
11. `tryToRushAttack()` selects ASSAULT squads only and skips retreating/recovering squads.
12. `findNewUnits()` ignores enemy-owned units.
13. Dead units are pruned from squads and empty squads are removed.

### Defense

1. `Defensive.getRange()` returns 5 for `EntityFireTurret` and 8 for `EntityTeslaCoil`.
2. A chokepoint-adjacent candidate outscores an open-field candidate with otherwise equal conditions.
3. With a threat center east of the base, the chosen first defense is east of the base.
4. A second defense is not placed within `MinSpacing` of the first.
5. `OverlapTarget` affects whether a second-layer candidate is preferred.
6. Tesla Coil and Flame Tower produce different preferred candidates on the same base.
7. A candidate on a narrow corridor (fewer than 5 passable neighbours) is rejected.
8. No valid candidate returns `null`; `chooseBuildLocation(DEFENSIVE)` falls back without throwing.
9. `chooseBuildLocation(DEFENSIVE)` delegates to `DefensePlanner` when a valid prototype exists.

### Regression

- All existing AI tests (`AIPlayerTest`, `AIPlayerFindClosestEnemyTest`, `AIPlayerExplorationTest`, `GroundStateBaseTest`) pass.
- The project compiles with the existing Ant/NetBeans setup.

## Risks

- **FSM wiring changes exploration behavior.** `ExplorationState` will now actually run instead of the legacy engage loop. This is the intended behavior, but exploration squads will no longer opportunistically attack.
- **Larger Squads.** Raising the effective idle cap can mass more units and affect performance. Values are per-profile and can be tuned down.
- **Defense scoring cost.** Candidate × approach-cell scoring is bounded and only runs when a defense is ready; the approach map is cached.
- **Range fix.** `getRange()` was previously unused by the AI; changing it has no known gameplay effect beyond debug rendering and the new planner.
- **Reinforcement policy.** Recovering squads consume idle attack units before new squads form; this is intentional to make regrouping meaningful.

## Acceptance criteria

- A losing squad disengages, returns to base, heals, absorbs reinforcements, and attacks again without player intervention.
- A squad that is winning does not retreat.
- Defenses are placed in valid, buildable cells within the configured radius.
- New defenses do not stack within minimum spacing and favor chokepoint/threat-side cells.
- Turrets with longer range are placed on longer lanes; shorter-range turrets layer closer in.
- No existing AI test regresses.
