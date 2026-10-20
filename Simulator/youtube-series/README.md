# Build an x86 CPU Emulator in Java — Season 1

Beginner-friendly, 5-episode YouTube series. Each episode ~7 minutes, screen recording.

## Files
- `00_series_bible.md` — brand, format, arc, production notes
- `episodes/ep01_what_is_cpu_emulator.md`
- `episodes/ep02_registers_memory.md`
- `episodes/ep03_fetch_decode_execute.md`
- `episodes/ep04_mov_add.md`
- `episodes/ep05_io_interrupts.md`

## Episode index
| # | Title | Tag |
|---|---|---|
| 1 | What Is a CPU Emulator? (Java Setup) | ep01 |
| 2 | Registers & Memory in Java | ep02 |
| 3 | The Fetch-Decode-Execute Loop | ep03 |
| 4 | Your First Real Instructions (MOV, ADD) | ep04 |
| 5 | I/O, Interrupts & Running a Program | ep05 |

## Deliverables per episode
Script (spoken word), timestamps, thumbnail concept, YouTube description, tags, production notes, final code state, "Try it" exercise.

## Companion repo
`github.com/<handle>/java-x86-emulator` with git tags `ep01`..`ep05`.

## Local checkpoints
`checkpoints/ep01` .. `checkpoints/ep05` contain the exact code state at the end of each episode (compiles and runs). Use them to follow along or to recover if you get stuck.

## Final project
`java-x86-emulator/` is the Season 1 finale project (Ant). Run with:
```
ant compile exec:java
```
