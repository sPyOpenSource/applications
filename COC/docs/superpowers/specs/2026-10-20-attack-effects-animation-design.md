# Design: Attack Effects Animation Improvements

Date: 2026-10-20
Status: Approved (design); implementation plan pending

## Problem

The Clash of Clans JavaFX game uses `PathTransition` for unit movement and
projectiles, plus hand-rolled line/path nodes for defender beams. Several effects
are visually inconsistent or leak scene-graph nodes:

- Projectile travel duration and controller wait loops can desync, causing
  projectiles to vanish or units to attack before arrival.
- Impact points have no visible feedback.
- Defender beams sometimes leave stale nodes or drift off target.
- Damaged buildings give no hit feedback.

## Goal

Improve attack-effect readability and consistency using only JavaFX shapes/colors
(no new image/audio assets), while keeping the existing threading model and
controller structure intact.

## Constraints

- No new image or audio files.
- Do not rewrite combat algorithms or unit stats.
- Keep all scene-graph mutations on the JavaFX Application Thread.
- Reuse existing controllers; add small helper methods where helpful.

## Chosen approach

Targeted polish of existing effects rather than a full animation-engine rewrite.
This keeps changes small, reviewable, and low-risk while addressing the most
visible animation issues.

## Components

### 1. Projectile motion (ranged units)

Units affected: `ArcherBalloonAttack`, `DefBalloonAttack`, `GoblinBalloonAttack`,
`RangedUnitAttack` (Wizard), and any future ranged controller.

- Keep `PathTransition` for travel.
- Ensure duration and wait loop use the same distance/speed factor.
- Remove the projectile node on transition completion.
- Guard removal against destroyed targets.

### 2. Impact flashes

- Spawn a small `Circle` at the impact point when a projectile lands or a melee
  strike connects.
- Fade out via `FadeTransition` (or opacity change + `PauseTransition`).
- Remove the node after fade completes.
- Color-code by unit type for clarity.

### 3. Tower beams (Tesla / Archer / Inferno)

- Keep the existing line/path approach.
- Remove previous beam/impact node before creating the next.
- Recompute endpoints against the target's current center each shot.
- Use distinct stroke colors/widths per tower type.

### 4. Building hit feedback

- On HP decrease, apply a brief scale pulse or opacity flash to the building's
  `ImageView`.
- Restore to normal after the pulse.
- Avoid stacking effects when hits arrive quickly.

## Data flow

1. Attack controller computes target and path.
2. `Platform.runLater` spawns projectile/effect node and plays transition.
3. On completion, controller removes the node and spawns an impact flash.
4. Defender controllers update beam endpoints each shot and remove stale nodes.
5. Building HP changes trigger a short visual pulse.

## Error handling

- All scene-graph edits occur on the FX thread.
- Effect nodes are removed even if the target/building is destroyed mid-animation.
- Controllers check `isDead()` before spawning new effects.

## Testing

- Compile all modules (`ant compile`).
- Manual gameplay check: projectiles land and disappear, impacts flash, beams do
  not leave stale nodes, buildings pulse on hit.
- No regressions in existing troop behavior.

## Out of scope

- New art/audio assets.
- Rewriting combat logic or scoring.
- Replacing the threading model with `AnimationTimer`.