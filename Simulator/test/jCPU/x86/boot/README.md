# Headless UEFI boot

`UefiBootRunner` does what the GUI does for *attach disk image* → *UEFI Boot*
→ *Go*, without Swing and without `jCPU.x86.CPU` (which needs `jx.compiler`
from another repository). Firmware and EFI console output appear on stdout;
an unknown instruction stops the run with the instruction history.

## Test images

`make-image.sh <dir>` builds disks with a FAT32 EFI system partition:

- `hello.img`: `hello.c`, a small x64 EFI application that prints through
  `ConOut->OutputString` and returns
- `grub.img`: a GRUB x64 EFI binary from `grub-mkimage`, as a large
  real-world application, with a `grub.cfg` it reads from the disk
- `grub-mbr.img`: the same with an MBR partition table instead of GPT

It needs clang, lld-link, sgdisk, sfdisk, mtools and grub-efi-amd64-bin.

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

For a session that runs the same way every time, give the keys in the
arguments; each is typed once its text has appeared in the output:

```sh
java -cp /tmp/boot jCPU.x86.UefiBootRunner grub.img 100000000 \
    --after 'grub> ' --type 'ls\n' --after 'grub> ' --type 'echo done\n'
```

When the step limit is reached, the runner prints the addresses of the
loop the program is in.

## The Java OS kernel

`make-jcore-image.sh <isodir> jcore.img` puts the boot files of the Java
OS (`app/isodir` in the sPyOpenSource/compiler repository: GRUB as
`EFI/boot/bootx64.efi`, `boot/grub`, `jxcore`, `code.zip`) on a GPT disk.
The same image boots in QEMU with OVMF, which shows what the emulator
should do:

```sh
qemu-system-x86_64 -machine q35 -m 512 -nic none -snapshot -serial stdio \
    -drive if=pflash,format=raw,readonly=on,file=OVMF_CODE_4M.fd \
    -drive if=pflash,format=raw,file=OVMF_VARS_4M.fd \
    -drive file=jcore.img,format=raw,if=ide
```

`RESERVED.java` is listed explicitly because its package (`j51.intel.graph`)
does not match its directory. Exit code 0 means the EFI application exited
with EFI_SUCCESS.
