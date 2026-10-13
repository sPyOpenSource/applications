# Two New Playable Troops (Barbarian, Wizard) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a melee Barbarian and a ranged Wizard as deployable troops to the Clash of Clans JavaFX game, reusing existing JavaRA sprites, by extracting shared attack-controller base classes and driving the `Battle` drag bar through a small `UnitIcon` helper.

**Architecture:** Two new JavaRA infantry entities (`Barbarian` uses `e1.shp`, `Wizard` uses `e3.shp`) that render their stand frame into `getImages()`. Three new COC controller classes factor the copy-pasted walk/attack threading skeleton: `UnitAttack` (shared base), `MeleeUnitAttack`, `RangedUnitAttack`. Two thin subclasses (`BarbarianAttack`, `WizardAttack`) supply stats/audio. `Battle.java` gains a `UnitIcon` list so all seven drag icons (five existing + two new) are wired data-driven instead of hand-written.

**Tech Stack:** Java, JavaFX (`PathTransition`, `Platform.runLater`, `MediaPlayer`), Ant (`nbproject/build-impl.xml`), the `assets` module (sprites/audio), the `JavaRA` library (`cr0s.javara.entity.infantry`, `ResourceManager`, `Sequence`).

## Global Constraints

- No new image or audio files anywhere; all sprites come from the RA archives (`e1.shp`, `e3.shp`) and all audio comes from existing `assets/audio/*.mp3` files.
- Do not modify any existing `controller/attack/*`, `controller/defense/*`, `CheckResultAttack`, `Panda`, `Dragon`, `GoblinBalloon`, `DefBalloon`, or `ArcherBalloon`.
- New entity classes live in `JavaRA/src/cr0s/javara/entity/infantry/`; new controllers and `UnitIcon` live under `COC/src/`.
- The app must remain runnable after every task (`ant compile` must pass; the final task requires `ant run`).
- Levels: Barbarian min level 1, Wizard min level 2. Costs: Barbarian 5, Wizard 20.
- `cr0s.javara.entity.infantry` is already exported by JavaRA's `module-info.java`; no module changes are needed.
- File paths are relative to the repo root `/Users/xuyi/Source/OS/armOS/lib/jcore/test` unless stated otherwise.

---
## Task 1: JavaRA entities `Barbarian` and `Wizard`

**Files:**
- Create: `JavaRA/src/cr0s/javara/entity/infantry/Barbarian.java`
- Create: `JavaRA/src/cr0s/javara/entity/infantry/Wizard.java`
- Build: `JavaRA/dist/JavaRA.jar` (rebuilt)

**Interfaces:**
- Produces: `Barbarian(Pos)` and `Wizard(Pos)` — both `extends EntityInfantry implements IHaveCost`. After construction, `getImages().get(0)` returns their rendered stand frame, `getImageView()` is set and positioned, and `getMoveSpeed()`, `getHp()`, `getDamagePerSecond()`, `setDead()`, `isDead()` all work. COC controllers depend on these exact methods (identical to how `EntityGrenadeTrooper` behaves today).

- [ ] **Step 1: Create `Barbarian`**

Write `JavaRA/src/cr0s/javara/entity/infantry/Barbarian.java`:

```java
package cr0s.javara.entity.infantry;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;

public class Barbarian extends EntityInfantry implements IHaveCost {
    public Barbarian(Pos pos) {
        super(pos);
        setHp(1200);
        setDamagePerSecond(150);
        this.texture = ResourceManager.getInstance().getInfantryTexture("e1.shp");
        this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
        Image image = standSequence.render();
        setImageView(new ImageView(image));
        getImageView().setX(pos.getX());
        getImageView().setY(pos.getY());
        getImages().add(image);
    }

    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal) {
        return null;
    }

    @Override
    public float getMoveSpeed() {
        return 30;
    }

    @Override
    public int getMinimumEnoughRange() {
        return 0;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
        return true;
    }

    @Override
    public int getWaitAverageTime() {
        return 0;
    }

    @Override
    public int getWaitSpreadTime() {
        return 0;
    }

    @Override
    public int getRevealingRange() {
        return 0;
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public int getBuildingCost() {
        return 5;
    }
}
```

