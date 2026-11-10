# AI Air Force Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the JavaRA AI build and use air units (Dragon) as part of its forces.

**Architecture:** Extend the existing AI production/role-assignment pipeline so aircraft are built via the air production slot, detected as new units, included in assault squads, and moved toward enemy targets. Harden dependent AI helpers (MCV check, enemy targeting, unit-name loading).

**Tech Stack:** Java 21, JavaFX 25, JUnit 4, SnakeYAML.

## Global Constraints

- Source/target Java 21; JavaFX 25 module path.
- Existing `AIPlayerTest` must remain green.
- No new aircraft types; Dragon only.

---

### Task 1: ProductionQueue air routing

**Files:**
- Modify: `JavaRA/src/cr0s/javara/gameplay/ProductionQueue.java`

- [x] Add `getCurrentAirProduction()` returning `currentAir`.
- [x] Route `EntityAircraft` in `startBuildingActor(String, ...)` to `currentAir`.

### Task 2: AIPlayer air awareness

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java`

- [x] Import `EntityAircraft`.
- [x] Implement `hasAdequateAirUnits(EntityActor)` and no-arg overload.
- [x] Add ownership guard in `findNewUnits()`; aircraft flow to hanging units.
- [x] Include aircraft in `createAttackForce()`; avoid empty squads.
- [x] Guard air production overlap in `buidUnits()` case 3.
- [x] Add `createAirSquad()` helper.
- [x] Expose `getUnitsHangingAroundTheBase()` and public `getSquadOfType()`.

### Task 3: AI helper hardening

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java`

- [x] Restore `thereIsNoMcv()` owned-MCV check.
- [x] Fix `UnitsCommonNames` loading into `unitCommonNames`.
- [x] Guard `chooseEnemyTarget()` against empty enemies.
- [x] Fix neutral-vehicles branch in `buidUnits()`.
- [x] Prefer ASSAULT squad in `tryToRushAttack()`.

### Task 4: Verification

- [x] Compile JavaRA module.
- [x] Run `AIPlayerTest` (10 tests pass).