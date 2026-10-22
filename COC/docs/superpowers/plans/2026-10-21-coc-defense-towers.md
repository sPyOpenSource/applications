# COC Defense Towers — Never Fire Fix: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make COC defensive towers (Archer Tower, Inferno Tower, Tesla) fire at attacking units instead of marking themselves dead at battle start.

**Architecture:** Each defense controller is a `Thread` that loops `selectHero()` / `attack()` and calls `setDead()` on conditions. Two defects break it: (1) all three controllers treat `capacityInt == 0 && no heroes` as "battle over", and capacity starts at 0, so towers die in their first loop iteration; (2) `MapLoader` instantiates a soviet `EntityTeslaCoil` for `"tesla"` actors while `Battle.start()` only launches `TeslaDefense` for `china.Tesla`. The fix removes the capacity disjunct from the death checks and fixes the Tesla type. Battle end stays in `CheckResultAttack`'s 30-second timer.

**Tech Stack:** Java 21-targeted JavaFX 25 application (NetBeans Ant build), threaded controllers with wait/notify synchronization.

## Global Constraints

- Build command (from `COC/` directory; verified working — the NetBeans `libs.JavaFX.classpath` maps to the JavaFX 25 SDK, and the compiler must be JDK 25+ to read its class files):

  ```bash
  cd COC && JAVA_HOME=/opt/homebrew/opt/openjdk@25 ant -Dlibs.JavaFX.classpath="/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx-swt.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.base.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.controls.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.fxml.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.graphics.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.media.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.swing.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.web.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/jdk.jsobject.jar" compile
  ```

  Expected: `BUILD SUCCESSFUL`.
- Keep the `capacityInt` constructor parameter and field in all three defense controllers even though they become unread (call sites in `Battle.start()` stay unchanged). Do not touch `capacityInt` initialization or `CheckResultAttack`.
- No new synchronization, threads, or classes. Only delete existing condition disjuncts and change one constructor call.
- The project is a JavaFX GUI with threaded controllers; there are no unit tests (existing stub `COC/test/test/testClass.java` stays untouched). Verification = compile + manual in-game check (Task 3).

---

### Task 1: Remove capacity-based death conditions from the three defense controllers

**Files:**
- Modify: `COC/src/controller/defense/ArcherTowerDefense.java`
- Modify: `COC/src/controller/defense/InfernoTowerDefense.java`
- Modify: `COC/src/controller/defense/TeslaDefense.java`

**Interfaces:**
- Consumes: nothing new — only deletes reads of the existing `capacityInt` (`java.util.concurrent.atomic.AtomicInteger`) field. The `map` (`cr0s.javara.render.map.Map`) and tower fields are unchanged.
- Produces: no signature changes. `COC/src/view/Battle.java` call sites (`new ArcherTowerDefense(root, map, tower, capacityInt)` etc.) remain valid because the constructor parameters are kept.

- [ ] **Step 1: Edit `ArcherTowerDefense.java` — `selectHero()`**

Find (inside `selectHero()`, after the for-loop):

```java
        if (map.getBuildingsMap().isEmpty() || archerTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
            archerTower.setDead();
        }
```

Replace with:

```java
        if (map.getBuildingsMap().isEmpty() || archerTower.getHp() <= 0) {
            archerTower.setDead();
        }
```

- [ ] **Step 2: Edit `ArcherTowerDefense.java` — `attack()`**

Find (at the top of the while-loop in `attack()`):

```java
            if (archerTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                archerTower.setDead();
                break;
            }
```

Replace with:

```java
            if (archerTower.getHp() <= 0) {
                archerTower.setDead();
                break;
            }
```

- [ ] **Step 3: Edit `InfernoTowerDefense.java` — `selectHero()`**

Find:

```java
        if (map.getBuildingsMap().isEmpty() || infernoTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
            infernoTower.setDead();
        }
```

Replace with:

```java
        if (map.getBuildingsMap().isEmpty() || infernoTower.getHp() <= 0) {
            infernoTower.setDead();
        }
```

- [ ] **Step 4: Edit `InfernoTowerDefense.java` — `attack()`**

Find (top of the while-loop in `attack()`, one indent level deeper than in the archer file):

```java
                if (infernoTower.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                    infernoTower.setDead();
                    break;
                }
```

Replace with:

```java
                if (infernoTower.getHp() <= 0) {
                    infernoTower.setDead();
                    break;
                }
```

- [ ] **Step 5: Edit `TeslaDefense.java` — `selectHero()`**

Find:

```java
        if (map.getBuildingsMap().isEmpty() || tesla.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
            tesla.setDead();
        }
```

Replace with:

```java
        if (map.getBuildingsMap().isEmpty() || tesla.getHp() <= 0) {
            tesla.setDead();
        }
```

- [ ] **Step 6: Edit `TeslaDefense.java` — `attack()`**

Find (top of the while-loop in `attack()`):