- [ ] **Step 2: Create `Wizard`**

Write `JavaRA/src/cr0s/javara/entity/infantry/Wizard.java` — identical to `Barbarian`, with the `e3.shp` texture, HP 500, DPS 300, move speed 20, cost 20:

```java
package cr0s.javara.entity.infantry;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;

public class Wizard extends EntityInfantry implements IHaveCost {
    public Wizard(Pos pos) {
        super(pos);
        setHp(500);
        setDamagePerSecond(300);
        this.texture = ResourceManager.getInstance().getInfantryTexture("e3.shp");
        this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
        Image image = standSequence.render();
        setImageView(new ImageView(image));
        getImageView().setX(pos.getX());
        getImageView().setY(pos.getY());
        getImages().add(image);
    }

    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal) {
        return null;
    }

    @Override
    public float getMoveSpeed() {
        return 20;
    }

    @Override
    public int getMinimumEnoughRange() {
        return 0;
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
        return true;
    }

    @Override
    public int getWaitAverageTime() {
        return 0;
    }

    @Override
    public int getWaitSpreadTime() {
        return 0;
    }

    @Override
    public int getRevealingRange() {
        return 0;
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public int getBuildingCost() {
        return 20;
    }
}
```

- [ ] **Step 3: Rebuild JavaRA and verify the classes land in the jar**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/JavaRA && ant jar
unzip -l dist/JavaRA.jar | grep -E "Barbarian|Wizard"
```

Expected: build succeeds (`BUILD SUCCESSFUL`) and the `unzip` output lists `cr0s/javara/entity/infantry/Barbarian.class` and `cr0s/javara/entity/infantry/Wizard.class`.

- [ ] **Step 4: Commit**

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git add JavaRA/src/cr0s/javara/entity/infantry/Barbarian.java JavaRA/src/cr0s/javara/entity/infantry/Wizard.java JavaRA/dist/JavaRA.jar && git commit -m "feat(JavaRA): add Barbarian and Wizard infantry entities reusing e1/e3 shp"
```

---
## Task 2: Shared base `UnitAttack`

**Files:**
- Create: `COC/src/controller/attack/UnitAttack.java`

**Interfaces:**
- Consumes: `MobileEntity.getImages()`, `MobileEntity.getImageView()`, `MobileEntity.setImageView(ImageView)`, `MobileEntity.getMoveSpeed()`, `MobileEntity.getHp()`, `MobileEntity.isDead()`, `MobileEntity.setDead()`, `MobileEntity.getDamagePerSecond()` (from `cr0s.javara.entity.MobileEntity`); `Map.getAttackingHeroes()`, `Map.getBuildingsMap()` (from `cr0s.javara.render.map.Map`); `EntityBuilding.getImageView()`; `Assets.get(String)`.
- Produces (used by Tasks 3-4): `protected` fields `assets`, `root`, `map`, `unit`, `sprite`; `protected` methods `audio(String,double)`, `runOnFx(Runnable)`, `await()`, `await(long)`, `nearestBuilding()`, `removeDestroyedBuilding(EntityBuilding)`, `static distance(...)`, `static nearestPointOnCircle(...)`; `public final synchronized void myNotify()`; abstract hooks `moveToward()`, `attack(EntityBuilding)`, `deployAudio()`, `attackAudio()`, `dieAudio()`, `attackRange()`.

- [ ] **Step 1: Create `UnitAttack`**

Write `COC/src/controller/attack/UnitAttack.java`:

