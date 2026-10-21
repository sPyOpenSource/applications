# AI Player Bug Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix crash-causing and obviously-wrong defects in the JavaRA AI player (`AIPlayer`) per the approved spec, with minimal behavior change.

**Architecture:** Surgical edits to `AIPlayer.java` (and one line in `Squad.java`/`BaseBuilder.java`). No rearchitecture. Each task is an independent, localized edit verified by compilation.

**Tech Stack:** Java 21, JavaFX, Ant/NetBeans build. No test framework for AI; verification is compile-only + manual smoke run.

## Global Constraints

- Source/target Java 21 (`javac.source=21`, `javac.target=21`).
- Do not change public method signatures used by other classes (`findClosestEnemy`, `chooseBuildLocation`, etc.).
- Preserve existing control flow and comments except where the fix requires.
- One logical change per commit.

---

### Task 1: Guard `findClosestEnemy(Pos)` against empty enemy list

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `findClosestEnemy(final Pos center)`)

**Interfaces:**
- Consumes: `world.getPlayers()`, `Player.isEnemyFor(this)`.
- Produces: `ArrayList<EntityActor>` (empty when no enemies).

- [ ] **Step 1: Apply edit**

Replace:
```java
    public ArrayList<EntityActor> findClosestEnemy(final Pos center) {
        enemies = new ArrayList<>();
	for (Player p : world.getPlayers()) {
	    if (p.isEnemyFor(this)) {
		enemies.add(p);
	    }
	}
	return this.findClosestEnemy(center, enemies.get(0), 20000000);
    }
```
with:
```java
    public ArrayList<EntityActor> findClosestEnemy(final Pos center) {
        enemies = new ArrayList<>();
	for (Player p : world.getPlayers()) {
	    if (p.isEnemyFor(this)) {
		enemies.add(p);
	    }
	}
	if (enemies.isEmpty()) {
	    return new ArrayList<>();
	}
	return this.findClosestEnemy(center, enemies.get(0), 20000000);
    }
```

- [ ] **Step 2: Compile**

Run: `cd JavaRA && ant compile` (or javac via NetBeans). Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add JavaRA/src/cr0s/javara/ai/AIPlayer.java
git commit -m "fix(ai): guard findClosestEnemy against empty enemy list"
```

---

### Task 2: Guard `chooseEnemyTarget()` against empty enemy list

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `chooseEnemyTarget()`)

- [ ] **Step 1: Apply edit**

After building `enemies` and before sort, add:
```java
	if (enemies.isEmpty()) {
	    return new ArrayList<>();
	}
```
Place immediately after the for-loop that populates `enemies` (before `Collections.sort`).

- [ ] **Step 2: Compile & commit**

Run compile; expected success. Commit message: `fix(ai): guard chooseEnemyTarget against empty enemy list`.

---

### Task 3: Guard DEFENSIVE build placement

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `chooseBuildLocation`, `BuildingType.DEFENSIVE` branch)

- [ ] **Step 1: Apply edit**

Replace:
```java
                    ArrayList<EntityActor> closestEnemy = this.findClosestEnemy(this.defenseCenter);
                    Pos targetCell = (closestEnemy.isEmpty()) ? this.getPlayerSpawnPoint() : closestEnemy.get(0).getCellPosition();
```
with same (already guarded) — verify present. If the single-arg call site uses `.get(0)` directly, fix to:
```java
                    ArrayList<EntityActor> closestEnemy = this.findClosestEnemy(this.defenseCenter);
                    Pos targetCell = this.getPlayerSpawnPoint();
                    if (!closestEnemy.isEmpty()) {
                        targetCell = closestEnemy.get(0).getCellPosition();
                    }
