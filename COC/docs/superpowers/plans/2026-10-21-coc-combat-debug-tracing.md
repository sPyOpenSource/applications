# COC Combat / Game-State Debug Tracing — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add structured, toggleable console tracing for combat and game-state events in the COC game, and replace bare `printStackTrace()` calls in touched combat/map code with contextual errors.

**Architecture:** A single `util.DebugLog` utility provides leveled, categorized output to `System.out`/`System.err`. Combat, deploy, defense, result, and map code call it at key state transitions. A system property (`-Dcoc.debug`) controls verbosity.

**Tech Stack:** Java 17, JavaFX, no new dependencies.

## Global Constraints
- All changes are within the `COC/` project.
- No new third-party dependencies.
- Output defaults to `TRACE`; quiet via `-Dcoc.debug=OFF` or `-Dcoc.debug=ERROR`.
- Preserve existing behavior; instrumentation must not change control flow.

---

### Task 1: Create `util.DebugLog`

**Files:**
- Create: `COC/src/util/DebugLog.java`

- [ ] **Step 1: Create the utility class**

```java
package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    public enum Level { OFF, ERROR, WARN, INFO, TRACE }

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static volatile Level level =
            parse(System.getProperty("coc.debug", "TRACE"));

    private DebugLog() {}

    public static void setLevel(Level newLevel) {
        level = newLevel;
    }

    public static Level getLevel() {
        return level;
    }

    private static Level parse(String value) {
        try {
            return Level.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return Level.TRACE;
        }
    }

    private static boolean enabled(Level threshold) {
        return level != Level.OFF && threshold.ordinal() <= level.ordinal();
    }

    private static String prefix(String category) {
        return "[" + LocalDateTime.now().format(TS) + "] ["
                + Thread.currentThread().getName() + "] [" + category + "]";
    }

    public static void error(String category, String message, Throwable throwable) {
        if (!enabled(Level.ERROR)) return;
        System.err.println(prefix(category) + " ERROR " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    public static void warn(String category, String message) {
        if (!enabled(Level.WARN)) return;
        System.out.println(prefix(category) + " WARN " + message);
    }

    public static void info(String category, String message) {
        if (!enabled(Level.INFO)) return;
        System.out.println(prefix(category) + " INFO " + message);
    }

    public static void trace(String category, String message) {
        if (!enabled(Level.TRACE)) return;
        System.out.println(prefix(category) + " TRACE " + message);
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd COC && javac -d build/classes src/util/DebugLog.java`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add COC/src/util/DebugLog.java
git commit -m "feat(coc): add DebugLog utility for combat/game-state tracing"
```

---

### Task 2: Instrument `MapLoader`

**Files:**
- Modify: `COC/src/model/Map/MapLoader.java`

- [ ] **Step 1: Add import**

```java
import util.DebugLog;
```

- [ ] **Step 2: Replace bare print in the default case**

Old:
```java
default:
    System.out.println(id);
```

New:
```java
default:
    DebugLog.warn("map", "Unknown actor id: " + id);
```

- [ ] **Step 3: Commit**

```bash
git add COC/src/model/Map/MapLoader.java
git commit -m "feat(coc): log unknown map actors via DebugLog"
```

---

### Task 3: Instrument `Battle.deploy`

**Files:**
- Modify: `COC/src/view/Battle.java`

- [ ] **Step 1: Add import**

```java
import util.DebugLog;
```

- [ ] **Step 2: Add trace in deploy after capacity update**

Old:
```java
capacityInt.addAndGet(-icon.cost);
capacity.setText("Capacity : " + capacityInt.get());
icon.spawn.apply(x, y).start();
```

New:
```java
capacityInt.addAndGet(-icon.cost);
capacity.setText("Capacity : " + capacityInt.get());
DebugLog.trace("deploy", "Deployed unit at (" + x + "," + y
        + ") cost=" + icon.cost + " remainingCapacity=" + capacityInt.get());
