# Design: Unified Logging for JavaRA

**Date:** 2026-10-21
**Project:** JavaRA (JavaRA/src, JavaFX real-time strategy game)
**Status:** Approved (design); implementation plan pending

---

## Problem

JavaRA's diagnostic output has no consistent format and no levels. Three
unrelated mechanisms coexist:

1. **Raw `System.out` / `System.err` calls** — 31 live call sites across
   `cr0s.javara`, each with its own ad-hoc prefix (`[AI]`, `[SBP]`, `[MAP]`,
   `ERR:`, `HIRES:`, `[Warning]`, `[BUG]`) and no way to turn them off.
2. **`GUI.DEBUG_MODE`** — a `public static boolean = false` compile-time switch.
   All 17 of its use sites across 17 files are dead: every block body is
   commented-out Slick render code left over from the Slick-to-JavaFX migration.
   The one exception, `MobileEntity.drawPath(Scene g)`, has an equally
   commented-out body and zero live callers. Two further references sit inside
   block comments (`render/World.java`, `entity/building/common/EntityConstructionYard.java`).
3. **`mazesolver.Helper.log` / `Helper.debug`** — a separate mini-app with its
   own `Settings.Main.showLog` / `showDebug` flags. Verified that
   `cr0s.javara` never calls these, so the two apps do not interact.

A fourth form of output, `e.printStackTrace()`, appears 19 times in
`cr0s.javara`. Bare stack traces on stderr carry no indication of what was
being attempted, no level, and no format.

The consequences:

- Real errors (missing sound file, invalid map) are indistinguishable from
  trace noise such as `"Attacking"` and `"[AI] Checking building limit for ..."`.
- Per-frame output cannot be silenced, so the console is flooded during play.
- There is no runtime verbosity control; changing what is printed requires a
  rebuild.
- Output is not greppable: no fixed level column, no fixed category column,
  no consistent timestamp.

## Goal

One logging facility for `cr0s.javara` with a fixed five-level vocabulary, a
fixed line format, a category that identifies the subsystem, and runtime
control over verbosity — so that errors are always visible, noise is silent by
default, and any subsystem can be turned up without recompiling.

## Non-goals

- `net/pso/jswarm/**` — vendored third-party particle-swarm library, including
  its own `Gpr.debug()` helper and `Example` programs. Left byte-for-byte
  unchanged.
- `mazesolver/**` — a separate mini-app with its own settings. Left
  byte-for-byte unchanged.
- Comments and commented-out code are not migrated, except where a
  commented-out reference names a method this change deletes (see *Retiring
  `GUI.DEBUG_MODE`*).
- Adding a file-based log, log rotation, or an in-game log overlay.
- Fixing the unrelated broken build paths in `nbproject/project.properties`
  (see *Aside*).

---

## 1. The logging facility

The foundation is `java.util.logging` (JDK built-in). Chosen over a bespoke
logger because it adds no dependency, works unmodified in JPMS module mode,
and its hierarchical logger names map directly onto JavaRA's package
structure — which yields per-subsystem level control for free.

### 1.1 `cr0s.javara.util.Log`

A thin facade over `java.util.logging`.

```java
package cr0s.javara.util;

public final class Log {
    public static Log of(Class<?> type)          // logger name = full class name
    public static Log of(String name)

    public void error(String message)
    public void error(String message, Throwable cause)
    public void warn (String message)
    public void warn (String message, Throwable cause)
    public void info (String message)
    public void debug(String message)
    public void trace(String message)

    public static void configure()               // idempotent
}
```

`Log` is immutable and holds one `java.util.logging.Logger`. Call sites hold
one instance per class:

```java
private static final Log LOG = Log.of(EntityInfantry.class);
```

There is no stack introspection to derive the category, so logging from a hot
path costs no more than the string concatenation it already does.

### 1.2 Level model

| Facility level | JUL `Level`   | Visible at default (`INFO`)? |
|----------------|---------------|------------------------------|
| `ERROR`        | `SEVERE`      | yes                          |
| `WARN`         | `WARNING`     | yes                          |
| `INFO`         | `INFO`        | yes                          |
| `DEBUG`        | `FINE`        | only at `-Djavara.log.level=DEBUG` |
| `TRACE`        | `FINER`       | only at `-Djavara.log.level=TRACE` |

