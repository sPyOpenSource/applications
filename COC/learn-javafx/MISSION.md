# Mission: Master the COC JavaFX Game — GUI First

## Why

You inherited a working "Clash of Clans" final project (Advanced Programming, Dr. Ramezani) and you're on a `new-gui` branch. The goal is to be able to **redesign and extend its screens confidently** — changing layouts, styling, and screen flow — without breaking the app or guessing at how JavaFX works.

## Success looks like

- Trace any screen from `GUI.start()` down to its rendered **scene graph**, and explain every node.
- Rebuild a screen's layout deliberately, choosing the right **layout pane** (`AnchorPane`, `BorderPane`, `VBox`, `HBox`, …) instead of hard-coding pixels everywhere.
- Style any screen with **CSS** and know which selector/property changes which node.
- Diagnose the JavaFX-specific bugs this codebase invites: image-path errors, module `opens`, background threads, and the "one `Application` per screen" pattern.
- Replace the project's screen-navigation hack with a clean, reusable pattern.

## Constraints

- Lessons target ~10 minutes each.
- You already have **some JavaFX basics** (Scene, basic layouts, buttons).
- Use the **real code** in `src/view/`, `src/controller/`, `src/model/` — not toy examples.
- Teaching files live in `COC/learn-javafx/`.
- The project must remain runnable after changes.

## Out of scope (for now)

- Rewriting game logic (hero combat algorithms, level scoring) except where the UI needs it.
- The socket-based two-player bonus.
- Packaging/deployment (`jpackage`, installers).
