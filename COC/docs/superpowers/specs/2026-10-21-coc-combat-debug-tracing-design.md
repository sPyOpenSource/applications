# COC Combat / Game-State Debug Tracing — Design

**Date:** 2026-10-21
**Project:** Clash of Clans (COC) JavaFX game
**Scope:** Game-state and combat tracing in the console

## 1. Goals
Improve debug output for the COC game by adding structured, readable console traces for combat and game-state events, and by replacing bare `printStackTrace()` calls in touched code with contextual error messages.

## 2. Scope
In scope:
- Unit deployment, target selection, movement, damage, building destruction, unit death.
- Defense tower targeting and damage.
- Match progress checks and win/lose decisions.
- Map loading diagnostics (unknown actors).
- Contextual error logging in combat/map code.

Out of scope:
- Non-combat UI polish.
- External logging frameworks.
- Performance profiling.

## 3. Architecture
Introduce `util.DebugLog` with levels `ERROR`, `WARN`, `INFO`, `TRACE` and categories `combat`, `deploy`, `defense`, `result`, `map`, `data`.

API:
- `DebugLog.error(String category, String message, Throwable t)`
- `DebugLog.warn(String category, String message)`
- `DebugLog.info(String category, String message)`
- `DebugLog.trace(String category, String message)`

Output format:
`[timestamp] [LEVEL] [category] [thread] message`
Errors/stack traces go to `System.err`; other levels to `System.out`.

Toggle: `DEBUG_LEVEL` defaults to `TRACE`, overridable via `-Dcoc.debug=LEVEL` (`OFF` disables all).

## 4. Instrumentation Points
- `view.Battle.deploy(...)` — unit spawn details.
- `controller.attack.UnitAttack` and subclasses (`DragonAttack`, etc.) — deploy, target selection, movement, attack, damage, destruction, death.
- `controller.defense.*Defense` — target selection and damage.
- `controller.CheckResultAttack.run()` — progress checks and win/lose decision.
- `model.Map.MapLoader.buildMap(...)` — warn on unknown actor IDs.

Error handling: replace `e.printStackTrace()` in these classes with `DebugLog.error(category, context, e)`.

## 5. Configuration & Defaults
- Default level `TRACE` in development.
- Quiet via `-Dcoc.debug=OFF` or `-Dcoc.debug=ERROR`.
- No new dependencies.

## 6. Testing
Manual smoke test across deploy/attack/destroy/win/lose scenarios; verify `OFF` emits no trace lines.