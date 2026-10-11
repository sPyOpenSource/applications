# YouTube JavaFX Walkthrough — Series Outline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce the series-outline deliverable for a 6-episode YouTube walkthrough of the COC JavaFX project.

**Architecture:** A single Markdown document. Each task writes one self-contained section (front matter, one episode entry, or the closing scope section). Episode entries all share the same seven-field format so a future script writer can expand any one entry independently.

**Tech Stack:** Markdown only. No code is changed. Reference source is this repo at branch `new-gui`.

## Global Constraints

- Deliverable is the outline document `COC/docs/youtube-javafx-walkthrough-outline.md` only. No scripts, no recording, no code changes.
- 6 episodes, roughly 10–20 minutes each.
- Audience: Java developers new to JavaFX. Skip "what is a button" material.
- Episode order follows the player's runtime journey: launch → log in → player panel → search → battle → save.
- Every episode entry contains exactly these seven fields, in this order: Title, Hook, Outcome, Code path, JavaFX concepts, On-screen beats, Refactor beat, Next time.
- All `file:line` anchors must resolve in this repo at branch `new-gui`.
- Recurring threads are callbacks, never their own episodes: (1) one-`Application`-per-screen navigation, (2) absolute vs auto layout, (3) the `assets` module + `module-info.java`, (4) threads and media.
- Source spec: `COC/docs/superpowers/specs/2026-10-11-youtube-javafx-walkthrough-design.md`.

---

### Task 1: Document skeleton, front matter, and episode index

**Files:**
- Create: `COC/docs/youtube-javafx-walkthrough-outline.md`

**Interfaces:**
- Consumes: nothing.
- Produces: the file with a stable heading hierarchy that Tasks 2–7 append episode sections to, and an episode index that lists the six episode titles verbatim.

- [ ] **Step 1: Write the skeleton**

Create the file with exactly this content:

```markdown
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
```

- [ ] **Step 2: Verify the headings and index**

Run: `grep -n '^#\|^[0-9]\. ' COC/docs/youtube-javafx-walkthrough-outline.md`
Expected: one `#` title, five `##` sections, and six numbered episode titles matching the spec's six titles.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: scaffold YouTube JavaFX walkthrough outline"
```

---

### Task 2: Episode 1 entry

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after the episode index)

**Interfaces:**
- Consumes: the `##` heading level used for front-matter sections in Task 1.
- Produces: the first `## Episode 1 — ...` heading, title "This App Skips Its Own Login Screen", matching index item 1.

- [ ] **Step 1: Append the Episode 1 section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '11,25p' src/com/coc/GUI.java && sed -n '19p' src/controller/LogInController.java && sed -n '61p' build.xml`
Expected: the `start`/`main` methods, the fabricated-`Player` return line, and the `run` target line respectively.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 1 (entry point) to walkthrough outline"
```

---

### Task 3: Episode 2 entry

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after Episode 1)

**Interfaces:**
- Consumes: the `##` episode heading used in Task 2.
- Produces: episode title "Every Screen Is Its Own Application", matching index item 2, and the `Screens.show(...)` navigation helper that Episode 6 references.

- [ ] **Step 1: Append the Episode 2 section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '79p' src/view/LogIn.java && sed -n '44,60p' src/view/SignUp.java`
Expected: the `new PlayerPanel(...).start(new Stage())` navigation line, and the `SignUp.start` body building the root/Scene.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 2 (scene graph and navigation) to walkthrough outline"
```

---

### Task 4: Episode 3 entry

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after Episode 2)

**Interfaces:**
- Consumes: the `##` episode heading.
- Produces: episode title "The Model and the Missing Database", matching index item 3.

- [ ] **Step 1: Append the Episode 3 section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '9p' src/model/data/SaveData.java && sed -n '90,126p' src/view/SignUp.java`
Expected: the `public class SaveData extends Thread` line, and the sign-up submit handler.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 3 (model and persistence) to walkthrough outline"
```

---

### Task 5: Episode 4 entry

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after Episode 3)

