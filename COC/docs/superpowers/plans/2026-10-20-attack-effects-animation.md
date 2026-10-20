# Attack Effects Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve attack-effect readability (projectiles, impact flashes, tower beams, building hit feedback) using only JavaFX shapes/colors.

**Architecture:** Add a small `AttackEffects` helper for reusable flashes and pulses, then call it from existing attack/defense controllers at impact points. Keep the current threading model and `PathTransition`-based motion; only add visual feedback and cleanup stale nodes.

**Tech Stack:** Java, JavaFX (`PathTransition`, `FadeTransition`, `ScaleTransition`, `Platform.runLater`), Ant.

## Global Constraints

- No new image or audio files.
- Do not modify combat algorithms or unit stats.
- All scene-graph mutations on the JavaFX Application Thread.
- Reuse existing controllers; keep changes minimal.

---

### Task 1: Create `AttackEffects` helper

**Files:**
- Create: `COC/src/controller/attack/AttackEffects.java`

**Interfaces:**
- Produces: `static void flashImpact(AnchorPane root, double x, double y, Color color)` and `static void pulse(ImageView view)`.

- [ ] **Step 1: Write the helper**

```java
package controller.attack;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public final class AttackEffects {
    private AttackEffects() {}

    public static void flashImpact(AnchorPane root, double x, double y, Color color) {
        Circle flash = new Circle(x, y, 6);
        flash.setFill(color);
        flash.setOpacity(0.9);
        root.getChildren().add(flash);
        FadeTransition fade = new FadeTransition(Duration.millis(250), flash);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> root.getChildren().remove(flash));
        fade.play();
    }

    public static void pulse(ImageView view) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), view);
        st.setToX(1.12);
        st.setToY(1.12);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}
```

- [ ] **Step 2: Compile**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add COC/src/controller/attack/AttackEffects.java
git commit -m "feat(coc): add AttackEffects helper for flashes and pulses"
```

---

### Task 2: Ranged projectile cleanup and impact flashes

**Files:**
- Modify: `COC/src/controller/attack/ArcherBalloonAttack.java`
- Modify: `COC/src/controller/attack/DefBalloonAttack.java`
- Modify: `COC/src/controller/attack/GoblinBalloonAttack.java`
- Modify: `COC/src/controller/attack/RangedUnitAttack.java`

**Interfaces:**
- Consumes: `AttackEffects.flashImpact`, `AttackEffects.pulse`.

- [ ] **Step 1: Add impact flash on projectile arrival**

In each ranged controller, where the projectile `PathTransition` is played, set
`setOnFinished` to spawn an impact flash at the target center:

```java
pathTransition.setOnFinished(e -> AttackEffects.flashImpact(root,
        building.getImageView().getX() + building.getImageView().getFitWidth() / 2,
        building.getImageView().getY() + building.getImageView().getFitHeight() / 2,
        Color.web("#B442F7")));
```

Use distinct colors per unit (archer: `#B442F7`, balloon: `#FFD700`, wizard: `#00BFFF`).

- [ ] **Step 2: Remove projectile nodes on completion**

Ensure each projectile `Circle` is removed in `setOnFinished` (or immediately after
the wait loop) so no orphaned nodes remain.

- [ ] **Step 3: Add building pulse on damage**

When `building.setHp(...)` is called, also call:

```java
Platform.runLater(() -> AttackEffects.pulse(building.getImageView()));
```

- [ ] **Step 4: Compile and commit**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`

```bash
git add COC/src/controller/attack/*.java
git commit -m "feat(coc): add ranged impact flashes and building pulses"
```

---

### Task 3: Melee impact flashes

**Files:**
- Modify: `COC/src/controller/attack/MeleeUnitAttack.java`
- Modify: `COC/src/controller/attack/PandaAttack.java`
- Modify: `COC/src/controller/attack/DragonAttack.java`

**Interfaces:**
- Consumes: `AttackEffects.flashImpact`, `AttackEffects.pulse`.

- [ ] **Step 1: Flash on melee strike**

In each melee controller's `attack()` method, before/when applying damage, spawn an
impact flash at the building's center:

```java
Platform.runLater(() -> AttackEffects.flashImpact(root,
        building.getImageView().getX() + building.getImageView().getFitWidth() / 2,
        building.getImageView().getY() + building.getImageView().getFitHeight() / 2,
        Color.web("#FF4500")));
```

- [ ] **Step 2: Pulse building on damage**

Call `AttackEffects.pulse(building.getImageView())` on the FX thread when HP
decreases.

- [ ] **Step 3: Compile and commit**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`

```bash
git add COC/src/controller/attack/*.java
git commit -m "feat(coc): add melee impact flashes and building pulses"
```

---

### Task 4: Tower beam cleanup and flashes

**Files:**
- Modify: `COC/src/controller/defense/TeslaDefense.java`
- Modify: `COC/src/controller/defense/ArcherTowerDefense.java`
- Modify: `COC/src/controller/defense/InfernoTowerDefense.java`

**Interfaces:**
- Consumes: `AttackEffects.flashImpact`.

- [ ] **Step 1: Remove stale beam/impact nodes**

Before creating a new beam/impact node, remove any previous one added by this
controller (track the node in a field).

- [ ] **Step 2: Recompute endpoints each shot**

Ensure beam endpoints use the target's current `localToScene` center.

- [ ] **Step 3: Add impact flash on hit**

Spawn a flash at the hero's center when damage is applied, color-coded per tower.

- [ ] **Step 4: Compile and commit**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`

```bash
git add COC/src/controller/defense/*.java
git commit -m "feat(coc): clean up tower beams and add impact flashes"
```

---

### Task 5: End-to-end verification

- [ ] **Step 1: Full rebuild**

Run: `ant jar`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Manual gameplay check**

1. Ranged units fire projectiles that land and disappear; impact flashes appear.
2. Melee strikes flash at building centers.
3. Towers beam correctly with no stale nodes.
4. Damaged buildings pulse briefly.
5. No regressions in win/lose flow.

- [ ] **Step 3: Record status**

Run: `git status --short`
Expected: only intended files modified.