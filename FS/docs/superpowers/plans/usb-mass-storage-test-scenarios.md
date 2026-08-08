# USB Mass Storage Additional Test Scenarios

## Overview
These scenarios extend the basic mount+RW test (`USBMassStorageTest`) to cover edge cases, error paths, and advanced functionality of the USB Mass Storage BOT implementation.

---

## Scenario 1: Media Change Detection (Hot-plug / Eject Handling)

**Objective**: Verify `processChanged()` correctly detects media removal/insertion and re-reads capacity.

**Setup**: 
- QEMU with `-device usb-storage,drive=hd0,removable=on`
- USB stick with multiple partitions or no media initially

**Steps**:
1. Boot with no media → expect `NOT_READY/0x3A` on first read attempt
2. Insert media (QEMU monitor: `change usb-hd0 /tmp/usbtest2.img`)
3. Issue read → should trigger `UNIT_ATTENTION/0x28` → `processChanged()` re-reads capacity → succeeds
4. Eject media (`eject usb-hd0`)
5. Read again → `NOT_READY/0x3A`

**Expected**: 
- `USBStorageSCSIDriver.processChanged()` sets `changed=true`
- Next operation catches `SCSICommandFailedException`, runs auto-REQUEST SENSE
- Sense key `UNIT_ATTENTION` ASC 0x28 → retries → re-reads capacity
- Sense key `NOT_READY` ASC 0x3A → returns EOF/IOException

**Test class**: `test.fs.MediaChangeTest`

---

## Scenario 2: Multi-Partition Support

**Objective**: Verify the driver can access multiple partitions on a single USB drive.

**Setup**: 
- Disk image with MBR: 2 FAT32 partitions (e.g., 32MB each)
- `fdisk /tmp/usbtest.img` → create p1, p2 → `mkfs.vfat -F 32` each

**Steps**:
1. Mount partition 1 via `USBStorageBlockIO` (reads LBA from partition table)
2. Write file to partition 1
3. Mount partition 2 (separate `USBStorageBlockIO` with partition offset)
3. Write different file to partition 2
4. Verify isolation

**Note**: Current `USBStorageBlockIO` reads entire device capacity. Need partition-aware variant or `USBStorageSCSIDriver` to expose partition table.

**Test class**: `test.fs.MultiPartitionTest`

---

## Scenario 3: Large File / Multi-Sector Transfer

**Objective**: Verify chunked transfers (>64KB) and partial-sector handling work correctly.

**Setup**: 
- Standard 64MB FAT32 image

**Steps**:
1. Create 200KB file (exceeds `MAX_BOT_TRANSFER` = 64KB → multiple chunks)
2. Write via `USBStorageBlockIO.writeSectors` (spans multiple BOT transactions)
3. Read back and byte-compare
4. Test exact 64KB boundary (chunk alignment)
5. Test file size not multiple of sector size (512B) → verify zero-padding in last sector

**Expected**:
- `USBStorageBulkTransport.transport()` splits data phase at `MAX_BOT_TRANSFER`
- `USBStorageSCSIDriver.write()` zero-pads partial last sector
- Read returns exact bytes written (no garbage in padding)

**Test class**: `test.fs.LargeFileTest`

---

## Scenario 4: Read-Only Media / Write Protect

**Objective**: Verify write-protect handling (sense key `DATA_PROTECT`/ASC 0x27).

**Setup**: 
- QEMU with `-device usb-storage,drive=hd0,readonly=on`
- Or physical USB stick with write-protect switch

**Steps**:
1. Mount read-write
2. Attempt create/write → expect `SCSICommandFailedException` with status 0x02 (WRITE_PROTECT)
3. Verify `processChanged()` doesn't retry writes
4. Read-only operations (read) should succeed

**Expected**: 
- `USBStorageSCSIDriver.write()` catches exception, sense key `DATA_PROTECT` (0x07) ASC 0x27
- Returns appropriate IOException

**Test class**: `test.fs.ReadOnlyMediaTest`

---

## Scenario 5: Error Injection (USB Stalls, CSW Failures)

**Objective**: Verify auto-REQUEST SENSE and retry logic on transient errors.

**Setup**: 
- QEMU with fault injection (if supported) or modified UHCI to inject stalls
- Or patch `USBStorageBulkTransport` to simulate failures

**Steps**:
1. Inject stall on bulk OUT (data phase) → expect `USBException` → clear halt → retry
2. Inject CSW status = 0x01 (command failed) → auto-REQUEST SENSE → retry once
3. Inject CSW status = 0x02 (phase error) → reset transport → retry once
4. Inject repeated failures → eventual propagation

