# Series Bible — "Build an x86 CPU Emulator in Java"

## Channel / Brand
- **Series name:** Build an x86 CPU Emulator in Java
- **Tagline:** "From zero to running code — in Java, in 7 minutes."
- **Host persona:** Friendly, patient teacher. Talks to camera, then cuts to screen recording. No jargon without a plain-English gloss.
- **Format:** Screen recording (IDE + terminal). Picture-in-picture webcam optional. 16:9, 1080p/60fps.
- **Episode length:** ~7 minutes (target 6:30–7:30). Script budget: ~950–1,100 spoken words.
- **Audience:** Absolute beginners to emulation AND to low-level computing. Assumes basic Java (classes, methods, arrays) only.
- **Cadence:** Weekly drop, 5 episodes = Season 1. End each episode with a "next episode" hook and a downloadable checkpoint (Git tag).

## Visual Language
- IDE: IntelliJ IDEA (dark theme, large font 16pt).
- Terminal: zsh with prompt `emulator $`.
- Overlays: big red "REGISTERS" box, memory hex dump panel, step counter.
- Color code: registers = cyan, memory = green, opcodes = yellow, errors = red.
- Intro (5s): animated CPU die → "JAVA x86 EMULATOR" title card.
- Outro (8s): "Subscribe + next episode" + repo link.

## Repository / Checkpoints
- Repo: `github.com/<handle>/java-x86-emulator`
- Branch: `main`. Tag per episode: `ep01` … `ep05`. Viewers can `git checkout ep03` to follow along.
- Build: Maven (`mvn compile`, `mvn exec:java`).

## Season 1 Arc (5 episodes)
| # | Title (draft) | Core concept | Deliverable at end |
|---|---|---|---|
| 1 | What Is a CPU Emulator? (Java Setup) | Emulation basics + project skeleton | Empty `Cpu` class runs |
| 2 | Registers & Memory in Java | Registers, flags, byte-addressable RAM | `Cpu` reads/writes memory |
| 3 | The Fetch-Decode-Execute Loop | Main loop, opcode dispatch | Loop prints fetched bytes |
| 4 | Your First Real Instructions (MOV, ADD) | Implement opcodes | Tiny program computes 2+3 |
| 5 | I/O, Interrupts & Running a Program | Console I/O, timer IRQ, load binary | Demo program prints "Hi" |

## Recurring Segments
- **"Plain English"** (10s): one sentence defining a term before using it.
- **"Checkpoint"** (5s): show git tag + repo link.
- **"Try it"** (15s): one exercise for viewers to do before next episode.

## Success Metrics / CTAs
- Pin a comment with the repo link + episode timestamps.
- End screen: next episode + "Java for Beginners" playlist.
- Pinned resource: cheat sheet PDF (registers, opcodes table).

## Notes for Production
- Record screen at 1920x1080, zoom IDE to 150%.
- Keep code on screen ≤ 25 lines; scroll slowly.
- Speak at ~140 wpm; pause 1s after each new concept.
- Every code change must compile before cutting. No "trust me" edits.
