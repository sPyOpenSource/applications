#!/bin/sh
# Build test disk images for the headless UEFI boot runner:
#   hello.img - GPT disk, FAT ESP with our own hello.c as \EFI\BOOT\BOOTX64.EFI
#   grub.img  - same layout with a GRUB x64 EFI binary (a large, real-world EFI app)
# Needs: clang, lld-link, sgdisk, mtools, grub-mkimage (grub-efi-amd64-bin).
# Usage: make-image.sh <output-dir>
set -e
src=$(cd "$(dirname "$0")" && pwd)
out=${1:?output directory}
mkdir -p "$out"

clang -target x86_64-unknown-windows -ffreestanding -fno-stack-protector -mno-red-zone \
    -mno-sse -O1 -fuse-ld=lld-link -nostdlib \
    -Wl,-entry:efi_main -Wl,-subsystem:efi_application \
    -o "$out/hello.efi" "$src/hello.c"

grub-mkimage -O x86_64-efi -p /EFI/BOOT -o "$out/grub.efi" normal echo

for name in hello grub; do
    img="$out/$name.img"
    rm -f "$img"
    truncate -s 40M "$img"
    sgdisk -n 1:2048:0 -t 1:ef00 -c 1:ESP "$img" >/dev/null
    # partition starts at sector 2048 = byte offset 1 MiB
    mformat -i "$img@@1M" -F -v ESP ::
    mmd -i "$img@@1M" ::/EFI ::/EFI/BOOT
    mcopy -i "$img@@1M" "$out/$name.efi" ::/EFI/BOOT/BOOTX64.EFI
done
ls -l "$out"