```java
package controller.attack;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import assets.Assets;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;

public abstract class UnitAttack extends Thread {
    protected final Assets assets = new Assets();
    protected final AnchorPane root;
    protected final Map map;
    protected final MobileEntity unit;
    protected final ImageView sprite;

    protected UnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        this.root = root;
        this.map = map;
        this.unit = unit;
        this.sprite = new ImageView(unit.getImages().get(0));
        unit.setImageView(new ImageView(sprite.getImage()));
        map.getAttackingHeroes().add(unit);
    }

    @Override
    public void run() {
        synchronized (this) {
            MediaPlayer deploy = audio(deployAudio(), 0.5);
            deploy.setAutoPlay(true);
            runOnFx(() -> {
                root.getChildren().add(new MediaView(deploy));
                root.getChildren().add(unit.getImageView());
                myNotify();
            });
            await();
            while (!unit.isDead()) {
                EntityBuilding building = moveToward();
                if (unit.isDead()) {
                    break;
                }
                attack(building);
            }
            MediaPlayer die = audio(dieAudio(), 0.5);
            die.setAutoPlay(true);
            runOnFx(() -> {
                root.getChildren().remove(unit.getImageView());
                root.getChildren().add(new MediaView(die));
                myNotify();
            });
            await();
            map.getAttackingHeroes().remove(unit);
        }
    }

    protected final MediaPlayer audio(String assetPath, double volume) {
        MediaPlayer player = new MediaPlayer(new Media(assets.get(assetPath)));
        player.setVolume(volume);
        return player;
    }

    protected final void runOnFx(Runnable task) {
        Platform.runLater(task);
    }

    public final synchronized void myNotify() {
        notify();
    }

    protected final void await() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected final void await(long millis) {
        try {
            wait(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected final EntityBuilding nearestBuilding() {
        EntityBuilding nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity node : map.getBuildingsMap()) {
            if (node instanceof EntityBuilding building) {
                double width = distance(unit.getImageView().getX(), unit.getImageView().getY(),
                        building.getImageView().getX(), building.getImageView().getY());
                if (width < best) {
                    best = width;
                    nearest = building;
                }
            }
        }
        return nearest;
    }

    protected final void removeDestroyedBuilding(EntityBuilding building) {
        map.getBuildingsMap().remove(building);
        runOnFx(() -> {
            root.getChildren().remove(building.getImageView());
            myNotify();
        });
        await();
    }

    protected static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    protected static double[] nearestPointOnCircle(double cx, double cy, double r, double x, double y) {
        double distanceToCenter = distance(x, y, cx, cy);
        if (distanceToCenter < r) {
            return new double[]{x, y};
        }
        double m1 = (cy - y) / (cx - x);
        double intercept = y - m1 * x;
        double a = 1 + Math.pow(m1, 2);
        double b = -2 * cx + 2 * m1 * (intercept - cy);
        double c = Math.pow(cx, 2) + Math.pow(intercept - cy, 2) - Math.pow(r, 2);
        double sqrt = Math.sqrt(Math.pow(b, 2) - 4 * a * c);
        double x1 = (-b + sqrt) / (2 * a);
        double x2 = (-b - sqrt) / (2 * a);
        double y1 = m1 * x1 + intercept;
        double y2 = m1 * x2 + intercept;
        return distance(x, y, x1, y1) < distance(x, y, x2, y2)
                ? new double[]{x1, y1} : new double[]{x2, y2};
    }

    protected abstract EntityBuilding moveToward();

    protected abstract void attack(EntityBuilding building);

    protected abstract String deployAudio();

    protected abstract String attackAudio();

    protected abstract String dieAudio();

    protected abstract double attackRange();
}
```

- [ ] **Step 2: Compile to verify**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant compile
```

Expected: `BUILD SUCCESSFUL` (the abstract class compiles alone; it references no Barbarian/Wizard yet).

- [ ] **Step 3: Commit**

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git add COC/src/controller/attack/UnitAttack.java && git commit -m "feat(coc): add shared UnitAttack base for attack controllers"
```

---
## Task 3: `MeleeUnitAttack` + `BarbarianAttack`

