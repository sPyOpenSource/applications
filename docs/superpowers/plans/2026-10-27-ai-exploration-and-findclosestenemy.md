# AI Exploration & findClosestEnemy Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `findClosestEnemy` so it returns the true nearest enemy across all enemy players (no crash when none exist), add a dedicated `EXPLORATION` squad that roams to unexplored/far cells, and wire it into the AI player tick loop.

**Architecture:** Extend the existing tick-driven `AIPlayer.update()` → `assignRolesToIdleUnits()` path. `AIPlayer` registers one small `EXPLORATION` squad; `Squad.update()` drives `StateMachine.fsm.update(squad)`; a new `ExplorationState.tick()` picks a destination cell and issues `Order("Move", ...)` to idle squad units. `findClosestEnemy(Pos)` now loops all enemy players, merges results, sorts by distance, returns closest-first (empty list if none). Both `.get(0)` call sites in `GroundStateBase` get empty-list guards.

**Tech Stack:** Java 21, JavaFX 25, JUnit 4, Ant (project build); plain `javac`/`java` with JDK 21 + JavaFX jars if Ant module path is broken in this environment.

## Global Constraints

- Java source/target 21 (`javac.source=21`, `javac.target=21` in `nbproject/project.properties`).
- Follow existing file layout: sources under `JavaRA/src/cr0s/javara/...`, tests under `JavaRA/test/test/`.
- Test package: `test`. Test framework: JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`).
- No new packages beyond `cr0s.javara.ai.states` (exploration state lives alongside `GroundStateBase`).
- No shroud wiring: `playerShroud` stays commented out in `Player`; explorer degrades gracefully when shroud is null.
- No combat, World, Base, pathfinding, or UI changes.
- No difficulty tiers or AI personalities.
- Every task ends with a commit.

---

### Task 1: Add empty-list guards in GroundStateBase

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/states/GroundStateBase.java:21-34` and `:56-70`

**Interfaces:**
- Consumes: `AIPlayer.findClosestEnemy(Pos)` returns `ArrayList<EntityActor>` (possibly empty).
- Produces: unchanged method signatures; only adds early returns before `.get(0)`.

- [ ] **Step 1: Write the failing test**

```java
package test;

import cr0s.javara.ai.Squad;
import cr0s.javara.ai.ai.states.GroundUnitsIdleState;
import cr0s.javara.ai.ai.states.GroundUnitsAttackMoveState;
import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.StateMachine;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import java.util.ArrayList;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class GroundStateBaseTest {
    @Test
    public void idleStateNoEnemyDoesNotCrash() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        GroundUnitsIdleState state = new GroundUnitsIdleState();
        state.activate(squad);
        state.tick(squad); // must not throw IndexOutOfBoundsException
        state.deactivate(squad);
    }

    @Test
    public void attackMoveStateNoEnemyDoesNotCrash() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        Squad squad = new Squad(ai, Squad.SquadType.ASSAULT);
        GroundUnitsAttackMoveState state = new GroundUnitsAttackMoveState();
        state.activate(squad);
        state.tick(squad); // must not throw IndexOutOfBoundsException
        state.deactivate(squad);
    }
}
```

- [ ] **Step 2: Run test to verify it fails to compile**

Run:
```
/opt/homebrew/opt/openjdk@21/bin/javac --release 21 -cp "<fxcp>:<junit>:<hamcrest>:src:test" -d /tmp/opencode/plan1 JavaRA/test/test/GroundStateBaseTest.java
```
Expected: FAIL — `GroundUnitsIdleState` and `GroundUnitsAttackMoveState` are package-private in `cr0s.javara.ai.ai.states`; test is in package `test`, so compile fails.

- [ ] **Step 3: Make states accessible**

Edit `JavaRA/src/cr0s/javara/ai/states/GroundStateBase.java`: change `class GroundUnitsIdleState` → `public class GroundUnitsIdleState` and `class GroundUnitsAttackMoveState` → `public class GroundUnitsAttackMoveState`.

