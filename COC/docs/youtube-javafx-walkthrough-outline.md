# YouTube: JavaFX by Walking Through Clash of Clans — Series Outline

## Who this is for

Java developers who can read Java and OOP code but are new to JavaFX. We teach
JavaFX concepts as the project's real code first needs them. No "what is a button"
material.

## How to use this outline

This is a series outline, not a script. Each episode entry lists a title, a hook,
the viewer outcome, the exact code anchors walked on screen, the JavaFX concepts
introduced, ordered on-screen beats, one small refactor beat, and a cliffhanger.
A script writer can expand any entry independently.

## Series shape

Six episodes, roughly 10–20 minutes each. The order follows the player's runtime
journey: launch → log in → player panel → search → battle → save.

## Recurring threads

Woven through episodes as callbacks, never given their own episode:

1. The one-`Application`-per-screen navigation quirk (`new X(...).start(new Stage())`).
2. Absolute layout (`setX`/`setY`) versus auto layout (`VBox`, `BorderPane`).
3. The `assets` module and `module-info.java` requirements.
4. Threads and media (`MediaPlayer`, `Thread.sleep`, blocking the FX thread).

## Episode index

1. This App Skips Its Own Login Screen
2. Every Screen Is Its Own Application
3. The Model and the Missing Database
4. Pixels vs Panes: Rendering the Map
5. Attack, Animation, and the FX Thread
6. Results, Saving, and the Refactor Payoff

## Episode 1 — This App Skips Its Own Login Screen (~12 min)

**Hook:** The project has a login screen, but the app launches straight past it.

**Outcome:** Trace `main()` → `launch()` → `start()` and explain the JavaFX
application lifecycle.

**Code path:** `src/com/coc/GUI.java:11-25`, `build.xml:61` (`run` target),
`src/module-info.java`.

**JavaFX concepts:** `Application`, `launch()`, first sight of `Stage` and
`Scene`, and why `module-info` requires `javafx.controls`.

**On-screen beats:**
1. Run the app; observe it lands on `PlayerPanel`, not `LogIn`.
2. Inspect `LogInController.getPlayer("","")` and see it fabricate a `Player`
   (`src/controller/LogInController.java:21`).
3. Walk `src/module-info.java` and read the `requires` lines.

**Refactor beat:** Redirect the entry point to the real `LogIn` screen so the
journey starts correctly.

**Next time:** Where the login screen actually lives.

## Episode 2 — Every Screen Is Its Own Application (~18 min)

**Hook:** In this project, every screen is technically its own application.

**Outcome:** Construct a scene graph by hand and explain why a new `Stage` per
screen is a smell.

**Code path:** `src/view/LogIn.java` (whole file), `src/view/LogIn.java:79`,
`src/view/SignUp.java:51-60`.

**JavaFX concepts:** `BorderPane`/`VBox`/`HBox`, `Insets`/`spacing`/`alignment`,
stylesheets, `DropShadow`, `MediaPlayer`/`MediaView`, and the single-parent node
rule.

**On-screen beats:**
1. Draw LogIn's scene-graph tree on screen.
2. Highlight `vBoxFields()` and how its children attach.
3. Click Log In and show `new PlayerPanel(...).start(new Stage())`
   (`src/view/LogIn.java:79`) followed by `stage.close()`.

**Refactor beat:** Introduce a `Screens.show(...)` navigation helper and stop
creating a `Stage` per screen.

**Next time:** The click hands off a `Player` — what is that?

## Episode 3 — The Model and the Missing Database (~16 min)

**Hook:** There is a full database layer written here — and all of it is commented out.

**Outcome:** Explain the view/controller/model split and the persistence path.

**Code path:** `src/view/PlayerPanel.java`, `src/model/Player.java`,
`src/controller/LogInController.java`, `src/controller/SignUpController.java`,
`src/view/SignUp.java:90-126`, `src/model/data/LoadData.java`,
`src/model/data/SaveData.java:9`.

**JavaFX concepts:** MVC responsibilities in a JavaFX app, controller
construction, `Player` scoring (`Win()`/`Lose()`), `IdPlayerException`,
serialization, and `LoadData`'s fully-commented SQL.