**Files:**
- Create: `COC/src/controller/attack/MeleeUnitAttack.java`
- Create: `COC/src/controller/attack/BarbarianAttack.java`

**Interfaces:**
- Consumes: `UnitAttack` base (Task 2); `Barbarian(Pos)` (Task 1); javafx `PathTransition`, `Duration`, `MoveTo`, `LineTo`, `Path`.
- Produces (used by Task 5): `BarbarianAttack(double x, double y, AnchorPane root, Map map)` — callable like the existing `DragonAttack`; calling `.start()` deploys the unit.

- [ ] **Step 1: Create `MeleeUnitAttack`**

Write `COC/src/controller/attack/MeleeUnitAttack.java`:

```java
package controller.attack;

import javafx.animation.PathTransition;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;

public abstract class MeleeUnitAttack extends UnitAttack {
    protected MeleeUnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        super(unit, root, map);
    }

    @Override
    protected double attackRange() {
        return 0;
    }

    @Override
    protected EntityBuilding moveToward() {
        EntityBuilding building = nearestBuilding();
        if (building == null) {
            unit.setDead();
            return null;
        }
        MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY());
        LineTo lineTo = new LineTo(building.getImageView().getX(),
                building.getImageView().getY() + (building.getImageView().getFitHeight() / 3));
        Path path = new Path();
        path.getElements().addAll(moveTo, lineTo);
        PathTransition transition = new PathTransition();
        double width = distance(unit.getImageView().getX(), unit.getImageView().getY(),
                building.getImageView().getX(), building.getImageView().getY());
        transition.setDuration(Duration.millis((width / unit.getMoveSpeed()) * 400));
        transition.setCycleCount(1);
        transition.setNode(unit.getImageView());
        transition.setAutoReverse(false);
        transition.setPath(path);
        runOnFx(transition::play);
        long start = System.currentTimeMillis();
        long elapsed = 0;
        while (elapsed < (long) ((width / unit.getMoveSpeed()) * 300)) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            await(300);
            elapsed = System.currentTimeMillis() - start;
        }
        return building;
    }

    @Override
    protected void attack(EntityBuilding building) {
        MediaPlayer player = audio(attackAudio(), 0.4);
        while (building.getHp() >= 0) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            runOnFx(player::play);
            building.setHp((int) (building.getHp() - (unit.getDamagePerSecond() * 1.25)));
            await(1250);
            runOnFx(player::stop);
        }
        if (!unit.isDead()) {
            removeDestroyedBuilding(building);
        }
    }
}
```

- [ ] **Step 2: Create `BarbarianAttack`**

Write `COC/src/controller/attack/BarbarianAttack.java`:

```java
package controller.attack;

import cr0s.javara.entity.infantry.Barbarian;
import cr0s.javara.render.map.Map;
import cr0s.javara.util.Pos;
import javafx.scene.layout.AnchorPane;

public class BarbarianAttack extends MeleeUnitAttack {
    public BarbarianAttack(double x, double y, AnchorPane root, Map map) {
        super(new Barbarian(new Pos(x, y)), root, map);
    }

    @Override
    protected String deployAudio() {
        return "/assets/audio/enter_panda.mp3";
    }

    @Override
    protected String attackAudio() {
        return "/assets/audio/exp1.mp3";
    }

    @Override
    protected String dieAudio() {
        return "/assets/audio/exp.mp3";
    }
}
```

- [ ] **Step 3: Compile to verify**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant compile
```

Expected: `BUILD SUCCESSFUL`. If javac reports an undefined method on the entity, re-run Task 1 Step 3 (the JavaRA jar may be stale).

- [ ] **Step 4: Commit**

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git add COC/src/controller/attack/MeleeUnitAttack.java COC/src/controller/attack/BarbarianAttack.java && git commit -m "feat(coc): add melee controller base and BarbarianAttack"
```

---
## Task 4: `RangedUnitAttack` + `WizardAttack`