**Interfaces:**
- Consumes: the `##` episode heading.
- Produces: episode title "Pixels vs Panes: Rendering the Map", matching index item 4.

- [ ] **Step 1: Append the Episode 4 section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '11,17p' src/model/Map/MapLoader.java && sed -n '34,44p' src/model/Map/MapLoader.java && sed -n '46,90p' src/model/Map/MapLoader.java`
Expected: the `cr0s.javara` imports, the `getMapView()` `AnchorPane` builder, and the `buildMap` YAML switch respectively.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 4 (layout and map rendering) to walkthrough outline"
```

---

### Task 6: Episode 5 entry

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after Episode 4)

**Interfaces:**
- Consumes: the `##` episode heading.
- Produces: episode title "Attack, Animation, and the FX Thread", matching index item 5.

- [ ] **Step 1: Append the Episode 5 section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '75p' src/view/LogIn.java && sed -n '80p' src/controller/defense/ArcherTowerDefense.java`
Expected: the `Thread.sleep(300)` line inside the login handler, and a `Platform.runLater` line in the defense controller.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 5 (attack, animation, threading) to walkthrough outline"
```

---

### Task 7: Episode 6 entry and Out of scope section

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (append after Episode 5)

**Interfaces:**
- Consumes: the `##` episode heading, and the `Screens.show(...)` helper named in Task 3.
- Produces: episode title "Results, Saving, and the Refactor Payoff", matching index item 6, plus a closing `## Out of scope` section.

- [ ] **Step 1: Append the Episode 6 section and Out of scope section**

Append exactly:

```markdown
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
```

- [ ] **Step 2: Verify anchors resolve**

Run: `sed -n '22p' src/controller/CheckResultAttack.java && sed -n '9p' src/model/data/UpdateData.java`
Expected: the `CheckResultAttack extends Thread` declaration line, and the `public class UpdateData extends Thread` line.

- [ ] **Step 3: Commit**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: add episode 6 and out-of-scope to walkthrough outline"
```

---

### Task 8: Full-outline consistency pass

**Files:**
- Modify: `COC/docs/youtube-javafx-walkthrough-outline.md` (only if a check fails)

**Interfaces:**
- Consumes: the complete outline from Tasks 1–7.
- Produces: a verified deliverable meeting the spec's success criteria.

- [ ] **Step 1: Check all seven fields appear once per episode**

Run: `grep -c '^\*\*Hook:\*\*\|^\*\*Outcome:\*\*\|^\*\*Code path:\*\*\|^\*\*JavaFX concepts:\*\*\|^\*\*On-screen beats:\*\*\|^\*\*Refactor beat:\*\*\|^\*\*Next time:\*\*' COC/docs/youtube-javafx-walkthrough-outline.md`
Expected: `42` (6 episodes × 7 fields).

- [ ] **Step 2: Check episode count and order match the index**

Run: `grep -n '^## Episode' COC/docs/youtube-javafx-walkthrough-outline.md`
Expected: six `## Episode` lines, numbered 1–6, with titles matching the index in Task 1.

- [ ] **Step 3: Re-run every anchor verification**

Run: `sed -n '11,25p' src/com/coc/GUI.java && sed -n '19p' src/controller/LogInController.java && sed -n '79p' src/view/LogIn.java && sed -n '9p' src/model/data/SaveData.java && sed -n '34,44p' src/model/Map/MapLoader.java && sed -n '80p' src/controller/defense/ArcherTowerDefense.java && sed -n '22p' src/controller/CheckResultAttack.java && sed -n '9p' src/model/data/UpdateData.java`
Expected: each range/line prints the referenced construct. If any fails, correct the anchor in the outline.

- [ ] **Step 4: Check no placeholder text slipped in**

Run: `grep -ni 'tbd\|todo\|fixme\|implement later\|etc\.' COC/docs/youtube-javafx-walkthrough-outline.md`
Expected: no output.

- [ ] **Step 5: Commit any fixes**

```bash
git add COC/docs/youtube-javafx-walkthrough-outline.md
git commit -m "docs: consistency pass on YouTube JavaFX walkthrough outline"
```
