# YouTube: JavaFX by Walking Through Clash of Clans — Episode 1 Script

**Episode:** 1 — "This App Skips Its Own Login Screen"
**Target runtime:** ~6 minutes
**Series:** 6-episode JavaFX walkthrough of the COC project
**Reference branch:** `new-gui`
**Deliverable type:** recording script (expands the outline entry; scripts were out of scope for the outline itself)

---

## Source context (verified on branch `new-gui`)

Every code anchor below resolves to real locations in the repo:

| Anchor | What it is | Verified |
|---|---|---|
| `src/com/coc/GUI.java:11-25` | Entry point: `GUI extends Application`, `start(Stage)`, `main()` → `launch()` | ✓ lines 11–25 are exactly the class |
| `src/controller/LogInController.java:21` | Fabricated-player fallback: `return new Player(...)` | ✓ line 21 |
| `src/controller/LogInController.java:15-23` | Whole `getPlayer` method incl. commented-out exception | ✓ |
| `src/module-info.java` | Module declaration, `requires javafx.controls` etc. | ✓ lines 1–18 |
| `build.xml:61` | Ant `run` target example | ✓ line 61 |
| `src/view/LogIn.java` | The real login screen (Ep 1 refactor target + Ep 2 setup) | ✓ lines 26–111 |
| `src/view/LogIn.java:79-80` | `new PlayerPanel(...).start(new Stage())` then `stage.close()` | ✓ lines 79–80 |

---

## Pre-flight checklist (before recording)

- [ ] On branch `new-gui`.
- [ ] Project compiles and runs (`ant run` or IDE run target `build.xml:61`).
- [ ] App launches and lands on `PlayerPanel` — **not** `LogIn`. This is the bug/hook of the episode.
- [ ] IDE or editor ready to open `GUI.java`, `LogInController.java`, and `module-info.java` side by side.
- [ ] Screen-recording + mic checked; ~12 min target budget.
- [ ] Outline entry for Ep 1 open for reference (`COC/docs/youtube-javafx-walkthrough-outline.md`).

---

## Script

### Cold open (0:00–1:30)

**On screen:** Launch the app. It boots straight into `PlayerPanel` — map, player stats, the works — with no username/password prompt ever shown.

**Narrator (on camera or voiceover):**
> "This project has a login screen. It has a sign-up screen. But if you run it right now, you never see either of them. The app just… skips straight to the player panel. In this episode we trace why — from `main()` through the JavaFX application lifecycle — and we fix it so the journey starts at login, like it should."

**On-screen beat 1:** Run the app; observe it lands on `PlayerPanel`, without `LogIn`.

---

### Beat 1 — The entry point: `main()` → `launch()` → `start()` (1:30–4:00)

**On screen:** Open `src/com/coc/GUI.java`. Highlight lines 23–25.

**Narrator:**
> "Every JavaFX application starts here. `GUI` extends `javafx.application.Application`. `main()` is just one line — it calls `launch()`, which is the static entry point the JavaFX runtime uses. `launch()` creates the FX application thread, instantiates our class, and calls `start(Stage)` — that's where our code actually runs."

**On-screen beat 2:** Highlight `start(Stage stage)` at line 13. Walk through the body:
- Line 15: `new LoadData()` — loads player data.
- Line 16: `new LogInController(...).getPlayer("", "")` — asks for a player with empty credentials.
- Line 17: `new PlayerPanel(player, ...).start(new Stage())` — opens the player panel in a brand-new window.

**Key concept to land:** `Application`, `launch()`, the single `Stage`/`Scene` relationship — first sight of `Stage` and `Scene` here via `PlayerPanel`.

---

### Beat 2 — Why it skips login: the fabricated player (4:00–7:00)

**On screen:** Open `src/controller/LogInController.java`. Highlight lines 15–23, especially line 21.