```

- [ ] **Step 2: Compile & commit**

---

### Task 4: Fix `findEnemyConstructionYards` ownership test

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `findEnemyConstructionYards`)

- [ ] **Step 1: Apply edit**

Replace condition:
```java
	    if (e.isDead() || !(e instanceof EntityConstructionYard) || (e.owner.isEnemyFor(this))) {
```
with:
```java
	    if (e.isDead() || !(e instanceof EntityConstructionYard) || !e.owner.isEnemyFor(this)) {
```

- [ ] **Step 2: Compile & commit**

Commit message: `fix(ai): collect enemy-owned construction yards`.

---

### Task 5: Filter by ownership in `findNewUnits`

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `findNewUnits`)

- [ ] **Step 1: Apply edit**

In the loop, after `EntityActor a = (EntityActor) e;`, add:
```java
	    if (a.owner != this) {
		continue;
	    }
```
before `if (!this.activeUnits.contains(a))`.

- [ ] **Step 2: Compile & commit**

---

### Task 6: Avoid registering empty assault squads

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `createAttackForce`)

- [ ] **Step 1: Apply edit**

Replace:
```java
    private void createAttackForce() {
	int randomSquadSize = this.squadSize + World.random.nextInt(4);

	if (this.unitsHangingAroundTheBase.size() >= randomSquadSize) {
	    Squad attackForce = this.registerNewSquad(SquadType.ASSAULT);

	    for (EntityActor a : this.unitsHangingAroundTheBase) {
		if (a instanceof ICanAttack) { // TODO: check is not aircraft
		    attackForce.addUnit(a);
		}
	    }

	    this.unitsHangingAroundTheBase.clear();
	}
    }
```
with:
```java
    private void createAttackForce() {
	int randomSquadSize = this.squadSize + World.random.nextInt(4);

	if (this.unitsHangingAroundTheBase.size() >= randomSquadSize) {
	    ArrayList<EntityActor> attackUnits = new ArrayList<>();
	    for (EntityActor a : this.unitsHangingAroundTheBase) {
		if (a instanceof ICanAttack) { // TODO: check is not aircraft
		    attackUnits.add(a);
		}
	    }

	    if (!attackUnits.isEmpty()) {
		Squad attackForce = this.registerNewSquad(SquadType.ASSAULT);
		for (EntityActor a : attackUnits) {
		    attackForce.addUnit(a);
		}
		this.unitsHangingAroundTheBase.clear();
	    }
	}
    }
```

- [ ] **Step 2: Compile & commit**

---

### Task 7: Guard `tryToRushAttack` against empty squads

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `tryToRushAttack`)

- [ ] **Step 1: Apply edit**

Replace:
```java
    private void tryToRushAttack() {
        ArrayList<EntityActor> a = chooseEnemyTarget();
        if(a.isEmpty()) return;
        if (squads.isEmpty()) return;
        Squad s = squads.get(0);
        s.setTarget(a.get(0));
```
with:
```java
    private void tryToRushAttack() {
        ArrayList<EntityActor> a = chooseEnemyTarget();
        if (a.isEmpty()) return;
        if (squads.isEmpty()) return;

        Squad s = null;
        for (Squad sq : squads) {
            if (sq.getType() == SquadType.ASSAULT) {
                s = sq;
                break;
            }
        }
        if (s == null) {
            s = squads.get(0);
        }
        s.setTarget(a.get(0));
```

- [ ] **Step 2: Compile & commit**

---

### Task 8: Fix unit common names assignment in `loadAIRules`

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `loadAIRules`)

- [ ] **Step 1: Apply edit**

In the "Load units common names" loop, change:
```java
		this.buildingCommonNames.put((String) e.getKey(), names);
```
to:
```java
		this.unitCommonNames.put((String) e.getKey(), names);
```

- [ ] **Step 2: Compile & commit**

---

### Task 9: Restore `thereIsNoMcv` ownership check

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `thereIsNoMcv`)

- [ ] **Step 1: Apply edit**

Replace:
```java
    private boolean thereIsNoMcv() {
	/*for (Entity e : GUI.getInstance().getWorld().getEntitiesList()) {
	    if (e.isDead() || !(e instanceof EntityMcv)) {
		continue;
	    }

	    EntityMcv mcv = (EntityMcv) e;

	    // It's our
	    if (mcv.owner == this) {
		return false;
	    }
	}*/

	// No any our MCV is found
	return true;
    }
```
with:
```java
    private boolean thereIsNoMcv() {
	for (Entity e : GUI.getInstance().getWorld().getEntitiesList()) {
	    if (e.isDead() || !(e instanceof EntityMcv)) {
		continue;
	    }

	    EntityMcv mcv = (EntityMcv) e;

	    // It's our
	    if (mcv.owner == this) {
		return false;
	    }
	}

	// No any our MCV is found
	return true;
    }
```

- [ ] **Step 2: Compile & commit**

---

### Task 10: Add neutral vehicles to buildables

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/AIPlayer.java` (method `buidUnits`, Vehicles case)

- [ ] **Step 1: Apply edit**

In the neutralVehicles branch:
```java
		} else if ((this.getBase().isAlliedWarFactoryPresent || this.getBase().isSovietWarFactoryPresent)
			&& this.getBase().getProductionQueue().neutralVehicles.containsKey(name)) {

		}
```
add:
```java
		    buildables.add(name);
```
inside the empty block.

- [ ] **Step 2: Compile & commit**

---

### Task 11: Make `Squad.isValid` treat all-dead squads as invalid

**Files:**
- Modify: `JavaRA/src/cr0s/javara/ai/Squad.java` (method `isValid`)

- [ ] **Step 1: Apply edit**

Replace:
```java
    public boolean isValid() {
	return !this.units.isEmpty();
    }
```
with:
```java
    public boolean isValid() {
	return !this.units.isEmpty() && this.units.stream().anyMatch(u -> !u.isDead());
    }
```

- [ ] **Step 2: Compile & commit**

---

### Task 12: Final compile & smoke check

**Files:** none new.

- [ ] **Step 1: Full compile**

Run: `cd JavaRA && ant clean compile`. Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 2: Manual smoke run (optional)**

Run the game, start a skirmish with an AI opponent, observe console for exceptions and that AI builds/harvests/attacks. Not required for merge if compile passes.

- [ ] **Step 3: Final commit (if any leftover)**

```bash
git status
```
Expected: clean working tree.