# Design: YouTube JavaFX Walkthrough of the COC Project

Date: 2026-10-11
Status: Approved (design); implementation plan pending

## Problem

The repo contains a working "Clash of Clans" JavaFX final project
(`src/com/coc/GUI.java` entry point, branch `new-gui`). We want to publish a
YouTube tutorial series that teaches JavaFX by walking through this real
codebase — explaining how it works and improving its rough edges — rather than
using toy examples.

## Deliverable

**A series outline document only.** No scripts, no recording, no code changes as
part of this deliverable. Each episode is specified as a self-contained outline
entry (see "Episode entry format") that a future author can expand into a
recording script.

## Audience

Java developers who are new to JavaFX. They can read Java and OOP code but need
JavaFX concepts (scene graph, layout panes, stylesheets, the single-thread rule)
introduced as the code first requires them. Introductory "what is a button"
material is out of scope.

## Angle and structure

**A code walkthrough that follows the player's runtime journey.** Episode order
mirrors what a player does — launch → log in → player panel → search → battle →
save — so each episode answers "what happens when you click X," and each JavaFX
concept is introduced at the moment the source needs it. The project's
architectural quirks surface naturally as pain points, driving a refactor thread.

Rejected alternatives:

- **Layering order** (entry → view → controller → model → data → engine): maps to
  the package structure but front-loads abstractions and loses the narrative.
- **Concept-per-episode** (scene graph, layout, CSS, modules, threading,
  refactor): fits a fundamentals course, not a code walkthrough; would drift from
  the project's own story.

## Series parameters

- 6 episodes, roughly 10–20 minutes each.
- Reference code is the repo at branch `new-gui`. Refactor beats describe changes
  that the *video* demonstrates; this deliverable performs no code changes. Each
  described change is small and keeps the app runnable.
- The existing personal `learn-javafx/` workspace is not the deliverable and is
  not modified by this outline.

## Episode entry format

Every episode in the outline contains:

1. **Title** and a one-line hook.
2. **Outcome** — what the viewer can do or explain afterward.
3. **Code path** — exact `file:line` anchors walked on screen.
4. **JavaFX concepts** — introduced when the code first needs them.
5. **On-screen beats** — ordered demo steps (open file → highlight → run).
6. **Refactor beat** — the small, runnable improvement applied this episode.
7. **Next time** — cliffhanger into the following episode.

## Recurring threads

Woven through episodes as callbacks, not given their own episodes:

1. The one-`Application`-per-screen navigation quirk
   (`new X(...).start(new Stage())`).
2. Absolute layout (`setX`/`setY`) versus auto layout (`VBox`, `BorderPane`).
3. The `assets` module and `module-info.java` requirements.
4. Threads and media (`MediaPlayer`, `Thread.sleep`, blocking the FX thread).

## Episodes

### Ep 1 — "This App Skips Its Own Login Screen" (~12 min)

- **Outcome:** trace `main()` → `launch()` → `start()` and explain the JavaFX app
  lifecycle.
- **Code path:** `src/com/coc/GUI.java:11-25`, `build.xml:61` (`run` target),
  `src/module-info.java`.
- **JavaFX concepts:** `Application`, `launch()`, first sight of `Stage`/`Scene`,
  why `module-info` requires `javafx.controls`.
- **On-screen beats:** run the app → observe it lands on `PlayerPanel`, not
  `LogIn` → inspect `LogInController.getPlayer("","")` returning a fabricated
  `Player` (`LogInController.java:21`) → walk `module-info.java` legibly.
- **Refactor beat:** redirect the entry point to the real `LogIn` screen so the
  journey starts correctly.
- **Next time:** where the login screen actually lives.

### Ep 2 — "Every Screen Is Its Own Application" (~18 min)

- **Outcome:** construct a scene graph by hand and explain why a new `Stage` per
  screen is a smell.
- **Code path:** `src/view/LogIn.java` (whole file), `src/view/SignUp.java:51-60`.
- **JavaFX concepts:** `BorderPane`/`VBox`/`HBox`, `Insets`/`spacing`/`alignment`,
  stylesheets, `DropShadow`, `MediaPlayer`/`MediaView`, node parent rules.
- **On-screen beats:** draw LogIn's scene-graph tree → highlight `vBoxFields()` →
  click Log In → show `new PlayerPanel(...).start(new Stage())`
  (`LogIn.java:79`) and `stage.close()`.
- **Refactor beat:** introduce a `Screens.show(...)` navigation helper and stop
  creating a `Stage` per screen.