**Note**: Hard to test without USB fault injection framework. Could mock in unit test.

**Test class**: `test.fs.ErrorInjectionTest` (unit test with mock pipes)

---

## Scenario 6: Concurrent Read/Write (Stress)

**Objective**: Verify no corruption under concurrent access.

**Setup**: 
- Mounted FAT fs
- Two threads: one writing sequentially, one reading randomly

**Steps**:
1. Writer: append 1KB records in loop
2. Reader: read random records, verify checksum
3. Run for 1000 iterations
3. Verify no data corruption

**Note**: JNode's FS layer may not be thread-safe; this tests driver + FS interaction.

**Test class**: `test.fs.ConcurrentRWTest`

---

## Scenario 7: Synchronize Cache / Flush

**Objective**: Verify `SYNCHRONIZE CACHE` (0x35) is issued and data persists.

**Setup**: 
- Mounted FAT fs
- Write file, call `flush()` / `synchronizeCache()`
- Simulate power loss (kill QEMU) → remount → verify data

**Steps**:
1. Write "flush test" file
2. `bio.flush()` → `driver.flush()` → `MMCUtils.synchronizeCache()` → CDB 0x35
3. Kill QEMU process
4. Reboot with same image → verify file exists with correct content

**Test class**: `test.fs.FlushTest`

---

## Scenario 8: Performance Benchmark

**Objective**: Measure throughput and latency.

**Setup**: 
- 64MB FAT32 image
- Sequential read/write of varying block sizes (4KB, 64KB, 1MB)

**Metrics**:
- MB/s read/write
- Latency per 64KB chunk (BOT round-trip)
- CPU overhead

**Expected**: 
- UHCI full-speed (12 Mbps) → ~1 MB/s theoretical max
- BOT overhead ~10-15%

**Test class**: `test.fs.PerformanceTest` (extends IOZone pattern)

---

## Implementation Priority

| Priority | Scenario | Reason |
|----------|----------|--------|
| High | MediaChangeTest | Validates `processChanged()` + auto-sense |
| High | LargeFileTest | Validates chunking + zero-padding |
| Medium | ReadOnlyMediaTest | Validates sense key handling |
| Medium | MultiPartitionTest | Real-world use case |
| Low | ErrorInjectionTest | Requires mock framework |
| Low | ConcurrentRWTest | FS layer dependent |
| Low | FlushTest | Manual verification |
| Low | PerformanceTest | Informational |

---

## Test Infrastructure

All new tests follow `test.fs` pattern:
- `public static void init(Naming naming, String[] args)` — setup Debug
- `public static void main(String[] args)` — entry point
- Started as thread from `AI.java` (like `USBMassStorageTest`)

Register in `AI.java`:
```java
new Thread(() -> MediaChangeTest.main(null), "MediaChangeTest").start();
new Thread(() -> LargeFileTest.main(null), "LargeFileTest").start();
// etc.
```

---

## QEMU Command Additions

For media change testing:
```bash
qemu-system-x86_64 -cdrom MyOS.iso -serial stdio \
    -drive file=/tmp/usbtest.img,format=raw,if=none,id=hd0,removable=on \
    -device usb-storage,drive=hd0,removable=on
```

For read-only:
```bash
    -device usb-storage,drive=hd0,readonly=on
```

For multi-partition: use `drive=hd0,format=raw,if=none` with partitioned image.

---

## Acceptance Criteria Matrix

| Scenario | Criteria |
|----------|----------|
| MediaChange | `UNIT_ATTENTION/0x28` → re-read capacity → PASS; `NOT_READY/0x3A` → EOF |
| MultiPartition | Two independent `BlockIO` instances access correct LBA ranges |
| LargeFile | 200KB write/read exact byte match; partial sector zero-padded |
| ReadOnly | Write → IOException; Read → succeeds |
| ErrorInjection | Auto-REQUEST SENSE on fail; retry once; propagate on persistent |
| Concurrent | No data corruption after 1000 mixed ops |
| Flush | Data survives simulated power loss |
| Performance | > 500 KB/s on UHCI full-speed |

---

## Notes

- The static `mounted` flag in `USBStorageMount` limits to one device. For multi-device tests, either remove the flag or use separate portals (`USBFS1`, `USBFS2`).
- `FatFileSystem` FAT16 null bug (review finding #2) affects small images (<32MB). Use ≥64MB for FAT32.
- Endpoint direction swap risk (review finding) — verify BOT works before running advanced tests.
- ZLP risk — reads at 64KB boundary may hang if UHCI doesn't emit zero-length TD.