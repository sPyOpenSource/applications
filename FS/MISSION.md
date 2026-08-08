# Mission: USB Mass Storage for File System Implementation

## Why

I want to implement a file system (FAT/exFAT) on top of USB mass storage. To do this, I need to understand how to communicate with USB mass storage devices at the block level — sending SCSI commands over USB to read/write sectors.

## Goal

Be able to write code that:
1. Enumerates a USB mass storage device
2. Sends SCSI commands (INQUIRY, READ CAPACITY, READ(10), WRITE(10), etc.)
3. Reads/writes raw blocks/sectors from the device
4. Uses this as the block device layer for a FAT/exFAT implementation

## Context

- Target: Embedded systems (likely ARM Cortex-M)
- Role: USB host (not device)
- Language: C/C++
- May use existing USB stack (TinyUSB, USB Host stack in Zephyr/ChibiOS, or custom)

## Success Criteria

- [ ] Understand USB mass storage class specification (Bulk-Only Transport)
- [ ] Understand SCSI Primary Commands (SPC) and Block Commands (SBC) relevant to mass storage
- [ ] Can implement a block device driver that translates file system read/write to SCSI commands
- [ ] Can handle common edge cases (media change, write protect, errors)
- [ ] Have reference implementation to study