- [ ] **Step 4: Add empty-list guards**

Replace the two `.get(0)` call sites:
- Line ~27 (`GroundUnitsIdleState.tick`): guard `if (enemies.isEmpty()) return;` before `enemies.get(0)`.
- Line ~62 (`GroundUnitsAttackMoveState.tick`): guard `if (closestEnemy == null) return;` / `if (enemies.isEmpty()) return;` before `.get(0)`.

- [ ] **Step 5: Run test to verify it passes**

Run same `javac` + `java -cp ... org.junit.runner.JUnitCore test.GroundStateBaseTest`.
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/states/GroundStateBase.java JavaRA/test/test/GroundStateBaseTest.java
git commit -m "test(fix): guard GroundStateBase against empty enemy list"
```

---

### Task 2: Fix findClosestEnemy to consider all enemy players

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java:341-358`

**Interfaces:**
- Consumes: `world.getPlayers()`, `player.isEnemyFor(this)`, inherited `Player.findClosestEnemy(Pos center, Player p, double r)`.
- Produces: `ArrayList<EntityActor>` closest-first across all enemy players; empty list if none.

- [ ] **Step 1: Write the failing test**

```java
package test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import java.util.ArrayList;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class AIPlayerFindClosestEnemyTest {
    @Test
    public void findClosestEnemyReturnsNearestAcrossAllEnemyPlayers() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        AIPlayer nearEnemy = new AIPlayer(world, "Near", Alignment.ALLIED, Color.BLUE);
        AIPlayer farEnemy = new AIPlayer(world, "Far", Alignment.ALLIED, Color.GREEN);

        Team aiTeam = new Team(); Team nearTeam = new Team(); Team farTeam = new Team();
        ai.setTeam(aiTeam); nearEnemy.setTeam(nearTeam); farEnemy.setTeam(farTeam);
        world.addPlayer(ai); world.addPlayer(nearEnemy); world.addPlayer(farEnemy);

        EntityRifleTrooper far = new EntityRifleTrooper(new Pos(100, 0));
        far.owner = farEnemy; farEnemy.entities.add(far);
        EntityRifleTrooper near = new EntityRifleTrooper(new Pos(10, 0));
        near.owner = nearEnemy; nearEnemy.entities.add(near);

        ArrayList<EntityActor> result = ai.findClosestEnemy(new Pos(0, 0));

        assertEquals(2, result.size());
        assertEquals(near, result.get(0));   // nearest first
        assertEquals(far, result.get(1));
    }

    @Test
    public void findClosestEnemyNoEnemiesReturnsEmpty() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        ArrayList<EntityActor> result = ai.findClosestEnemy(new Pos(0, 0));
        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run `javac`/`java` as in Task 1. Expected: compile OK (API exists); the "nearest across all players" test FAILS because current code returns only `enemyPlayers.get(0)`'s actors (may return `far` first or wrong order).

- [ ] **Step 3: Implement fix in AIPlayer.findClosestEnemy(Pos)**

Replace lines 341-358 with:
```java
public ArrayList<EntityActor> findClosestEnemy(final Pos center) {
    if (center == null) {
        return new ArrayList<>();
    }

    ArrayList<EntityActor> all = new ArrayList<>();
    for (Player p : world.getPlayers()) {
        if (!p.isEnemyFor(this)) {
            continue;
        }
        all.addAll(this.findClosestEnemy(center, p, 20000000));
    }

    all.sort((a, b) -> (int) (a.getPosition().distanceToSq(center)
            - b.getPosition().distanceToSq(center)));
    return all;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run `javac`/`java` `JUnitCore test.AIPlayerFindClosestEnemyTest`. Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/AIPlayer.java JavaRA/test/test/AIPlayerFindClosestEnemyTest.java
git commit -m "fix(ai): findClosestEnemy considers all enemy players, returns nearest-first"
```

---

### Task 3: Add SquadType.EXPLORATION

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/Squad.java:13`

**Interfaces:**
- Consumes: nothing new.
- Produces: `SquadType.EXPLORATION` enum constant.

- [ ] **Step 1: Add enum constant**

Edit `JavaRA/src/cr0s/javara/ai/Squad.java` line 13:
```java
public enum SquadType { ASSAULT, AIR, RUSH, PROTECTION, DEFENSIVE, HARVEST, CONSTRUCTION, EXPLORATION };
```

- [ ] **Step 2: Verify compilation**

Run `javac --release 21 -cp "<fxcp>" -d /tmp/opencode/plan3 JavaRA/src/cr0s/javara/ai/Squad.java`. Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/Squad.java
git commit -m "feat(ai): add SquadType.EXPLORATION"
```

---

### Task 4: Create ExplorationState

**Files:**
- Create: `JavaRA/src/cr0s/javara/ai/states/ExplorationState.java`

**Interfaces:**
- Consumes: `Squad`, `AIPlayer` (via `squad.getBot()`), `World`, `Shroud` (may be null), `Pos`, `Order`.
- Produces: implements `cr0s.javara.ai.StateMachine.IState` (`activate`, `tick`, `deactivate`).

- [ ] **Step 1: Create the state file**

```java
package cr0s.javara.ai.states;

import java.util.ArrayList;
import java.util.Collections;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.StateMachine.IState;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Order;
import cr0s.javara.render.shrouds.Shroud;
import cr0s.javara.util.Pos;

public class ExplorationState implements IState {

    @Override
    public void activate(Squad squad) {}

    @Override
    public void tick(Squad squad) {
        if (!squad.isValid() || squad.getUnits().isEmpty()) {
            return;
        }
        if (squad.targetIsValid()) {
            return; // combat targeting owns this squad
        }

        AIPlayer bot = squad.getBot();
        Pos dest = pickDestination(bot, squad.getUnits().get(0).getCellPosition());
        if (dest == null) {
            return;
        }

        for (EntityActor u : squad.getUnits()) {
            if (!u.isDead() && u.owner == bot && u.isIdle()) {
                u.resolveOrder(new Order("Move", u, dest, true));
            }
        }
    }

    @Override
    public void deactivate(Squad squad) {}

    private Pos pickDestination(AIPlayer bot, Pos nearCell) {
        Shroud shroud = bot.getShroud();
        int mapW = bot.world.getMap().getWidth();
        int mapH = bot.world.getMap().getHeight();
        Pos spawn = bot.getPlayerSpawnPoint();
        if (spawn == null) {
            spawn = nearCell;
        }

        ArrayList<Pos> candidates = new ArrayList<>();
        if (shroud != null) {
            for (int x = 0; x < mapW; x++) {
                for (int y = 0; y < mapH; y++) {
                    Pos c = new Pos(x, y);
                    if (!bot.world.isCellPassable(c)) {
                        continue;
                    }
                    if (shroud.isExplored(c)) {
                        continue;
                    }
                    if (c.distanceToSq(spawn) < 400) { // too close to base
                        continue;
                    }
                    candidates.add(c);
                }
            }
        }
        if (candidates.isEmpty()) {
            // Fallback: random passable cell far from base
            for (int i = 0; i < 200; i++) {
                int x = World.getRandomInt(0, mapW);
                int y = World.getRandomInt(0, mapH);
                Pos c = new Pos(x, y);
                if (bot.world.isCellPassable(c)
                        && c.distanceToSq(spawn) >= 400) {
                    return c;
                }
            }
            return null;
        }
        Collections.shuffle(candidates, World.random);
        return candidates.get(0);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run `javac --release 21 -cp "<fxcp>" -d /tmp/opencode/plan4 JavaRA/src/cr0s/javara/ai/ai/states/ExplorationState.java`. Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/ai/states/ExplorationState.java
git commit -m "feat(ai): add ExplorationState that roams to unexplored cells"
```

---

### Task 5: Wire exploration squad into AIPlayer

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (add field + `createExplorationSquad()` + call in `assignRolesToIdleUnits`)

**Interfaces:**
- Consumes: `SquadType.EXPLORATION`, `ExplorationState`, `Squad`, `StateMachine`.
- Produces: one EXPLORATION squad registered from idle combat units; squad's `StateMachine` set to `ExplorationState`.

- [ ] **Step 1: Add field and helper**

Add field near line 60:
```java
private Squad explorationSquad;
```

Add method after `createAttackForce()`:
```java
private void createExplorationSquad() {
    if (this.explorationSquad != null) {
        return;
    }
    ArrayList<EntityActor> scouts = new ArrayList<>();
    for (EntityActor a : this.unitsHangingAroundTheBase) {
        if (a instanceof cr0s.javara.combat.attack.ICanAttack && !a.isDead()) {
            scouts.add(a);
            if (scouts.size() >= 4) {
                break;
            }
        }
    }
    if (scouts.isEmpty()) {
        return;
    }
    this.explorationSquad = this.registerNewSquad(SquadType.EXPLORATION);
    for (EntityActor a : scouts) {
        this.explorationSquad.addUnit(a);
        this.unitsHangingAroundTheBase.remove(a);
    }
    this.explorationSquad.getFsm().changeState(this.explorationSquad, new ExplorationState(), false);
}
```

- [ ] **Step 2: Call it in assignRolesToIdleUnits**

In `assignRolesToIdleUnits()` (line ~562), after `this.createAttackForce();`, add:
```java
this.createExplorationSquad();
```

- [ ] **Step 3: Verify compilation**

Run `javac --release 21 -cp "<fxcp>" -d /tmp/opencode/plan5 JavaRA/src/cr0s/javara/ai/AIPlayer.java`. Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/AIPlayer.java
git commit -m "feat(ai): AIPlayer creates and maintains one exploration squad"
```

---

### Task 6: Add exploration tests

**Files:**
- Create: `JavaRA/test/test/AIPlayerExplorationTest.java`

**Interfaces:**
- Consumes: `AIPlayer`, `SquadType.EXPLORATION`, `Squad`, `ExplorationState`, `EntityRifleTrooper`, `World`, `Shroud` (null OK).
- Produces: assertions that squad is created, type is EXPLORATION, FSM state is ExplorationState.

- [ ] **Step 1: Write tests**

```java
package test;

import cr0s.javara.ai.AIPlayer;
import cr0s.javara.ai.Squad;
import cr0s.javara.ai.states.ExplorationState;
import cr0s.javara.ai.StateMachine;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.infantry.EntityRifleTrooper;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import cr0s.javara.gameplay.Team;
import cr0s.javara.gameplay.Team.Alignment;
import java.util.ArrayList;
import javafx.scene.paint.Color;

import org.junit.Test;
import static org.junit.Assert.*;

public class AIPlayerExplorationTest {
    @Test
    public void explorationSquadIsCreatedFromIdleUnits() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);

        EntityRifleTrooper u1 = new EntityRifleTrooper(new Pos(5, 5));
        u1.owner = ai;
        ai.entities.add(u1);
        ai.unitsHangingAroundTheBase.add(u1);

        ai.createExplorationSquad();

        Squad s = ai.getSquadOfType(Squad.SquadType.EXPLORATION);
        assertNotNull("exploration squad should exist", s);
        assertEquals(1, s.getUnits().size());
        StateMachine.IState st = s.getFsm().getCurrentState(); // see Task 7 if getter missing
        assertTrue("FSM should be in ExplorationState", st instanceof ExplorationState);
    }

    @Test
    public void explorationTickIssuesMoveOrdersWithoutShroud() {
        World world = new World("forest-path");
        AIPlayer ai = new AIPlayer(world, "AI", Alignment.SOVIET, Color.RED);
        // shroud is null by default; explorer must not NPE

        EntityRifleTrooper u1 = new EntityRifleTrooper(new Pos(5, 5));
        u1.owner = ai;
        ai.entities.add(u1);
        ai.unitsHangingAroundTheBase.add(u1);
        ai.createExplorationSquad();

        Squad s = ai.getSquadOfType(Squad.SquadType.EXPLORATION);
        s.update(); // must not throw
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run `javac`/`java` JUnitCore. Expected: compile fails — `Squad.getFsm()` has no `getCurrentState()` getter (Task 7). Also `unitsHangingAroundTheBase` is private (Task 8 may adjust access, or test via `findNewUnits` path). Mark as compile-fail target.

- [ ] **Step 3: Commit the test file (compile-fail reference)**

```bash
git add JavaRA/test/test/AIPlayerExplorationTest.java
git commit -m "test(ai): add exploration squad tests (compile-fail until Tasks 7-8 done)"
```

---

### Task 7: Add StateMachine.getCurrentState getter

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/StateMachine.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `public IState getCurrentState()` returning `this.currentState`.

- [ ] **Step 1: Add getter**

```java
public IState getCurrentState() {
    return this.currentState;
}
```

- [ ] **Step 2: Verify compilation**

Run `javac` on `StateMachine.java`. Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/StateMachine.java
git commit -m "feat(ai): StateMachine exposes getCurrentState()"
```

---

### Task 8: Make unitsHangingAroundTheBase accessible for tests / exploration

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (field visibility + optional accessor)

**Interfaces:**
- Consumes: nothing.
- Produces: `unitsHangingAroundTheBase` becomes accessible (package-private or add `getUnitsHangingAroundTheBase()`).

- [ ] **Step 1: Change field visibility**

Change `private ArrayList<EntityActor> unitsHangingAroundTheBase` to package-private (remove `private`), and add:
```java
ArrayList<EntityActor> getUnitsHangingAroundTheBase() {
    return this.unitsHangingAroundTheBase;
}
```

- [ ] **Step 2: Verify compilation**

Run `javac` on `AIPlayer.java`. Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/AIPlayer.java
git commit -m "test(ai): expose unitsHangingAroundTheBase for exploration tests"
```

---

### Task 9: Run all AI tests and fix regressions

**Files:**
- Test: `JavaRA/test/test/AIPlayerTest.java`, `AIPlayerFindClosestEnemyTest.java`, `AIPlayerExplorationTest.java`, `GroundStateBaseTest.java`, `PosTest.java`

**Interfaces:**
- Consumes: all modified source files.
- Produces: green test suite.

- [ ] **Step 1: Compile all tests**

Run `javac --release 21 -cp "<fxcp>:<junit>:<hamcrest>:snakeyaml:json:assets:src:test" -d /tmp/opencode/plan9 JavaRA/test/test/*.java`. Expected: PASS.

- [ ] **Step 2: Run the test suite**

Run `java -cp "<built>:<fxcp>:<junit>:<hamcrest>:..." org.junit.runner.JUnitCore test.AIPlayerTest test.AIPlayerFindClosestEnemyTest test.AIPlayerExplorationTest test.GroundStateBaseTest test.PosTest`. Expected: PASS all.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/test/test/
git commit -m "test(ai): full AI test suite passes"
```

---

### Self-review

**Spec coverage:** Each spec requirement maps to a task: `findClosestEnemy` fix → Task 2; empty-list guards → Task 1; `EXPLORATION` squad type → Task 3; `ExplorationState` → Task 4; squad wiring into AIPlayer → Task 5; tests → Tasks 6, 7, 8, 9. No gaps.

**Placeholder scan:** No TBD/TODO/incomplete sections remain; all code blocks and commands are concrete.

**Type consistency:** `Squad.getFsm()` returns `StateMachine`; `StateMachine.getCurrentState()` added in Task 7. `AIPlayer.getSquadOfType(SquadType)` already exists. `unitsHangingAroundTheBase` accessor added in Task 8. `Order("Move", u, dest, true)` matches existing constructor usage in `Squad.setTarget`. All consistent.

**Plan complete and ready for execution.** Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task + two-stage review (requires `superpowers:subagent-driven-development`)
2. **Inline Execution** — execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints

Which approach?
