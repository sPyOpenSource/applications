# AI Player Combat Engagement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make AI squads actually engage enemies by issuing Attack orders, delegating Attack to vehicles, and retargeting.

**Architecture:** Three localized edits: Squad.setTarget and Squad.update in Squad.java; EntityVehicle.resolveOrder in EntityVehicle.java. No rearchitecture.

**Tech Stack:** Java 21, JavaFX.

## Global Constraints

- Preserve public method signatures.
- Import ICanAttack, Order where needed.
- Compile with JDK 21 release.

---

### Task 1: Squad.setTarget issues Attack orders

**Files:** Modify `JavaRA/src/cr0s/javara/ai/Squad.java`

- [ ] Step 1: Add imports for `ICanAttack` and `Order`.
- [ ] Step 2: Replace setTarget body:
```java
    public void setTarget(EntityActor tgt) {
	this.target = new Target(tgt);
	for (EntityActor u : this.units) {
	    if (u instanceof ICanAttack) {
		u.resolveOrder(new Order("Attack", u, null, tgt));
	    }
	}
    }
```
- [ ] Step 3: Compile & commit.

---

### Task 2: EntityVehicle delegates Attack orders

**Files:** Modify `JavaRA/src/cr0s/javara/entity/vehicle/EntityVehicle.java`

- [ ] Step 1: Add imports `ICanAttack`, `Order`.
- [ ] Step 2: Replace resolveOrder:
```java
    @Override
    public void resolveOrder(Order order) {
	if (order.orderString.equals("Attack") && this instanceof ICanAttack) {
	    ((ICanAttack) this).getAttackStrategy().resolveOrder(order);
	    return;
	}
	super.resolveOrder(order);
    }
```
- [ ] Step 3: Compile & commit.

---

### Task 3: Squad.update engagement loop

**Files:** Modify `JavaRA/src/cr0s/javara/ai/Squad.java`

- [ ] Step 1: Replace update():
```java
    public void update() {
	if (!this.isValid()) {
	    return;
	}

	if (!this.targetIsValid()) {
	    if (!this.units.isEmpty()) {
		ArrayList<EntityActor> enemies = this.bot.findClosestEnemy(this.units.get(0).getPosition());
		if (!enemies.isEmpty()) {
		    this.setTarget(enemies.get(0));
		}
	    }
	    return;
	}

	EntityActor tgt = (EntityActor) this.target.getTargetEntity();
	for (EntityActor u : this.units) {
	    if (u.isIdle() && u instanceof ICanAttack) {
		u.resolveOrder(new Order("Attack", u, null, tgt));
	    }
	}
    }
```
Need import ArrayList (already? Squad imports ArrayList yes).

- [ ] Step 2: Compile & commit.

---

### Task 4: Full compile & smoke

- [ ] Compile all; expected success. Commit.