- **Next time:** the click hands off a `Player` — what is that?

### Ep 3 — "The Model and the Missing Database" (~16 min)

- **Outcome:** explain the view/controller/model split and the persistence path.
- **Code path:** `src/view/PlayerPanel.java`, `src/model/Player.java`,
  `src/controller/LogInController.java`, `src/controller/SignUpController.java`,
  `src/view/SignUp.java:90-126`, `src/model/data/LoadData.java`,
  `src/model/data/SaveData.java`.
- **JavaFX concepts:** MVC responsibilities in a JavaFX app, controller
  construction, `Player` scoring (`Win()`/`Lose()`), `IdPlayerException`,
  serialization, and `LoadData`'s fully-commented SQL.
- **On-screen beats:** sign up with a map choice → `SaveData extends Thread`
  (`SaveData.java:9`) → reopen LoadData's commented JDBC block.
- **Refactor beat:** replace the fabricated-player fallback and introduce a
  repository seam over `LoadData`.
- **Next time:** you have a player — now find an enemy.

### Ep 4 — "Pixels vs Panes: Rendering the Map" (~18 min)

- **Outcome:** choose between absolute and auto layout, and load maps from YAML
  and the assets module.
- **Code path:** `src/view/SearchOpponents.java`, `src/model/Map/MapLoader.java`,
  `src/model/Player.java` (map field), the `assets` module, the `cr0s.javara`
  imports.
- **JavaFX concepts:** `AnchorPane` + `setX`/`setY` versus auto panes, `Group`,
  `ImageView`, snakeyaml parsing, and module `requires` (`snakeyaml`,
  `cr0s.javara.main`).
- **On-screen beats:** open SearchOpponents' absolute placement →
  `MapLoader.buildMap()` YAML switch → `getMapView()` building the `AnchorPane`.
- **Refactor beat:** re-lay-out one absolute screen with auto layout and
  bindings.
- **Next time:** you found an opponent — fight.

### Ep 5 — "Attack, Animation, and the FX Thread" (~20 min)

- **Outcome:** explain per-unit controllers, `Transition` animation, and the
  single-thread rule.
- **Code path:** `src/view/Battle.java`, `src/controller/attack/*.java`,
  `src/controller/defense/*.java`, `src/controller/CheckResultAttack.java`,
  `src/view/LogIn.java:75`.
- **JavaFX concepts:** strategy-style controller per unit,
  `PathTransition`/`Transition`, `Platform.runLater`, `AnimationTimer`, and why
  `Thread.sleep(300)` on the FX thread stutters the UI.
- **On-screen beats:** attack with one unit → follow a `PathTransition` → show
  `Platform.runLater` in a defense controller → show the blocking `Thread.sleep`
  and its visible effect.
- **Refactor beat:** replace the blocking sleep with `PauseTransition`.
- **Next time:** the battle ends — who wins, and does it stick?

### Ep 6 — "Results, Saving, and the Refactor Payoff" (~16 min)

- **Outcome:** follow a result from detection to persistence, and see the
  navigation refactor end-to-end.
- **Code path:** `src/controller/CheckResultAttack.java:22-140`,
  `src/model/data/UpdateData.java`, `src/model/data/SaveData.java`, and the Ep 2
  `Screens` helper.
- **JavaFX concepts:** result detection → `Player.Win()`/`Lose()` →
  `UpdateData extends Thread`, thread-based persistence, closing the MVC loop.
- **On-screen beats:** finish a battle → `CheckResultAttack` writes the result →
  show `UpdateData`/`SaveData` → replay the whole journey on the refactored
  navigation.
- **Refactor beat:** consolidate `SaveData`/`UpdateData` behind one persistence
  API.
- **Outro:** what is next — the socket two-player bonus and `jpackage` — both
  explicitly out of scope.

## Out of scope

- Recording, editing, voiceover, and thumbnail production.
- Word-for-word scripts (a future step may expand the outline into scripts).
- Rewriting game logic (hero combat algorithms, level scoring) except where a
  refactor beat requires it.
- The socket-based two-player bonus.
- Packaging/deployment (`jpackage`, installers).
- Any change to the existing `learn-javafx/` personal-learning workspace.

## Success criteria

- Six episode entries, each complete against the episode-entry format.
- Every episode's code anchors resolve to real locations in the repo at branch
  `new-gui`.
- The episode order follows the player's runtime journey.
- Each listed refactor beat is independent, small, and keeps the app runnable.
- A future script writer can expand any entry into a recording script without
  re-doing the code archaeology.