JUL orders levels `FINER (400) < FINE (500) < INFO (800)`, and a logger accepts
a record when the record's level is at or above the logger's threshold. Mapping
`DEBUG` to `FINE` and `TRACE` to `FINER` therefore yields the expected
filtering with no extra logic: a logger at `DEBUG` accepts `FINE` and rejects
`FINER`.

`TRACE` is a legitimate home for per-frame state chatter in a real-time game,
so it is part of the vocabulary even though the initial migration uses it in
only a few places.

### 1.3 Category

The JUL logger name is the **full class name**, which gives the finest
possible control (`-Djavara.log.level.cr0s.javara.ai.BaseBuilder=TRACE`).

The console shows a short category derived from that name: the first segment
after `cr0s.javara.`.

| Logger name                                    | Category |
|------------------------------------------------|----------|
| `cr0s.javara.ai.BaseBuilder`                    | `ai`     |
| `cr0s.javara.render.map.TileMap`                | `render` |
| `cr0s.javara.entity.actor.activity.activities.Drag` | `entity` |
| `cr0s.javara.resources.ResourceManager`         | `resources` |
| `cr0s.javara.combat.Combat`                     | `combat` |
| `cr0s.javara.ui.GameSideBar`                    | `ui`     |

A logger name outside `cr0s.javara.` falls back to its last segment. The
derivation lives in one place (`LogFormatter`) and is covered by unit tests.

### 1.4 Output format

```
14:32:05.123 ERROR [resources] Mix file tileset.mix is not found
14:32:05.124 WARN  [ai] Cannot find common names for: Power
14:32:05.130 INFO  [render] Map size: 64 x 64
14:32:05.131 DEBUG [ai] Building power plant: PowerPlant
14:32:05.131 TRACE [entity] Attacking
```

- **Timestamp** — `HH:mm:ss.SSS`, local time.
- **Level** — left-padded to exactly 5 characters (`ERROR`, `WARN `, `INFO `,
  `DEBUG`, `TRACE`) so the category column never shifts between lines.
- **Category** — bracketed, as derived in 1.3.
- **Message** — the caller's text, unchanged apart from the removals noted in
  section 2.
- **Throwable** — when a cause is attached, its stack trace follows the
  message line.

### 1.5 `cr0s.javara.util.LogFormatter`

A `java.util.logging.Formatter` that renders a `LogRecord` in the shape above.
It owns the level-name mapping (including `CONFIG`→`DEBUG` and
`FINEST`→`TRACE` for completeness) and the category derivation.

### 1.6 Runtime control