**Narrator:**
> "Look at line 16. `getPlayer("", "")` — empty id, empty password. The method loops through the known players (lines 16–20), finds no match because the credentials are blank, and then… line 21. Instead of throwing an exception or showing the login screen, it returns a brand-new fabricated `Player`: `new Player(new MapLoader(4, "map4"), "", "")`. A fake player, invented on the spot. That fake player is what gets handed to `PlayerPanel` back in `GUI.java:17` — so the app never needed a real login at all. The login screen exists in the repo, but nothing ever opens it."

**On-screen beat 3:** Show line 21 side-by-side with `GUI.java:16-17` so the viewer sees the hand-off: fabricated player → player panel.

**Line 22 note:** Mention the commented-out `throw new Exception("username or password is wrong!")` — the real behavior was deliberately replaced by the fallback.

---

### Beat 3 — The module path (7:00–9:00)

**On screen:** Open `src/module-info.java`. Read the `requires` lines aloud.

**Narrator:**
> "One more piece of the puzzle: this app uses the Java Platform Module System. `module com.coc` requires `javafx.controls`, `javafx.fxml`, `javafx.media`, `javafx.graphics`, and a few others — plus the project's own `assets` module. If you're running from the command line, the module path has to include all of these, which is why the Ant `run` target in `build.xml:61` matters. For this episode you don't need to master the module system — just know that `javafx.controls` is what puts `Application`, `Stage`, and `Scene` on the classpath."

**Concept:** why `module-info` requires `javafx.controls` (and friends).

---

### Beat 4 — The refactor beat: start at login (9:00–10:30)

**On screen:** Back in `GUI.java`, `start(Stage stage)`.

**Narrator:**
> "The fix is small and keeps the app runnable. Instead of fabricating a player and jumping straight to `PlayerPanel`, the entry point should open the real `LogIn` screen first. So we replace the body of `start()` with: load the players, create `new LogIn(players)`, and call `logIn.start(stage)` — reusing the stage we were given, not inventing a new one. The login screen then hands off to `PlayerPanel` only after a real login."

**On-screen beat 4:** Edit `GUI.java` to redirect the entry point to `LogIn`. Run the app again; confirm it now opens on `LogIn` instead of `PlayerPanel`.

**Refactor beat:** Redirect the entry point to the real `LogIn` screen so the journey starts correctly.

---

### Wrap & cliffhanger (10:30–12:00)

**On screen:** Show the app launching to `LogIn` now. Briefly show `LogIn.java` — its own `start(Stage)`, its `vBoxFields()`, and line 79: `new PlayerPanel(...).start(new Stage())` followed by `stage.close()`.

**Narrator:**
> "Now the app starts where it should — at login. But notice something odd: `LogIn` is itself a `javafx.application.Application`, and every screen in this project creates its own `Stage` with `new Stage()`. That works, but it's a smell. Next episode, we look at where the login screen actually lives, draw its scene graph by hand, and replace one-`Stage`-per-screen with a single navigation helper."

**Next time:** Where the login screen actually lives (Episode 2 — "Every Screen Is Its Own Application").

---

## Production notes

- **Tone:** Code-first, no "what is a button" material. Audience = Java developers new to JavaFX.
- **Recurring thread established in this episode:** one-`Application`-per-screen navigation (`new X(...).start(new Stage())`) — pay off in Episodes 2 and 6.
- **Do not cover in this episode:** MVC split (Ep 3), layout/refactor of SearchOpponents (Ep 4), animation & FX thread (Ep 5), persistence consolidation (Ep 6), socket two-player bonus, `jpackage`.
- **Length guard:** If recording runs long, trim Beat 3 (module-info reading) first; it's the least visual beat.

---

## Definition of done

- [ ] Script recorded (video + voiceover).
- [ ] On-screen beats 1–4 all captured in the final cut.
- [ ] Refactor beat demonstrated and app remains runnable after the change.
- [ ] Code anchors in the final video match the verified table above (branch `new-gui`).
- [ ] Episode ends on the Ep 2 cliffhanger.