**On-screen beats:**
1. Sign up with a map choice.
2. Show `SaveData extends Thread` (`src/model/data/SaveData.java:9`).
3. Reopen LoadData's commented JDBC block.

**Refactor beat:** Replace the fabricated-player fallback and introduce a
repository seam over `LoadData`.

**Next time:** You have a player — now find an enemy.

## Episode 4 — Pixels vs Panes: Rendering the Map (~18 min)

**Hook:** This screen places every building by hand, one X/Y at a time.

**Outcome:** Choose between absolute and auto layout, and load maps from YAML and
the assets module.

**Code path:** `src/view/SearchOpponents.java`, `src/model/Map/MapLoader.java`,
`src/model/Player.java`, the `assets` module, the `cr0s.javara` imports in
`src/model/Map/MapLoader.java:11-17`.

**JavaFX concepts:** `AnchorPane` + `setX`/`setY` versus auto panes, `Group`,
`ImageView`, snakeyaml parsing, and module `requires` (`snakeyaml`,
`cr0s.javara.main`).

**On-screen beats:**
1. Open SearchOpponents' absolute placement.
2. Walk `MapLoader.buildMap()`'s YAML switch
   (`src/model/Map/MapLoader.java:46-90`).
3. Show `getMapView()` building the `AnchorPane`
   (`src/model/Map/MapLoader.java:34-44`).

**Refactor beat:** Re-lay-out one absolute screen with auto layout and bindings.

**Next time:** You found an opponent — fight.

## Episode 5 — Attack, Animation, and the FX Thread (~20 min)

**Hook:** Every unit is a controller, and every controller sleeps on the UI thread.

**Outcome:** Explain per-unit controllers, `Transition` animation, and the
single-thread rule.

**Code path:** `src/view/Battle.java`, `src/controller/attack/*.java`,
`src/controller/defense/*.java`, `src/controller/CheckResultAttack.java`,
`src/view/LogIn.java:75`.

**JavaFX concepts:** a strategy-style controller per unit,
`PathTransition`/`Transition`, `Platform.runLater`, `AnimationTimer`, and why
`Thread.sleep(300)` on the FX thread stutters the UI.

**On-screen beats:**
1. Attack with one unit.
2. Follow a `PathTransition` in an attack controller.
3. Show `Platform.runLater` in a defense controller
   (e.g. `src/controller/defense/ArcherTowerDefense.java:80`).
4. Show the blocking `Thread.sleep` and its visible effect
   (`src/view/LogIn.java:75`).

**Refactor beat:** Replace the blocking sleep with `PauseTransition`.

**Next time:** The battle ends — who wins, and does it stick?

## Episode 6 — Results, Saving, and the Refactor Payoff (~16 min)

**Hook:** The battle ends, a result is decided, and the navigation refactor pays off.

**Outcome:** Follow a result from detection to persistence, and see the
navigation refactor end-to-end.

**Code path:** `src/controller/CheckResultAttack.java:22-140`,
`src/model/data/UpdateData.java:9`, `src/model/data/SaveData.java:9`, and the
`Screens.show(...)` helper introduced in Episode 2.

**JavaFX concepts:** result detection → `Player.Win()`/`Lose()` →
`UpdateData extends Thread`, thread-based persistence, closing the MVC loop.

**On-screen beats:**
1. Finish a battle.
2. Show `CheckResultAttack` writing the result
   (`src/controller/CheckResultAttack.java:22-140`).
3. Show `UpdateData`/`SaveData`.
4. Replay the whole journey on the refactored navigation.

**Refactor beat:** Consolidate `SaveData`/`UpdateData` behind one persistence API.

**Next time:** What is next — the socket two-player bonus and `jpackage` — both
explicitly out of scope.

## Out of scope

- Recording, editing, voiceover, and thumbnail production.
- Word-for-word scripts.
- Rewriting game logic (hero combat algorithms, level scoring) except where a
  refactor beat requires it.
- The socket-based two-player bonus.
- Packaging/deployment (`jpackage`, installers).
- Any change to the existing `learn-javafx/` personal-learning workspace.