| Property | Effect |
|----------|--------|
| `javara.log.level` | Global threshold. One of `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. Default `INFO`. |
| `javara.log.level.<logger-name>` | Per-subtree override. The suffix is any JUL logger name, e.g. `javara.log.level.cr0s.javara.ai=DEBUG`. |

A `javara-log.properties` file in the working directory may supply the same
keys. System properties take precedence over the file, and the file takes
precedence over the defaults.

Configuration reads, in order: default threshold `INFO` → `javara-log.properties`
(if present) → system properties. Each `javara.log.level.<name>` sets the
level on the JUL logger of that name, which applies to its whole subtree
through JUL's hierarchy.

### 1.7 Handler setup

A single `ConsoleHandler` is installed on the root logger with `LogFormatter`
attached, and `setUseParentHandlers(false)` is set. JUL's default root handlers
are removed so that output is not duplicated or rendered in JUL's default
format.

`Log.configure()` is idempotent and is invoked from `Log`'s static
initialiser, so logging works correctly regardless of the order in which
classes are first touched. `GUI` may also call it explicitly at startup.

**Ordering constraint.** JUL fixes a logger's parent at creation time by
walking up its name. A per-subtree override therefore only reaches the class
loggers beneath it if the subtree logger already exists when they are created.
`Log`'s static initialiser runs `configure()` before any `Log.of(...)` call
can create a class logger, and `configure()` itself creates the subtree
loggers named by `javara.log.level.<name>`. This ordering is what makes
`-Djavara.log.level.cr0s.javara.ai=DEBUG` reach `cr0s.javara.ai.BaseBuilder`,
and it must be preserved: nothing may create a `java.util.logging.Logger`
directly, bypassing `Log`.

### 1.8 Module change

`src/module-info.java` gains:

```java
requires transitive java.logging;
```

`transitive` is required because `cr0s.javara.util` is an exported package and
`LogFormatter` extends `java.util.logging.Formatter`. Consumers of the module
(such as the `test` module) must be able to read that supertype without
declaring `requires java.logging` themselves.

---

## 2. Migration

### 2.1 `System.out` / `System.err` call sites

All 31 live call sites in `cr0s.javara` are migrated. The 19 commented-out
ones are left as they are.

| File:line | Level | New message |
|---|---|---|
| `combat/Combat.java:103` | ERROR | `Weapon {class} trying to impact without warheads` |
| `entity/effect/TeslaZapRenderer.java:147` | ERROR | `Sprite limit reached` |
| `entity/building/common/EntityWall.java:41` | ERROR | `Wall texture not found: {textureName}` |
| `entity/actor/activity/activities/Drag.java:25` | DEBUG | `Drag is cancelled` |
| `entity/infantry/EntityInfantry.java:184` | TRACE | `Attacking` |
| `resources/RemappedTextureCache.java:65` | WARN | `Remapped texture cache: image already cached` |
| `resources/ResourceManager.java:222` | ERROR | `Record SHP ({name}) in {tileSetName}.mix is not found` |
| `resources/ResourceManager.java:226` | ERROR | `Mix file {tileSetName}.mix is not found` |
| `resources/ResourceManager.java:258` | ERROR | `Template mix file {type}.mix is not found` |
| `resources/ResourceManager.java:322` | WARN | `Hi-res texture {name} not found` |
| `resources/SoundManager.java:73` | ERROR | `Sound file not found: {filePath}` |
| `render/World.java:306` | DEBUG | `Spawned entity {name}` |
| `render/EntityBlockingMap.java:135` | ERROR | `Null subcell` |
| `render/map/TileMap.java:119` | ERROR | `Invalid map` |
| `render/map/TileMap.java:135` | INFO | `Map size: {w} x {h}` |
| `render/map/TileMap.java:275` | TRACE | `Map entity texture: {name}` |
| `render/map/TileSet.java:58` | INFO | `Loaded tileSet: {path}` |
| `render/map/TheaterCache.java:195` | ERROR | `Unable to add texture: {name}` |
| `ui/GameSideBar.java:221` | TRACE | `Mouse event: {event}` |
| `ui/GameSideBar.java:540` | DEBUG | `Button {button} clicked at {x},{y}` |
| `ui/sbpages/SideBarPage.java:67` | TRACE | `Button click at {x},{y}` |
| `ui/sbpages/SideBarPage.java:72` | DEBUG | `Found button: {description}` |
| `ai/BaseBuilder.java:74` | WARN | `Cannot find common names for: {commonName}` |
| `ai/BaseBuilder.java:110` | DEBUG | `Checking building limit for {building}: {count}/{limit}` |
| `ai/BaseBuilder.java:124` | INFO | `Building power plant: {name}` |
| `ai/BaseBuilder.java:133` | INFO | `Building ore refinery` |
| `ai/BaseBuilder.java:143` | INFO | `Building production: {name}` |
| `ai/BaseBuilder.java:153` | INFO | `Building ore silo` |
| `ai/BaseBuilder.java:182` | INFO | `Building power plant: {name}` |
| `ai/BaseBuilder.java:188` | INFO | `Building: {name}` |
| `ai/AIPlayer.java:809` | DEBUG | `Cash: {cash}` |

Notes on the judgment calls:

- The ad-hoc prefixes (`[AI]`, `[SBP]`, `[MAP]`, `ERR:`, `HIRES:`,
  `[Warning]`, `[BUG]`) are **removed**. The category column now carries that
  information. `[BUG]` in particular is redundant with the `ERROR` level.
- `BaseBuilder`'s "Building X" lines are `INFO`, not `DEBUG`: they fire once
  per actual construction decision and answer "why did the AI build that?",
  which is the single most useful thing to observe during play. The
  per-candidate "Checking building limit" line is `DEBUG` because it fires on
  every candidate on every decision round.
- `ResourceManager:322` (missing hi-res texture) is `WARN` rather than
  `ERROR`, because the game falls back to the low-res texture. Every other
  missing required asset is `ERROR`.
- `TheaterCache:195` was annotated `// debug` by the original author, but it
  means the sprite was never placed in the sheet and the entity will render
  wrong, so it is `ERROR`.
- `AIPlayer:809` printed a bare `getCash()` number with no label; it becomes
  `Cash: {cash}` at `DEBUG`.

### 2.2 `printStackTrace()` call sites