**Files:**
- Create: `COC/src/controller/attack/RangedUnitAttack.java`
- Create: `COC/src/controller/attack/WizardAttack.java`

**Interfaces:**
- Consumes: `UnitAttack` base (Task 2); `Wizard(Pos)` (Task 1); javafx `PathTransition`, `Duration`, `MoveTo`, `LineTo`, `Path`, `Circle`, `Color`.
- Produces (used by Task 5): `WizardAttack(double x, double y, AnchorPane root, Map map)` — `.start()` deploys the unit. `attackRange()` is 150.

- [ ] **Step 1: Create `RangedUnitAttack`**

Write `COC/src/controller/attack/RangedUnitAttack.java`. This port keeps `ArcherBalloonAttack`'s geometry and pacing but replaces `viewBalloon` with `unit.getImageView()` and `archerBalloon.getRang()` with `attackRange()`:

```java
package controller.attack;

import javafx.animation.PathTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.map.Map;
import javafx.scene.layout.AnchorPane;

public abstract class RangedUnitAttack extends UnitAttack {
    private boolean insideRadius;
    private double widthLowe;
    private double[] nearestPointLineTo;

    protected RangedUnitAttack(MobileEntity unit, AnchorPane root, Map map) {
        super(unit, root, map);
        this.nearestPointLineTo = new double[2];
    }

    private double centerX() {
        return unit.getImageView().getX() + (unit.getImageView().getFitWidth() / 2);
    }

    private double centerY() {
        return unit.getImageView().getY() + (unit.getImageView().getFitHeight() / 2);
    }

    @Override
    protected EntityBuilding moveToward() {
        widthLowe = Double.MAX_VALUE;
        EntityBuilding building = null;
        for (Entity node : map.getBuildingsMap()) {
            if (node instanceof EntityBuilding entityBuilding) {
                double width = distance(centerX(), centerY(),
                        entityBuilding.getImageView().getX() + (entityBuilding.getImageView().getFitWidth() / 2),
                        entityBuilding.getImageView().getY() + (entityBuilding.getImageView().getFitHeight() / 2));
                if (width < widthLowe) {
                    widthLowe = width;
                    building = entityBuilding;
                    insideRadius = true;
                }
            }
        }
        if (attackRange() < widthLowe && building != null) {
            for (Entity node : map.getBuildingsMap()) {
                if (node instanceof EntityBuilding entityBuilding) {
                    double[] nearestPoint = nearestPointOnCircle(
                            entityBuilding.getImageView().getX() + (entityBuilding.getImageView().getFitWidth() / 2),
                            entityBuilding.getImageView().getY() + (entityBuilding.getImageView().getFitHeight() / 2),
                            attackRange(), centerX(), centerY());
                    double width = distance(centerX(), centerY(), nearestPoint[0], nearestPoint[1]);
                    if (width < widthLowe) {
                        widthLowe = width;
                        building = entityBuilding;
                        nearestPointLineTo = nearestPoint;
                        insideRadius = false;
                    }
                }
            }
            MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY());
            LineTo lineTo = new LineTo(nearestPointLineTo[0], nearestPointLineTo[1]);
            Path path = new Path();
            path.getElements().addAll(moveTo, lineTo);
            PathTransition transition = new PathTransition();
            transition.setDuration(Duration.millis((widthLowe / unit.getMoveSpeed()) * 400));
            transition.setCycleCount(1);
            transition.setNode(unit.getImageView());
            transition.setAutoReverse(false);
            transition.setPath(path);
            runOnFx(transition::play);
            long start = System.currentTimeMillis();
            long elapsed = 0;
            while (elapsed < (long) ((widthLowe / unit.getMoveSpeed()) * 300)) {
                if (unit.getHp() <= 0) {
                    unit.setDead();
                    break;
                }
                await(300);
                elapsed = System.currentTimeMillis() - start;
            }
        } else if (building == null) {
            unit.setDead();
        }
        return building;
    }

    @Override
    protected void attack(EntityBuilding building) {
        if (!insideRadius) {
            runOnFx(() -> {
                unit.getImageView().setX(nearestPointLineTo[0]);
                unit.getImageView().setY(nearestPointLineTo[1]);
                myNotify();
            });
            await();
        }
        Path path = new Path();
        MoveTo moveTo = new MoveTo(unit.getImageView().getX(), unit.getImageView().getY() + 20);
        LineTo lineTo = new LineTo(building.getImageView().getX() + (building.getImageView().getFitWidth() / 2),
                building.getImageView().getY() + 20);
        Circle circle = new Circle(unit.getImageView().getX() + 28.8, unit.getImageView().getY() + 60, 3);
        circle.setFill(Color.web("#B442F7"));
        path.getElements().addAll(moveTo, lineTo);
        runOnFx(() -> {
            root.getChildren().add(circle);
            myNotify();
        });
        await();
        while (building.getHp() >= 0) {
            if (unit.getHp() <= 0) {
                unit.setDead();
                break;
            }
            PathTransition pathTransition = new PathTransition();
            pathTransition.setDuration(Duration.millis(1000));
            pathTransition.setPath(path);
            pathTransition.setCycleCount(1);
            pathTransition.setNode(circle);
            runOnFx(pathTransition::play);
            building.setHp(building.getHp() - unit.getDamagePerSecond());
            await(1000);
        }
        runOnFx(() -> {
            root.getChildren().remove(circle);
            myNotify();
        });
        await();
        if (!unit.isDead()) {
            map.getBuildingsMap().remove(building);
            runOnFx(() -> {
                root.getChildren().remove(building.getImages().get(0));
                myNotify();
            });
            await();
        }
    }
}
```

