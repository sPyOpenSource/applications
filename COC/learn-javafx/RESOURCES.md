# JavaFX (COC GUI) — Resources

## Knowledge

- [OpenJFX JavaDoc — `javafx.scene.Node`](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/Node.html)
  The authoritative definition of a scene-graph node, branch vs leaf, the single-parent/no-cycle rules, and `id`/`styleClass`. Use for: any claim about how nodes attach, style, or behave.

- [OpenJFX JavaDoc — `javafx.scene.Scene` & `javafx.stage.Stage`](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/package-summary.html)
  `Scene` holds the root node and size; `Stage` is the OS window. Use for: the window/stack model at the top of every screen.

- [Oracle Tutorial — Working with the JavaFX Scene Graph](https://docs.oracle.com/javase/8/javafx/scene-graph-tutorial/scenegraph.htm)
  The clearest end-to-end walkthrough of the scene graph concept. Still accurate. Use for: grounding the mental model before touching code.

- [Oracle Tutorial — Using Built-in Layout Panes](https://docs.oracle.com/javafx/2/layout/builtin_layouts.htm)
  Side-by-side behaviour of `BorderPane`, `HBox`, `VBox`, `StackPane`, `AnchorPane`, `GridPane`, `FlowPane`, `TilePane`. Use for: choosing the right pane when rebuilding a screen.

- [JavaFX CSS Reference Guide (OpenJFX)](https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/doc-files/cssref.html)
  Every `-fx-*` property per node class, and how stylesheets cascade from `Scene`/`Parent`. Use for: theming and knowing what `style.css` can control.

- [Dev.java — JavaFX learning trail](https://dev.java/learn/javafx/)
  Oracle's modern, maintained tutorial series (layout controls, effects, "make a scene"). Use for: concise refreshers with current API.

## Wisdom (Communities)

- [r/JavaFX](https://www.reddit.com/r/JavaFX)
  Active, moderated against low-effort posts. Use for: layout critiques, "why does my node not resize?" troubleshooting, and sanity-checking patterns.

- [JavaFX Discord (via openjfx.io community)](https://openjfx.io/community/)
  Real-time help from framework contributors and experienced users. Use for: sticky module/`opens` and runtime errors where a fast back-and-forth beats a forum.

## Gaps

- The project pulls map rendering and entities from an external `cr0s.javara` engine plus a custom `assets` module. No external docs found yet — must be reverse-engineered from the local jars/source. Flagged for a future investigation lesson.
- No local offline copy of the JavaFX JavaDoc yet; links currently assume internet access.
