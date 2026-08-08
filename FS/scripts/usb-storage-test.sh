#!/usr/bin/env bash
# USB mass storage QEMU integration test for JNode (armOS fork).
#
# Creates a small FAT32 disk image pre-populated with hello.txt, then boots
# MyOS.iso under QEMU with the image attached as a USB mass storage device.
#
# JNode's AI boot domain registers USBStorageMount on every USB hub monitor;
# when the mass-storage device enumerates it mounts a FatFileSystem via the
# VFS (portals "USBFS"/"USBBlockIO") and USBMassStorageTest writes/reads a
# file, printing PASS/FAIL on the serial console (-serial stdio).
#
# Prerequisites:
#   - MyOS.iso built (see docs/superpowers/plans/...-implementation.md Task 10)
#   - qemu-system-x86_64, dd, mkfs.vfat, mount (sudo)
set -e

ISO="${1:-MyOS.iso}"
IMG="${2:-/tmp/usbtest.img}"
SIZE_MB=64
MOUNT_DIR=/tmp/usbmnt

if [ ! -f "$ISO" ]; then
    echo "error: $ISO not found (build it first, see plan Task 10 Step 3)" >&2
    exit 1
fi

echo "== Creating $SIZE_MB MB FAT32 image $IMG"
dd if=/dev/zero of="$IMG" bs=1M count=$SIZE_MB status=none
mkfs.vfat -F 32 "$IMG"

echo "== Pre-populating hello.txt"
mkdir -p "$MOUNT_DIR"
sudo mount -o loop "$IMG" "$MOUNT_DIR"
echo "Hello USB" | sudo tee "$MOUNT_DIR/hello.txt" > /dev/null
sudo umount "$MOUNT_DIR"

echo "== Booting $ISO with USB mass storage"
echo "Expected serial output:"
echo "  USBStorageMount: capacity N x 512 bytes"
echo "  USBStorageMount: FatFileSystem mounted ..."
echo "  USBMassStorageTest: wrote=10 read=10 contentOk=true"
echo "  USBMassStorageTest: PASS"
echo

qemu-system-x86_64 -cdrom "$ISO" -serial stdio -m 256M \
    -drive file="$IMG",format=raw,if=none,id=hd0 \
    -device usb-storage,drive=hd0
