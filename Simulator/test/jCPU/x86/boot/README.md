# Headless UEFI boot

`UefiBootRunner` does what the GUI does for *attach disk image* → *UEFI Boot*
→ *Go*, without Swing and without `jCPU.x86.CPU` (which needs `jx.compiler`
from another repository). Firmware and EFI console output appear on stdout;
an unknown instruction stops the run with the instruction history.

## Test images

`make-image.sh <dir>` builds two GPT disks with a FAT32 EFI system partition:

- `hello.img`: `hello.c`, a small x64 EFI application that prints through
  `ConOut->OutputString` and returns
- `grub.img`: a GRUB x64 EFI binary from `grub-mkimage`, as a large
  real-world application

It needs clang, lld-link, sgdisk, mtools and grub-efi-amd64-bin.

## Running

From `Simulator/`:

```sh
javac -d /tmp/boot -sourcepath src \
    src/jCPU/x86/X86*.java src/jCPU/x86/i8086.java src/jCPU/MCS51/graph/RESERVED.java \
    test/jCPU/x86/boot/UefiBootRunner.java
java -cp /tmp/boot jCPU.x86.UefiBootRunner hello.img [maxSteps]
```

Standard input is the keyboard (through the UART), so commands can be
typed or piped in:

```sh
printf 'echo hello\n' | java -cp /tmp/boot jCPU.x86.UefiBootRunner grub.img
```

When the step limit is reached, the runner prints the addresses of the
loop the program is in.

`RESERVED.java` is listed explicitly because its package (`j51.intel.graph`)
does not match its directory. Exit code 0 means the EFI application exited
with EFI_SUCCESS.