All 19 live `printStackTrace()` calls in `cr0s.javara` become
`LOG.error(message, cause)`, which attaches the exception to a labelled
`ERROR` instead of emitting a bare stack trace.

| File:line | Message |
|---|---|
| `ui/cursor/CursorManager.java:111` | `Failed to update cursor` |
| `ui/GameSideBar.java:99` | `Failed to load sidebar button sheet` |
| `entity/vehicle/EntityVehicle.java:86` | `Failed to instantiate vehicle {name}` |
| `resources/ResourceManager.java:103` | `Failed to load mix file {path}` |
| `resources/ResourceManager.java:184` | `Failed to decode sound {name}` |
| `resources/ResourceManager.java:274` | `Failed to load palette {name}` |
| `resources/ResourceManager.java:350` | `Failed to load texture {name}` |
| `resources/ResourceManager.java:357` | `Failed to close file for {name}` |
| `render/map/TileMap.java:170` | `Failed to load map resources` |
| `render/map/TheaterCache.java:74` | `Failed to create sprite sheet` |
| `render/map/TheaterCache.java:176` | `Failed to copy texture into sprite sheet` |
| `render/map/TheaterCache.java:226` | `Failed to copy texture into sprite sheet` |
| `render/map/TheaterCache.java:263` | `Failed to copy texture into sprite sheet` |
| `render/map/MinimapRenderer.java:36` | `Failed to initialise minimap` |
| `render/map/MinimapRenderer.java:105` | `Failed to render minimap` |
| `ai/AIPlayer.java:224` | `Failed to load AI configuration` |
| `ai/AIPlayer.java:230` | `Failed to load AI configuration` |
| `main/StateMainMenu.java:120` | `Failed to load main menu textures` |
| `main/StatePauseMenu.java:127` | `Failed to load pause menu textures` |

The stack trace is preserved: `LogFormatter` renders it beneath the message
line.

---

## 3. Retiring `GUI.DEBUG_MODE`

Verified before deciding: every one of the 17 live use sites across 17 files
has a body that is entirely commented-out Slick `Graphics` render code, left
behind when the game moved from Slick to JavaFX. `MobileEntity.drawPath(Scene g)`
is the only site with a live guard, and its body is also entirely commented out
— and it has zero live callers (all six call sites are `//drawPath(g);`).

The flag therefore does nothing at runtime today.

Actions:

1. Delete `public static boolean DEBUG_MODE = false;` from
   `cr0s/javara/main/GUI.java`.
2. Delete all 17 live `if (GUI.DEBUG_MODE) { ... }` blocks. Runtime no-op,
   since every body is a comment. Delete the two commented-out references to
   the field as well (`render/World.java`, `entity/building/common/EntityConstructionYard.java`),
   so no stale mention of the removed field survives.
3. Delete `MobileEntity.drawPath(Scene g)` entirely, along with the six stale
   `//drawPath(g);` comments that reference it.
4. Remove the now-unused `import cr0s.javara.main.GUI;` from the 16 files
   that referenced `GUI` only for the flag. Files that use `GUI` for other
   members (`GUI.getInstance()`, and so on) keep the import.

No compile-time switch remains. Verbosity is a runtime decision.

---

## 4. Testing and verification

### 4.1 Unit tests

New file `test/test/LogTest.java` (package `test`), following the existing
JUnit 4 conventions in `test/test/`.

| # | Test | Assertion |
|---|------|-----------|
| 1 | Level mapping | `error`→`SEVERE`, `warn`→`WARNING`, `info`→`INFO`, `debug`→`FINE`, `trace`→`FINER` |
| 2 | Format shape | `LogFormatter.format(record)` returns exactly `HH:mm:ss.SSS LEVEL [category] message` |
| 3 | Level padding | The level field is always 5 characters, so the category column does not shift |
| 4 | Category derivation | `cr0s.javara.ai.BaseBuilder`→`ai`; `cr0s.javara.render.map.TileMap`→`render`; `cr0s.javara.entity.actor.activity.activities.Drag`→`entity`; a name outside `cr0s.javara.` falls back to its last segment |
| 5 | Throwable rendering | A record with an attached cause renders the message line *and* the stack trace |
| 6 | Level filtering | A logger at `INFO` drops `debug`/`trace` and keeps `info`; at `DEBUG` it keeps `debug` but drops `trace`; at `TRACE` it keeps all |
| 7 | Property wiring | `javara.log.level` sets the global threshold; `javara.log.level.cr0s.javara.ai` overrides one subtree only |

