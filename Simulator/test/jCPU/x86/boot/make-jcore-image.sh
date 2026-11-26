#!/bin/sh
# Build a GPT disk image that boots the Java OS kernel (jxcore) the way the
# ISO does: GRUB as \EFI\BOOT\BOOTX64.EFI, /boot/grub, /jxcore and /code.zip
# on a FAT32 EFI system partition.
# Usage: make-jcore-image.sh <isodir> <output.img>
#   <isodir>: compiler/app/isodir from the sPyOpenSource/compiler repository
# Needs: sgdisk, mtools.
set -e
iso=${1:?isodir}
img=${2:?output image}
size_mb=$(( $(du -sm "$iso" | cut -f1) + 64 ))
rm -f "$img"
truncate -s ${size_mb}M "$img"
sgdisk -n 1:2048:0 -t 1:ef00 -c 1:ESP "$img" >/dev/null
mformat -i "$img@@1M" -F -v JXCORE ::
mmd -i "$img@@1M" ::/EFI ::/EFI/BOOT ::/boot
mcopy -i "$img@@1M" "$iso/EFI/boot/bootx64.efi" ::/EFI/BOOT/BOOTX64.EFI
mcopy -s -i "$img@@1M" "$iso/boot/grub" ::/boot/
mcopy -i "$img@@1M" "$iso/jxcore" "$iso/code.zip" ::/
# The standalone GRUB in EFI/boot looks for /grub.cfg first and sets root
# to the partition it is on; /boot/grub/grub.cfg is also in its own memdisk.
mcopy -i "$img@@1M" "$iso/boot/grub/grub.cfg" ::/grub.cfg
ls -l "$img"
