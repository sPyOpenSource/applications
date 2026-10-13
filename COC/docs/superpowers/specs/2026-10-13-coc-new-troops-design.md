# Design: Two New Playable Troops (Barbarian, Wizard)

Date: 2026-10-13
Status: Approved (design); implementation plan pending

## Problem

The Clash of Clans JavaFX game (entry point `src/com/coc/GUI.java`, branch
`new-gui`) ships five playable troops — Dragon, Panda, Balloon, Goblin, Archer.
Each troop is an `*Attack` controller (a `Thread`) plus a backing entity, and the
drag bar that deploys them lives in `src/view/Battle.java`. We want to add more
playable characters, but the existing controllers copy-paste the same
synchronized `wait`/`notify` + `Platform.runLater` walk/attack skeleton, and
`Battle.java` wires each icon with a hand-written click handler. Naively adding
troops would duplicate ~200 lines of threading code each and push `Battle.java`
past 300 lines of near-identical wiring.

## Goal

Add two new playable troops — a melee **Barbarian** and a ranged **Wizard** — that
reuse existing JavaRA sprites (no new art), while removing the duplication that
adding characters would otherwise create.

## Deliverable

A working in-game feature: two new deployable units with correct stats, drag
icons, capacity costs, and level gating. Plus two shared controller base classes
and a small icon helper, so future characters are cheap to add.

## Audience / context

This is the same codebase as the YouTube walkthrough project
(`docs/superpowers/specs/2026-10-11-youtube-javafx-walkthrough-design.md`);
that outline is unrelated to this deliverable and is not modified here.

## Chosen approach

**Approach B — shared melee/ranged base classes.** Two abstract controllers,
`MeleeUnitAttack` and `RangedUnitAttack`, capture the walk/attack loop once.
The two new controllers become thin subclasses. The existing `PandaAttack`,
`DragonAttack`, `DefBalloonAttack`, `GoblinBalloonAttack`, and
`ArcherBalloonAttack` are left untouched.

Rejected alternatives:

- **Approach A — pattern-copy.** Clone `PandaAttack` and `ArcherBalloonAttack`
  into two new controllers. Lowest risk, but ~440 lines of near-duplicate
  threading code and a larger `Battle.java`. Rejected as needless duplication.
- **Approach C — data-driven unit registry.** A `UnitType` record plus a
  `BattleBar` that builds the entire bar (icons, costs, level gating) from a
  list. Cleanest long-term, but a large refactor of working, delicate code —
  overkill for two characters (YAGNI).

## Components

### 1. Entities (JavaRA)

Two new classes in `JavaRA/src/cr0s/javara/entity/infantry/`, modeled on the
existing `EntityGrenadeTrooper` (which loads an RA `.shp` texture through
`ResourceManager`, builds `Sequence`s, renders the stand frame, and adds that
`Image` to `getImages()`):

- `Barbarian` — reuses `e1.shp` (rifle soldier).
- `Wizard` — reuses `e3.shp` (rocket soldier).

The COC-specific entities `Panda`, `Dragon`, `GoblinBalloon`, `DefBalloon`, and
`ArcherBalloon` already live in this same JavaRA tree, so adding COC troops here
follows the existing convention. No new sprite or audio files are introduced;
sprites load from the RA archives exactly as `ArcherBalloonAttack` already does
via `EntityGrenadeTrooper`.

Each entity exposes its sprite via `getImages().get(0)`. Facing is a single stand
frame used for both directions (no left/right flip), matching the existing
`ArcherBalloonAttack` behavior; the RA sprite is a single-frame stand pose.

### 2. Controller base classes (COC)

New files in `src/controller/attack/`:

- `UnitAttack` (abstract, `extends Thread`) — owns everything shared:
  - Fields: `root` (`AnchorPane`), `map` (`Map`), `unit` (`MobileEntity`),
    `view` (`ImageView`), start position. The constructor positions the unit,
    registers it with `map.getAttackingHeroes().add(unit)`, and sets its
    `ImageView`.
  - Concurrency helpers: `myNotify()`, `await()`, `await(long ms)`,
    `runOnFx(Runnable)` — the `Platform.runLater` + `wait`/`notify` scaffolding
    currently copy-pasted in every controller.
  - Audio helper: `playAudio(String assetPath, double volume, boolean loop)`.
  - Geometry helpers: `nearestBuilding()`, `distance(...)`,
    `nearestPointOnCircle(...)` (shared verbatim from `ArcherBalloonAttack`).
  - Template `run()`: deploy sprite -> loop `{ moveToward(); attack(building); }`
    until the unit dies -> remove sprite and notify. Calls two abstract hooks:
    `moveToward()` and `attack(EntityBuilding)`.
  - Abstract config hooks: `deployAudio()`, `moveAudio()`, `attackAudio()`,
    `dieAudio()`, `attackRange()`.
- `MeleeUnitAttack extends UnitAttack` — `moveToward()` walks via
  `PathTransition` to the building edge; `attack()` ticks
  `building.setHp(hp - DPS)` every 1250 ms until the building or unit dies.
  Single sprite frame; no projectile.
