# Teaching Notes — JavaFX / COC

## Preferences

- **Goal:** master this COC project, starting with **GUI redesign**.
- **Level:** some JavaFX basics (Scene, layouts, buttons) — skip "what is a button" material.
- **Pacing:** ~10-minute lessons, one tangible win each.
- **Workspace:** `COC/learn-javafx/` (kept out of the tracked source tree root).
- **Ground everything in the real code** in `src/view|controller|model/`, not toy snippets.
- Use **retrieval quizzes** at the end of each lesson; keep MCQ options the same word-length.
- Lesson style: Tufte — clean, printable, linked to reference docs.

## Working context

- Repo branch: `new-gui`. Entry point: `src/com/coc/GUI.java` → `launch()`.
- Every screen is its own `Application` subclass, opened with `new X(...).start(new Stage())`. This is the central architectural quirk to fix for clean GUI work.
- `module-info.java` requires many `javafx.*` modules and `opens com.coc to javafx.fxml`.
- CSS lives at `src/style.css`, loaded per-scene via `scene.getStylesheets().add("style.css")`.