```java
            if (tesla.getHp() <= 0 || (map.getAttackingHeroes().isEmpty() && capacityInt.get() == 0)) {
                tesla.setDead();
                break;
            }
```

Replace with:

```java
            if (tesla.getHp() <= 0) {
                tesla.setDead();
                break;
            }
```

- [ ] **Step 7: Verify no remaining capacity-based death checks**

Run:

```bash
grep -n "capacityInt.get() == 0" COC/src/controller/defense/*.java
```

Expected: no output (empty). If any match remains, a find/replace step above was missed — fix it before compiling.

- [ ] **Step 8: Compile**

Run the build command from Global Constraints (from the `COC/` directory).

Expected: `BUILD SUCCESSFUL`. (Note: `capacityInt` fields are now unread in these files — that is a warning at most, not an error; do not "fix" it by removing the fields.)

- [ ] **Step 9: Commit**

```bash
git add COC/src/controller/defense/ArcherTowerDefense.java COC/src/controller/defense/InfernoTowerDefense.java COC/src/controller/defense/TeslaDefense.java
git commit -m "fix(coc): stop defense towers dying on capacity-based battle-over check"
```

---

### Task 2: Fix Tesla type in `MapLoader`

**Files:**
- Modify: `COC/src/model/Map/MapLoader.java`

**Interfaces:**
- Consumes: `cr0s.javara.entity.building.china.Tesla` (already wildcard-imported via `import cr0s.javara.entity.building.china.*;` at line 16) — a `Defensive` subclass whose constructor is `Tesla(Pos x)`, loading its sprite and ImageView in the superclass constructor exactly like `ArcherTower` and `InfernoTower`.
- Produces: `buildingsMap` entries for `"tesla"` become `china.Tesla`, which satisfies the `building instanceof Tesla tesla` branch in `COC/src/view/Battle.java:70-71` so `TeslaDefense` threads start. `TeslaDefense` reads `getImageView()/getRange()/getDamagePerSecond()/getHp()` — all provided by `Defensive`.

- [ ] **Step 1: Replace the tesla actor case in `MapLoader.java`**

Find (in `buildMap()`):

```java
                case "tesla":
                    EntityTeslaCoil tesla = new EntityTeslaCoil(new Pos(x, y));
                    buildingsMap.add(tesla);
                    break;
```

Replace with:

```java
                case "tesla":
                    Tesla tesla = new Tesla(new Pos(x, y));
                    buildingsMap.add(tesla);
                    break;
```

- [ ] **Step 2: Remove the now-unused import**

Find and delete this line:

```java
import cr0s.javara.entity.building.soviet.EntityTeslaCoil;
```

(Verify it is unused first: `grep -n "EntityTeslaCoil" COC/src/model/Map/MapLoader.java` should show no occurrences after Step 1's replacement besides this import line.)

- [ ] **Step 3: Compile**

Run the build command from Global Constraints (from the `COC/` directory).

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add COC/src/model/Map/MapLoader.java
git commit -m "fix(coc): load china.Tesla so Battle starts TeslaDefense threads"
```

---

### Task 3: Manual in-game verification

**Files:** none modified — verification only.

**Interfaces:**
- Consumes: Tasks 1 and 2 (compiled `COC/build/classes`).
- Produces: evidence that the bug is fixed; no code.

- [ ] **Step 1: Run the app**

Run the build command from Global Constraints but with the `run` target instead of `compile` (from the `COC/` directory):

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 ant -Dlibs.JavaFX.classpath="/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx-swt.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.base.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.controls.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.fxml.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.graphics.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.media.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.swing.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/javafx.web.jar:/Users/xuyi/opt/javafx-sdk-25.0.2/lib/jdk.jsobject.jar" run
```

Expected: the game window opens at the login screen.

- [ ] **Step 2: Enter a battle**

Sign up / log in, reach the player panel, and click through to `SearchOpponents`. Wait for the opponent map preview, then click the attack icon. map1 (`assets/src/assets/maps/map1.yaml`) contains 2 Archer Towers, 1 Tesla, 1 Inferno Tower, plus a town hall and other buildings.

- [ ] **Step 3: Deploy and watch towers fire**

Select a troop icon (e.g. Dragon or Panda) and click on the map to deploy a unit near the towers. Expected for each tower type:
- Archer Tower: spawns small purple projectile circles that travel toward the target.
- Inferno Tower: renders an orange (`#FF6F00`) beam to the target.
- Tesla: moves its attack gif sprite along the target path.

None of the towers should have gone silently dead before the first unit is deployed. Deploy 2–3 units so multiple towers have targets.

- [ ] **Step 4: Verify battle end and capacity**

Stay in the battle with no further deployments until the 30-second timer expires (first deployments drain capacity). Expected: the star/percentage result screen appears; the capacity counter still regenerates over time and gates deployment exactly as before.

- [ ] **Step 5: Record results in the plan**

If any tower type failed to fire, reopen Task 1/2 and compare the edited spots against the plan before changing anything else. If all three fire and battle end works, no commit is needed — the work is complete.