- [ ] **Step 2: Create `WizardAttack`**

Write `COC/src/controller/attack/WizardAttack.java`:

```java
package controller.attack;

import cr0s.javara.entity.infantry.Wizard;
import cr0s.javara.render.map.Map;
import cr0s.javara.util.Pos;
import javafx.scene.layout.AnchorPane;

public class WizardAttack extends RangedUnitAttack {
    public WizardAttack(double x, double y, AnchorPane root, Map map) {
        super(new Wizard(new Pos(x, y)), root, map);
    }

    @Override
    protected String deployAudio() {
        return "/assets/audio/balloonDeploy.mp3";
    }

    @Override
    protected String attackAudio() {
        return "/assets/audio/fire.mp3";
    }

    @Override
    protected String dieAudio() {
        return "/assets/audio/balloonDie.mp3";
    }

    @Override
    protected double attackRange() {
        return 150;
    }
}
```

- [ ] **Step 3: Compile to verify**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant compile
```

Expected: `BUILD SUCCESSFUL`. The removal uses `building.getImages().get(0)`, matching the existing `ArcherBalloonAttack`.

- [ ] **Step 4: Commit**

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git add COC/src/controller/attack/RangedUnitAttack.java COC/src/controller/attack/WizardAttack.java && git commit -m "feat(coc): add ranged controller base and WizardAttack"
```

---
## Task 5: `UnitIcon` and `Battle.java` integration

**Files:**
- Create: `COC/src/view/UnitIcon.java`
- Modify: `COC/src/view/Battle.java` (replace the hand-wired icon section in `attack()`, lines 81-236)

**Interfaces:**
- Consumes: `UnitIcon` (this task); all seven `*Attack` constructors `(double x, double y, AnchorPane root, Map map)`; `Barbarian(Pos)` and `Wizard(Pos)` for sprite icons; existing `assets/png/*_drag.png` icons; `attackingPlayer.getLevel()`.
- Produces: nothing new for later tasks.

- [ ] **Step 1: Create `UnitIcon`**

Write `COC/src/view/UnitIcon.java`:

