# AI Player Combat Engagement — Design

Date: 2026-10-21
Project: JavaRA (cr0s.javara.ai)
Scope: Make AI squads actually engage enemies. Approach A: minimal engagement fix.

## Problem

AI squads never attack. `Squad.update()` calls a dormant `StateMachine` whose `currentState` is never set, so it is a no-op. `Squad.setTarget()` queues `Move` + `Follow` activities but never an `Attack`. Individual units with `AutoTarget` only engage when idle, but squad members are busy moving, so they never fire. Vehicles additionally ignore `"Attack"` orders because `EntityVehicle.resolveOrder` does not delegate to their attack strategy (only infantry does).

## Goal

Ensure that when an AI squad is given a target, every member moves into range and attacks it, retargets when the target dies, and vehicles respond to `"Attack"` orders like infantry.

## Changes

### 1. `Squad.setTarget(EntityActor tgt)`
Replace Move/Follow queueing with an `"Attack"` order per unit:
```java
this.target = new Target(tgt);
for (EntityActor u : this.units) {
    if (u instanceof ICanAttack) {
        u.resolveOrder(new Order("Attack", u, null, tgt));
    }
}
```
Requires import of `ICanAttack` and `Order`.

### 2. `EntityVehicle.resolveOrder(Order)`
Delegate `"Attack"` orders to the attack strategy when the vehicle is an `ICanAttack`:
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
Requires imports of `ICanAttack`, `Order`.

### 3. `Squad.update()`
Replace the dormant FSM call with a direct engagement loop:
- If squad invalid, return.
- If target invalid/dead, find closest enemy via `bot.findClosestEnemy(units.get(0).getPosition())`; if found, `setTarget(first)`; return.
- Otherwise re-issue `"Attack"` to any idle member (so units that finished a move re-engage).

## Out of scope

- Formation/cohesion stubs (`adoptFormation`, `maintainCohesion`).
- Focus-fire priority, retreat, terrain usage.
- State machine wiring (remains dormant).

## Testing

Compile-only verification (no AI tests). Manual smoke: AI squads move to and attack enemy units/buildings.