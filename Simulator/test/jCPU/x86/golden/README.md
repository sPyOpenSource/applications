# Reference results for the x86-64 executor

`x86_64.golden` (64-bit code) and `x86_32.golden` (32-bit protected mode)
hold instruction snippets together with the registers, flags and memory the
real CPU produces for them. `X86Amd64GoldenTest` runs each snippet in the
emulator and compares.

- `gen_golden.py` lists the snippets, assembles them, runs them and writes both files.
- `runner.c` executes a snippet on the real CPU. With `--32` it runs 32-bit
  code in compatibility mode, through a far jump to the kernel's 32-bit user
  code segment.
- `functions.c` holds C functions that are compiled at `-O0`, `-O1`, `-O2` and
  `-Os`, for x86-64 and with `gcc -m32`, and run as snippets.

Regenerating needs an x86-64 Linux machine with gcc (including `-m32`
support) and binutils:

    python3 gen_golden.py

Running the test needs none of this; it only reads the `.golden` files.