```java
package view;

import java.util.function.BiFunction;
import javafx.scene.image.ImageView;

public class UnitIcon {
    public final ImageView view;
    public final int cost;
    public final int minLevel;
    public final BiFunction<Double, Double, Thread> spawn;

    public UnitIcon(ImageView view, int cost, int minLevel, BiFunction<Double, Double, Thread> spawn) {
        this.view = view;
        this.cost = cost;
        this.minLevel = minLevel;
        this.spawn = spawn;
    }
}
```

- [ ] **Step 2: Rewrite `Battle.java`**

Add these imports to `COC/src/view/Battle.java`:

```java
import cr0s.javara.entity.infantry.Barbarian;
import cr0s.javara.entity.infantry.Wizard;
import cr0s.javara.util.Pos;
import java.util.function.BiFunction;
```

Add a field next to `capacityInt`:

```java
private final ArrayList<UnitIcon> icons = new ArrayList<>();
```

Replace the whole body of `attack()` (currently lines 78-236) and add the three private helpers. The new `attack()`:

```java
private AnchorPane attack(){
    AnchorPane root = defensivePlayer.getMap().getMapView();

    Rectangle rectangle = new Rectangle(0, 635, 1000, 737-635);
    rectangle.setFill(Color.rgb(0,0,0,0.4));

    Text capacity = new Text("Capacity : " + capacityInt.get());
    capacity.setId("text");
    capacity.setX(810);
    capacity.setY(50);

    ImageView imageViewCapacity = new ImageView("assets/png/capacity.png");
    imageViewCapacity.setX(800);
    imageViewCapacity.setY(20);
    imageViewCapacity.setFitWidth(150);
    imageViewCapacity.setFitHeight(50);

    installIcon("assets/png/dragon_drag.png", 20, 10, 1,
            (x, y) -> new DragonAttack(x, y, root, defensivePlayer.getMap()));
    installIcon("assets/png/panda_drag.png", 110, 10, 1,
            (x, y) -> new PandaAttack(x, y, root, defensivePlayer.getMap()));
    installIcon("assets/png/balloon_drag.png", 200, 10, 2,
            (x, y) -> new DefBalloonAttack(x, y, root, defensivePlayer.getMap()));
    installIcon("assets/png/goblin_drag.png", 290, 2, 3,
            (x, y) -> new GoblinBalloonAttack(x, y, root, defensivePlayer.getMap()));
    installIcon("assets/png/archer_drag.png", 380, 3, 4,
            (x, y) -> new ArcherBalloonAttack(x, y, root, defensivePlayer.getMap()));
    installIcon(new Barbarian(new Pos(0, 0)).getImages().get(0), 470, 5, 1,
            (x, y) -> new BarbarianAttack(x, y, root, defensivePlayer.getMap()));
    installIcon(new Wizard(new Pos(0, 0)).getImages().get(0), 560, 20, 2,
            (x, y) -> new WizardAttack(x, y, root, defensivePlayer.getMap()));

    for (UnitIcon icon : icons) {
        if (attackingPlayer.getLevel() >= icon.minLevel) {
            root.getChildren().add(icon.view);
        }
    }
    icons.get(0).view.setEffect(shadow);

    root.getChildren().addAll(rectangle, imageViewCapacity, capacity);
    root.setOnMouseClicked(mouseEvent -> deploy(mouseEvent.getX(), mouseEvent.getY(), root, capacity));
    return root;
}

private void deploy(double x, double y, AnchorPane root, Text capacity) {
    if (y >= 575) {
        return;
    }
    for (UnitIcon icon : icons) {
        if (icon.view.getEffect() != null
                && capacityInt.get() >= icon.cost
                && root.getChildren().contains(icon.view)) {
            capacityInt.addAndGet(-icon.cost);
            capacity.setText("Capacity : " + capacityInt.get());
            icon.spawn.apply(x, y).start();
        }
    }
    refreshBar(root);
}

private void refreshBar(AnchorPane root) {
    for (UnitIcon icon : icons) {
        if (capacityInt.get() < icon.cost && root.getChildren().contains(icon.view)) {
            root.getChildren().remove(icon.view);
        }
    }
}

private UnitIcon installIcon(String imagePath, double x, int cost, int minLevel,
        BiFunction<Double, Double, Thread> spawn) {
    return installIcon(new ImageView(imagePath), x, cost, minLevel, spawn);
}

private UnitIcon installIcon(Image image, double x, int cost, int minLevel,
        BiFunction<Double, Double, Thread> spawn) {
    return installIcon(new ImageView(image), x, cost, minLevel, spawn);
}

private UnitIcon installIcon(ImageView view, double x, int cost, int minLevel,
        BiFunction<Double, Double, Thread> spawn) {
    UnitIcon icon = new UnitIcon(view, cost, minLevel, spawn);
    icon.view.setX(x);
    icon.view.setY(642);
    icon.view.setFitWidth(80);
    icon.view.setFitHeight(85);
    icon.view.setOnMouseClicked(mouseEvent -> {
        for (UnitIcon other : icons) {
            other.view.setEffect(null);
        }
        icon.view.setEffect(shadow);
    });
    icons.add(icon);
    return icon;
}
```

