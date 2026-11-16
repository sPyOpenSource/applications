# Reference results for the x86-64 executor

`x86_64.golden` holds instruction snippets together with the registers, flags
and memory the real CPU produces for them. `X86Amd64GoldenTest` runs each
snippet in the emulator and compares.

- `gen_golden.py` lists the snippets, assembles them, runs them and writes `x86_64.golden`.
- `runner.c` executes a snippet on the real CPU.
- `functions.c` holds C functions that are compiled at `-O0`, `-O1`, `-O2` and `-Os` and run as snippets.

Regenerating needs an x86-64 Linux machine with gcc and binutils:

    python3 gen_golden.py

Running the test needs none of this; it only reads `x86_64.golden`.
