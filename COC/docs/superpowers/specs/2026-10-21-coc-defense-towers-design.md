# COC Defense Towers — Never Fire Fix

Date: 2026-10-21
Status: approved

## Problem

Defensive towers (Archer Tower, Inferno Tower, Tesla) in battle never fire at
attacking units. Two independent causes:

1. **Capacity race at battle start.** `Battle` initializes `capacityInt` to
   `level * 0`, so capacity starts at 0. Every defense thread treats
   `capacityInt == 0 && no attacking heroes` as "battle over" and calls
   `setDead()` on the tower in its first `selectHero()` loop iteration —
   before any unit is deployed and before `CheckResultAttack`'s regeneration
   tick (first tick at 600 ms) can raise capacity above 0. The tower thread
   exits immediately and the tower never attacks.
2. **Tesla type mismatch.** `MapLoader.buildMap()` instantiates the soviet
   `EntityTeslaCoil` for `"tesla"` map actors, while `Battle.start()` only
   launches `TeslaDefense` for `cr0s.javara.entity.building.china.Tesla`.
   Tesla towers never receive a defense thread at all.

## Decision

Battle end is governed solely by `CheckResultAttack`'s 30-second timer and its
all-buildings-destroyed check. Capacity remains an in-battle resource with its
existing regeneration behavior; it no longer participates in battle-end or
tower-lifecycle logic.

## Changes

### 1. Defense controllers — remove capacity-based death conditions

In `COC/src/controller/defense/ArcherTowerDefense.java`,
`InfernoTowerDefense.java`, and `TeslaDefense.java`:

- In `selectHero()`, replace the death check

  ```java
  if (map.getBuildingsMap().isEmpty() || tower.getHp() <= 0
          || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
      tower.setDead();
  }
  ```

  with only the building/HP conditions:

  ```java
  if (map.getBuildingsMap().isEmpty() || tower.getHp() <= 0) {
      tower.setDead();
  }
  ```

- In `attack()`, apply the same removal to the inner loop's death check
  (`tower.getHp() <= 0` alone remains; the `capacityInt` disjunct is dropped).

The `capacityInt` constructor parameter and field remain (call sites unchanged)
even though the controllers no longer read them, to keep this change minimal;
they can be dropped in a follow-up cleanup if desired.

Result: a tower thread stays alive while its HP and the defender's buildings
allow, selects targets as they enter range, and idles (1 s wait in
`selectHero()`) when no attacker is in range. When the 30 s timer expires,
`CheckResultAttack` ends the battle and the stage closes.

### 2. Tesla wiring — `MapLoader` instantiates `china.Tesla`

In `COC/src/model/Map/MapLoader.java`, `buildMap()`:

```java
case "tesla":
    Tesla tesla = new Tesla(new Pos(x, y));
    buildingsMap.add(tesla);
    break;
```

instead of `EntityTeslaCoil`. Remove the now-unused
`cr0s.javara.entity.building.soviet.EntityTeslaCoil` import if no other use
remains.

`china.Tesla` extends `Defensive`, which loads the sprite and sets the
ImageView in its constructor — same as `ArcherTower` and `InfernoTower`, so it
renders in `getMapView()` and matches `Battle.start()`'s `instanceof Tesla`
branch. `TeslaDefense` reads `getX()/getY()/getRange()/getDamagePerSecond()`,
all provided by `Defensive`.

### 3. Untouched

- `capacityInt` initialization (`level * 0`) and regeneration in
  `CheckResultAttack` ( +2 per 600 ms up to `level * 15` ).
- `CheckResultAttack`'s 30-second timer and win/lose evaluation.
- `Battle.start()`'s tower-thread launch loop.
- The attack controllers (`controller/attack/*`).

## Error handling and concurrency

No new synchronization is introduced. The existing wait/notify handshake in
each controller is unchanged; only the conditions that call `setDead()` shrink.
A tower with no valid target idles in `selectHero()`'s `wait(1000)` loop until
the thread ends via HP loss or map destruction, or the stage closes when the
battle timer expires.

## Testing

JavaFX GUI with threaded controllers — verification is manual via `ant run`:

1. Enter a battle with all three tower types (map1 has 2 Archer Towers,
   1 Tesla, 1 Inferno Tower).
2. Deploy a unit; confirm each tower type acquires targets in range, renders
   its beam/projectile, and ticks attacker HP down.
3. Let the Tesla take damage from a unit; confirm it still fires while alive.
4. Confirm battle ends at 30 s (or when all buildings are destroyed) with the
   usual star/percentage screen.
5. Confirm capacity still regenerates and gates unit deployment as before.

The existing JUnit stub (`test/test/testClass.java`) is untouched;
unit-testing the threaded GUI controllers is out of scope.

## Out of scope

- Refactoring the wait/notify threading model of the defense controllers.
- Dropping the now-unused `capacityInt` parameters from defense controllers.
- Rebalancing tower range, HP, or damage.
- Changing attack controllers or `CheckResultAttack` beyond what is stated.

## Success criteria

- All three tower types fire at attacking units while they are in range.
- No tower marks itself dead before any unit is deployed.
- Battle ends after the 30-second timer or full building destruction, with the
  existing win/lose star screen.
- Capacity regenerates and gates deployment exactly as before.

## File map

| Action | Path |
|---|---|
| edit | `COC/src/controller/defense/ArcherTowerDefense.java` |
| edit | `COC/src/controller/defense/InfernoTowerDefense.java` |
| edit | `COC/src/controller/defense/TeslaDefense.java` |
| edit | `COC/src/model/Map/MapLoader.java` |
| untouched | `COC/src/controller/CheckResultAttack.java`, `COC/src/view/Battle.java`, `controller/attack/*` |
