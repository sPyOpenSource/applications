# AI Air Force — Design

Date: 2026-11-09
Project: JavaRA (cr0s.javara.ai)
Scope: Make the AI build and use air units (Dragon) as part of its forces.

## Problem

The AI already has partial air support: airfields can be built, Dragons can be produced and deployed, and `buidUnits()` has an air branch. However, several gaps prevent the AI from actually using air units:

- `createAttackForce()` excludes aircraft (only `ICanAttack` units are added) and registers empty squads.
- `findNewUnits()` does not verify ownership, so enemy units can be absorbed; aircraft are not explicitly handled.
- `buidUnits()` air branch does not guard against overlapping production and does not check airfield readiness consistently.
- `ProductionQueue.startBuildingActor(String, ...)` does not route aircraft to the air production slot.
- `hasAdequateAirUnits()` is a stub returning `true`.
- `Squad`/target handling for non-attacking aircraft is inconsistent.
- Supporting AI helpers (`thereIsNoMcv`, `loadAIRules` unit names, `chooseEnemyTarget`, etc.) contain defects that affect overall AI stability.

## Goal

Ensure the AI builds airfields and Dragons, deploys them, and includes them in assault squads so they move to and engage enemy targets alongside ground forces. Also harden the AI helpers that the air logic depends on.

## Changes

### 1. AIPlayer

- Add import for `EntityAircraft`.
- `hasAdequateAirUnits(EntityActor a)`: return `true` only for live `EntityAircraft`; add no-arg overload counting owned live aircraft.
- `findNewUnits()`: skip units not owned by this AI; aircraft flow into `unitsHangingAroundTheBase` like other combat units.
- `createAttackForce()`: collect units that are `ICanAttack` or `EntityAircraft`; only register a squad and clear hanging units when at least one such unit exists.
- `buidUnits()` case 3 (Air): guard against overlapping air production; build from the `air` map when an airfield is present.
- `tryToRushAttack()`: prefer an ASSAULT squad when available.
- `thereIsNoMcv()`: restore owned, non-dead MCV check.
- `loadAIRules()`: load `UnitsCommonNames` into `unitCommonNames` (not `buildingCommonNames`).
- `chooseEnemyTarget()`: guard against empty enemy list.
- Expose `getUnitsHangingAroundTheBase()` and make `getSquadOfType()` public.
- Add `createAirSquad()` helper to form a dedicated AIR squad from hanging aircraft.

### 2. ProductionQueue

- `startBuildingActor(String, ...)`: route `EntityAircraft` targets to `currentAir`.
- Add `getCurrentAirProduction()`.

### 3. Squad

- `isValid()` already treats all-dead squads as invalid (unchanged).
- `setTarget()`/`update()` already issue Move orders to non-attacking units, including aircraft.

## Out of scope

- Making Dragon a fully weaponized attacker (it moves with squads and uses existing rendering).
- Naval unit production.
- New aircraft types beyond Dragon.

## Testing

Compile the JavaRA module (Java 21, JavaFX 25). Existing `AIPlayerTest` must pass. Manual smoke: AI builds airfield, produces Dragon, and includes it in attacks.