- `RangedUnitAttack extends UnitAttack` — `moveToward()` stops at
  `attackRange()` px using `nearestPointOnCircle`; `attack()` spawns a projectile
  `Circle`, animates it with a `PathTransition`, and ticks DPS every 1000 ms.

### 3. Concrete controllers (COC)

- `BarbarianAttack extends MeleeUnitAttack` — constructs `Barbarian`, supplies
  stats and audio. ~30 lines.
- `WizardAttack extends RangedUnitAttack` — constructs `Wizard`, supplies stats,
  audio, and `attackRange() == 150`. ~30 lines.

### 4. Battle integration and `UnitIcon` (COC)

New `src/view/UnitIcon` value type: image, capacity cost, minimum level, and a
spawn callback. `Battle.java` installs all seven icons through it, replacing:

- the five copy-pasted `setOnMouseClicked` selection blocks,
- the 7-branch deploy `if/else` chain,
- the four capacity-based removal `if`s.

The generic handler manages mutually-exclusive selection (the `DropShadow`
effect), checks the deploy zone (`mouseEvent.getY() < 575`) and affordability,
decrements capacity, starts the unit's `*Attack` thread, and updates the
capacity text. Icons are removed when capacity drops below their cost, as today.

## Character specification

| | Barbarian (melee) | Wizard (ranged) |
|---|---|---|
| Sprite | `e1.shp` | `e3.shp` |
| Entity | `Barbarian extends EntityInfantry` | `Wizard extends EntityInfantry` |
| HP | 1200 | 500 |
| DPS | 150 | 300 |
| Move speed | 30 | 20 |
| Attack | Melee contact, 1250 ms tick | Stops at 150 px, projectile circle, 1000 ms tick |
| Capacity cost | 5 | 20 |
| Available from level | 1 | 2 |
| Deploy / move audio | `enter_panda.mp3` / `not_path.mp3` | `balloonDeploy.mp3` / `not_path.mp3` |
| Attack / die audio | `exp1.mp3` | `fire.mp3` / `balloonDie.mp3` |

## Level gating

`Battle.attack()`'s `switch (attackingPlayer.getLevel())` becomes:

| Level | Units shown |
|---|---|
| 1 | Dragon, Panda, Barbarian |
| 2 | previous + Balloon, Wizard |
| 3 | previous + Goblin |
| 4 | previous + Archer |

The player starts at level 4 (`Player` field initializer), so all seven icons
appear by default.

## Data flow

Player selects an icon -> clicks the map -> `Battle` starts the unit's
`*Attack` thread -> the thread adds the sprite to `root` via `Platform.runLater`,
walks to the nearest building with a `PathTransition`, ticks building HP,
removes the building, and repeats. `CheckResultAttack` continues to watch
`map.getAttackingHeroes()` for win/lose and is unchanged.

## Error handling and concurrency

`InterruptedException` is caught and logged exactly as in the existing
controllers. The `run()` loop guards against a null building (unit dies) and a
dead unit (loop breaks), so no thread hangs. No new failure modes are introduced
beyond the existing pattern.

## Testing

The app is a JavaFX GUI, so verification is manual via `ant run`:

1. Sign up and enter battle.
2. Confirm the Barbarian and Wizard icons appear for the player's level.
3. Select each and deploy; observe walking, attacking, capacity decrement,
   building removal, and win/lose.
4. Confirm the five existing troops still behave as before (selection,
   deploy, capacity).

The existing JUnit stub (`test/test/testClass.java`) is left untouched;
unit-testing the threaded GUI controllers is out of scope.

## Out of scope

- Any new art or audio assets.
- Changing the five existing troops or the defense controllers.
- Refactoring `CheckResultAttack` or the persistence layer.
- The socket two-player bonus and packaging (`jpackage`).
- Modifying the YouTube walkthrough documents.

## Success criteria

- `Barbarian` and `Wizard` are deployable in battle with the stats, costs, and
  level gating above, reusing existing sprites and audio.
- The five existing troops behave identically to before.
- Adding the two troops introduces no copy-pasted threading code: the shared
  loop lives only in `UnitAttack`/`MeleeUnitAttack`/`RangedUnitAttack`.
- `Battle.java` icon wiring is data-driven through `UnitIcon`; each icon is one
  entry rather than a hand-written click handler.
- The app compiles and runs (`ant run`) with no regressions.

## File map

| Action | Path |
|---|---|
| add | `JavaRA/src/cr0s/javara/entity/infantry/Barbarian.java` |
| add | `JavaRA/src/cr0s/javara/entity/infantry/Wizard.java` |
| add | `COC/src/controller/attack/UnitAttack.java` |
| add | `COC/src/controller/attack/MeleeUnitAttack.java` |
| add | `COC/src/controller/attack/RangedUnitAttack.java` |
| add | `COC/src/controller/attack/BarbarianAttack.java` |
| add | `COC/src/controller/attack/WizardAttack.java` |
| add | `COC/src/view/UnitIcon.java` |
| edit | `COC/src/view/Battle.java` |
| untouched | all existing `controller/attack/*`, `controller/defense/*`, `CheckResultAttack`, `Panda`, `Dragon` |