Mechanism: attach a recording `Handler` to the logger under test and assert on
the captured `LogRecord`s; call `LogFormatter.format(record)` directly and
assert on the returned string. No stdout capture, no ordering flakiness.

The `test` module needs no change: `requires transitive java.logging` on the
main module makes the types visible.

### 4.2 Build and test commands

The NetBeans `build.xml` cannot be used as-is (see *Aside*), so verification
uses direct `javac` / `java` invocations. This recipe was confirmed to compile
both modules and run the existing suite on this machine:

```bash
cd JavaRA
JDK=/opt/homebrew/opt/openjdk@25/bin
MP="/Users/xuyi/opt/javafx-sdk-25.0.2/lib:/Users/xuyi/Source/Java/lib/snakeyaml-2.4.jar:../assets/dist/assets.jar"
JUNIT=/Users/xuyi/Source/Java/JML/lib/junit-4.12.jar
HAMCREST=/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar
JSON=/Users/xuyi/Source/Java/lib/json-20200518.jar

rm -rf out/main out/test && mkdir -p out/main out/test
$JDK/javac -d out/main --module-path "$MP" -nowarn $(find src -name '*.java')
$JDK/javac -d out/test --module-path "$MP:out/main:$JUNIT:$HAMCREST:$JSON" -nowarn $(find test -name '*.java')
$JDK/java --module-path "$MP:out/main:out/test:$JUNIT:$HAMCREST:$JSON" \
    --add-modules test,junit -m junit/org.junit.runner.JUnitCore \
    test.LogTest test.PosTest test.PointsUtilTest test.RotationUtilTest \
    test.AStarPathFinderTest test.UtilsTest test.ClassTest test.SoundManagerTest
```

**Baseline measured before the change: `OK (76 tests)`.** After the change the
same command must report `76 + N` passing with zero regressions.

Additional mechanical check — no stray output statements left in `cr0s.javara`:

```bash
grep -rn "System\.out\|System\.err\|printStackTrace" src/cr0s/
```

After the migration every remaining hit must be inside a comment. The hits that
remain are pre-existing commented-out code (19 `System.out`/`System.err`
sites, 1 `printStackTrace`, and the two commented `DEBUG_MODE` blocks), which
is expected.

### 4.3 Manual smoke test

Run the game at each level and confirm:

- `INFO` — only errors, warnings and lifecycle information; no per-tick noise.
- `DEBUG` — AI decisions, entity spawns, button interactions.
- `TRACE` — per-frame state traces.
- `-Djavara.log.level.cr0s.javara.ai=DEBUG` — raises verbosity for the AI
  subtree only; other subsystems stay at `INFO`.
- Every line matches the format in 1.4, and no line appears twice (JUL's
  default handler must not also be printing).

---

## 5. Files touched

**Added**

- `src/cr0s/javara/util/Log.java`
- `src/cr0s/javara/util/LogFormatter.java`
- `test/test/LogTest.java`

**Modified**

- `src/module-info.java` — add `requires transitive java.logging;`
- 31 files containing migrated `System.out` / `System.err` call sites
- 19 files containing migrated `printStackTrace()` call sites
- 17 files containing live `if (GUI.DEBUG_MODE)` blocks, plus 2 with
  commented-out ones
- `src/cr0s/javara/main/GUI.java` — remove the `DEBUG_MODE` field
- `src/cr0s/javara/entity/MobileEntity.java` — remove `drawPath`
- 16 files — remove a now-unused `import cr0s.javara.main.GUI;`
- `README.md` — add a short "Debug output" section documenting the
  `-Djavara.log.level` property, so the knob is discoverable without reading
  the source

**Untouched**

- `src/net/pso/jswarm/**`
- `src/mazesolver/**`
- `src/cr0s/javara/**` debug rendering code that carries no output

---

## Aside: unrelated broken build paths

`nbproject/project.properties` sets
`file.reference.vecmath.jar=/home/spy/Source/host/lib/lib/ext/vecmath.jar`,
a path from another machine, and `run.jvmargs` references
`javafx-sdk-17.0.0.1`, which is not installed here. Nothing in `src/` or
`test/` imports `javax.vecmath`, so removing that one classpath entry would
unblock `ant build` on this machine.

This is out of scope for the logging change and is recorded here only so it
is not rediscovered from scratch.