icon.spawn.apply(x, y).start();
```

- [ ] **Step 3: Commit**

```bash
git add COC/src/view/Battle.java
git commit -m "feat(coc): trace unit deployment in Battle"
```

---

### Task 4: Instrument `UnitAttack` base class

**Files:**
- Modify: `COC/src/controller/attack/UnitAttack.java`

- [ ] **Step 1: Add import**

```java
import util.DebugLog;
```

- [ ] **Step 2: Add trace in run() loop**

Old:
```java
while (!unit.isDead()) {
    EntityBuilding building = moveToward();
    if (unit.isDead()) {
        break;
    }
    attack(building);
}
```

New:
```java
while (!unit.isDead()) {
    DebugLog.trace("combat", unit + " seeking nearest building");
    EntityBuilding building = moveToward();
    if (unit.isDead()) {
        DebugLog.trace("combat", unit + " died while moving");
        break;
    }
    if (building == null) {
        DebugLog.trace("combat", unit + " found no target");
        break;
    }
    DebugLog.trace("combat", unit + " attacking " + building);
    attack(building);
}
```

- [ ] **Step 3: Add trace before death cleanup**

Old:
```java
map.getAttackingHeroes().remove(unit);
```

New:
```java
DebugLog.trace("combat", unit + " removed from attacking heroes");
map.getAttackingHeroes().remove(unit);
```

- [ ] **Step 4: Commit**

```bash
git add COC/src/controller/attack/UnitAttack.java
git commit -m "feat(coc): trace combat loop in UnitAttack"
```

---

### Task 5: Instrument `DragonAttack`

**Files:**
- Modify: `COC/src/controller/attack/DragonAttack.java`

- [ ] **Step 1: Add import**

```java
import util.DebugLog;
```

- [ ] **Step 2: Add trace in run()**

After `while (!dragon.isDead()){`:
```java
DebugLog.trace("combat", dragon + " seeking nearest building");
```

In `moveToward()`, when building found, before path transition:
```java
DebugLog.trace("combat", dragon + " moving toward " + building);
```

In `attack()`, at start of `while (building.getHp() >= 0)`:
```java
DebugLog.trace("combat", dragon + " attacking " + building
        + " (hp=" + building.getHp() + ")");
```

After building destroyed:
```java
DebugLog.trace("combat", dragon + " destroyed " + building);
```

- [ ] **Step 3: Replace printStackTrace calls in DragonAttack**

Replace each `e.printStackTrace();` with:
```java
DebugLog.error("combat", "Interrupted in DragonAttack", e);
```

- [ ] **Step 4: Commit**

```bash
git add COC/src/controller/attack/DragonAttack.java
git commit -m "feat(coc): trace combat in DragonAttack"
```

---

### Task 6: Instrument defense classes

**Files:**
- Modify: `COC/src/controller/defense/ArcherTowerDefense.java`
- Modify: `COC/src/controller/defense/TeslaDefense.java`
- Modify: `COC/src/controller/defense/InfernoTowerDefense.java`

- [ ] **Step 1: Add import to each**

```java
import util.DebugLog;
```

- [ ] **Step 2: Trace target selection and damage**

In `selectHero()` (or equivalent), after selecting a hero:
```java
DebugLog.trace("defense", archerTower + " selected target " + hero);
```
(Use the appropriate tower variable name in each class.)

In `attack(...)`, when applying damage:
```java
DebugLog.trace("defense", "<tower> hit " + hero + " (hp=" + hero.getHp() + ")");
```

- [ ] **Step 3: Replace printStackTrace calls**

Replace each `e.printStackTrace();` with:
```java
DebugLog.error("defense", "Interrupted in <Tower>Defense", e);
```

- [ ] **Step 4: Commit**

```bash
git add COC/src/controller/defense/*.java
git commit -m "feat(coc): trace defense actions"
```

---

### Task 7: Instrument `CheckResultAttack`

**Files:**
- Modify: `COC/src/controller/CheckResultAttack.java`

- [ ] **Step 1: Add import**

```java
import util.DebugLog;
```

- [ ] **Step 2: Trace match progress and decisions**

In the win branch:
```java
DebugLog.info("result", "Defender eliminated; attacker wins");
```

In the 15% branch:
```java
DebugLog.info("result", "Attacker reaches 2-star (buildings remaining "
        + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
```

In the 50% branch:
```java
DebugLog.info("result", "Attacker reaches 1-star (buildings remaining "
        + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
```

In the timeout loss branch:
```java
DebugLog.info("result", "Time expired; defender wins (buildings remaining "
        + map.getBuildingsMap().size() + "/" + (int) firstSize + ")");
```

- [ ] **Step 3: Replace printStackTrace calls**

Replace each `e.printStackTrace();` with:
```java
DebugLog.error("result", "Interrupted in CheckResultAttack", e);
```

- [ ] **Step 4: Commit**

```bash
git add COC/src/controller/CheckResultAttack.java
git commit -m "feat(coc): trace match result decisions"
```

---

### Task 8: Manual verification

- [ ] **Step 1: Compile the project**

Run: `cd COC && ant compile` (or IDE build)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run with default (TRACE) level**

Launch the game, start an attack, deploy units, and confirm console shows deploy/combat/defense/result traces.

- [ ] **Step 3: Run with OFF level**

Run with `-Dcoc.debug=OFF` and confirm no trace lines are emitted.

- [ ] **Step 4: Commit verification note (optional)**

No code change required.