Note: `attack()` currently ends at line 236 with `return root;` and the outer class closes after it. Do not duplicate `start(Stage)` or `attack()`; the original `switch (attackingPlayer.getLevel())` block and all five `imageViewX.setOnMouseClicked` blocks are deleted by this replacement. The `new Thread(){...}` TileMap block at lines 209-217 may remain (it is currently dead code and is out of scope).

- [ ] **Step 3: Compile to verify**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant compile
```

Expected: `BUILD SUCCESSFUL`. If javac reports unused/undefined identifiers, confirm the substitution removed *all* references to `imageViewDragon`, `imageViewPanda`, `imageViewBalloon`, `imageViewGoblin`, and `imageViewArcher`.

- [ ] **Step 4: Commit**

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git add COC/src/view/UnitIcon.java COC/src/view/Battle.java && git commit -m "feat(coc): drive battle drag bar through UnitIcon list with new troops"
```

---
## Task 6: End-to-end verification

**Files:**
- None (verification only)

**Interfaces:**
- Depends on all prior tasks.

- [ ] **Step 1: Full rebuild**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/JavaRA && ant jar && cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant jar
```

Expected: both builds succeed and `COC/dist/coc.jar` is regenerated.

- [ ] **Step 2: Manual gameplay check**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test/COC && ant run
```

Interactively verify (player is created at level 4, so all seven icons appear):

1. Log in → search for an opponent → enter battle.
2. Drag bar shows all seven icons: Dragon, Panda, Balloon, Goblin, Archer, plus the two new sprite icons at x=470 (Barbarian) and x=560 (Wizard).
3. Click the Barbarian icon (gold glow selects it), then click inside the map (`y < 575`). Capacity decreases by 5 and a soldier walks to the nearest building and attacks it in melee.
4. Click the Wizard icon, deploy. Capacity decreases by 20; a rocket trooper stops ~150 px away and lobs a purple projectile.
5. Deploy until capacity is exhausted; icons vanish as their cost exceeds remaining capacity.
6. The five existing troops (especially `PandaAttack` melee and `ArcherBalloonAttack` ranged) behave exactly as before.
7. When all buildings are destroyed, the win/lose result logic (`CheckResultAttack`) still resolves.

- [ ] **Step 3: Record and commit**

Run:

```bash
cd /Users/xuyi/Source/OS/armOS/lib/jcore/test && git status --short
```

Expected: only the files from Tasks 1-5 are modified (plus any unrelated pre-existing dirty files such as `assets/dist/assets.jar` or `.understand-anything/knowledge-graph.json` — do not stage those). No new commit is required unless a fix was needed; if a fix was made, commit it with a `fix(coc): ...` message.