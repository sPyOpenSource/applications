# USB Mass Storage Bulk-Only Transport Implementation Design

**Date**: 2026-08-07  
**Project**: JNode USB Mass Storage (Java)  
**Target**: Complete existing BOT implementation for VFS integration

---

## 1. Overview

Complete the partially implemented USB Mass Storage Bulk-Only Transport (BOT) in the JNode codebase (`org.jnode.driver.block.usb.storage.*`) so that USB mass storage devices can be mounted and used via the VFS (`FSImpl.java`).

### 1.1 Current State

The codebase has a well-structured foundation:
- `USBStorageBulkTransport` — CBW/CSW wrapper (no data phase)
- `CBW`/`CSW` — Packet structures (complete)
- `USBStorageSCSIHostDriver` — Device enumeration, INQUIRY working
- `USBStorageSCSIDevice` — SCSI command execution stub
- `MMCUtils` — READ CAPACITY, READ(10) utilities (partial)
- CDB classes for INQUIRY, TEST UNIT READY, REQUEST SENSE, READ(10), READ CAPACITY (stubs for `getDataTransfertCount()`)
- `SCSIBuffer` — All methods are stubs

### 1.2 Goals

1. Implement full BOT protocol with data phase (CBW → Data → CSW)
2. Fix all CDB classes with correct transfer counts
3. Implement `SCSIBuffer` Memory read/write
4. Add missing CDB classes: WRITE(10), SYNCHRONIZE CACHE(10), MODE SENSE/SELECT(6)
5. Complete `USBStorageSCSIDriver` block operations (read, write, flush, media change detection)
6. Integrate with VFS for mount/read/write

---

## 2. Design Decisions (Confirmed)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Block size | Read from READ CAPACITY | Devices may have 512, 4096, or other sector sizes |
| Large transfers | Split at 64KB max per BOT transaction | USB bulk endpoints and BOT spec limit per transaction |
| USB requests | Synchronous (`syncSubmit` with timeout) | Simpler error handling, matches existing pattern |
| VFS integration | `FSImpl.mount()` → `USBStorageSCSIDriver` as block device | Follows existing mount pattern |

---

## 3. Component Specifications

### 3.1 SCSIBuffer (`org.jnode.driver.bus.scsi.SCSIBuffer`)

**Purpose**: Big-endian byte buffer backed by `jx.zero.Memory`.

**Methods to implement**:
```java
// All multi-byte values are big-endian (MSB first)
void setInt8(int offset, int v)
int getUInt8(int offset)
void setInt16(int offset, int v)      // writes 2 bytes
int getUInt16(int offset)             // reads 2 bytes
void setInt32(int offset, int v)      // writes 4 bytes
int getInt32(int offset)              // reads 4 bytes
String getASCII(int offset, int length)
Memory toByteArray()
```

**Implementation**: Delegate to `Memory.set8()`, `Memory.get8()`, etc. with manual byte shifting for 16/32-bit.

---

### 3.2 CDB Classes — Fix `getDataTransfertCount()`

| Class | Opcode | Constructor | `getDataTransfertCount()` Returns |
|-------|--------|-------------|-----------------------------------|
| `CDBInquiry` | 0x12 | `CDBInquiry(int allocLen)` | `allocLen` |
| `CDBRequestSense` | 0x03 | `CDBRequestSense(int allocLen)` | `allocLen` |
| `CDBTestUnitReady` | 0x00 | `CDBTestUnitReady()` | `0` |
| `CDBReadCapacity` | 0x25 | `CDBReadCapacity()` | `8` (fixed response) |
| `CDBRead10` | 0x28 | `CDBRead10(int lba, int nrBlocks, int blockSize)` | `nrBlocks * blockSize` |
| `CDBWrite10` (new) | 0x2A | `CDBWrite10(int lba, int nrBlocks, int blockSize)` | `nrBlocks * blockSize` |
| `CDBSynchronizeCache10` (new) | 0x35 | `CDBSynchronizeCache10()` | `0` |
| `CDBModeSense6` (new) | 0x1A | `CDBModeSense6(int pageCode, int allocLen)` | `allocLen` |
| `CDBModeSelect6` (new) | 0x15 | `CDBModeSelect6(int paramLen)` | `paramLen` |

**Note**: `CDBRead10`/`CDBWrite10` need `blockSize` parameter added to constructor.

---

### 3.3 ITransport Interface

**Update signature**:
```java
public interface ITransport {
    void transport(CDB cdb, Memory data, int dataOffset, long timeout);
    void reset() throws USBException;
}
```

---

### 3.4 USBStorageBulkTransport — Full BOT Implementation

**Current**: Only sends CBW, receives CSW. No data phase.

**New `transport()` algorithm**:
```java
public void transport(CDB cdb, Memory data, int dataOffset, long timeout) {
    // 1. Build CBW
    CBW cbw = new CBW();
    cbw.setSignature(US_BULK_CB_SIGN);
    cbw.setTag(nextTag++);
    int dataLen = cdb.getDataTransfertCount();
    cbw.setDataTransferLength(dataLen);
    cbw.setFlags((byte) (data != null && isDataIn(cdb) ? US_BULK_FLAG_IN : US_BULK_FLAG_OUT));
    cbw.setLun((byte) 0);
    cbw.setLength((byte) cdb.toByteArray().size());
    cbw.setCdb(cdb.toByteArray());

    // 2. Send CBW (OUT)
    bulkOutPipe.syncSubmit(outPipe.createRequest(cbw), timeout);

    // 3. Data phase (if dataLen > 0)
    if (dataLen > 0) {
        if (isDataIn(cdb)) {
            // IN: device → host
            // Split into max 64KB chunks
            for (int offset = 0; offset < dataLen; offset += chunkSize) {
                int chunk = Math.min(65536, dataLen - offset);
                Memory chunkBuf = data.slice(dataOffset + offset, chunk);
                USBRequest req = bulkInPipe.createRequest(chunkBuf);
                bulkInPipe.syncSubmit(req, timeout);
            }
        } else {
            // OUT: host → device
            for (int offset = 0; offset < dataLen; offset += chunkSize) {
                int chunk = Math.min(65536, dataLen - offset);
                Memory chunkBuf = data.slice(dataOffset + offset, chunk);
                USBRequest req = bulkOutPipe.createRequest(chunkBuf);
                bulkOutPipe.syncSubmit(req, timeout);
            }
        }
    }

    // 4. Receive CSW (IN)
    CSW csw = new CSW();
    USBRequest cswReq = bulkInPipe.createRequest(csw);
    bulkInPipe.syncSubmit(cswReq, timeout);

    // 5. Validate CSW
    if (csw.getSignature() != US_BULK_CS_SIGN) throw new USBException("Invalid CSW signature");
    if (csw.getTag() != cbw.getTag()) throw new USBException("CSW tag mismatch");
    if (csw.getStatus() != US_BULK_CS_CMD_NO_ERROR) {
        // Caller should handle CHECK CONDITION via REQUEST SENSE
        throw new SCSIException("Command failed: status=" + csw.getStatus(), csw.getStatus());
    }
    if (csw.getResidue() != 0) {
        // Partial transfer - log warning
    }
}
```

**Helper**: `isDataIn(CDB)` — check opcode or CBW flags bit 7.

**Reset**: Already implemented (Bulk-Only Mass Storage Reset via control pipe).

---

### 3.5 USBStorageSCSIDevice.executeCommand()

**Update signature**:
```java
public int executeCommand(CDB cdb, Memory data, int dataOffset, long timeout)
    throws Exception, InterruptedException
```

**Algorithm**:
```java
ITransport t = storageDeviceData.getTransport();
try {
    t.transport(cdb, data, dataOffset, timeout);
    return 0; // Success
} catch (SCSIException e) {
    if (e.getStatus() == US_BULK_CS_CMD_FAILED) {
        // Auto-issue REQUEST SENSE
        Memory senseBuf = allocateSenseBuffer();
        CDBRequestSense rs = new CDBRequestSense(senseBuf.size());
        try {
            t.transport(rs, senseBuf, 0, timeout);
            SenseData sense = new SenseData(senseBuf);
            throw new SCSIException("SCSI error", sense);
        } catch (Exception ex) {
            throw new SCSIException("Failed to get sense data", ex);
        }
    }
    throw e;
}
```

---

### 3.6 MMCUtils — Add Missing Methods

```java
// Write data to device
public static void writeData(SCSIDevice dev, int lba, int nrBlocks,
                             Memory data, int dataOffset) 
    throws SCSIException, Exception, InterruptedException {
    CDB cdb = new CDBWrite10(lba, nrBlocks, dev.getSectorSize());
    dev.executeCommand(cdb, data, dataOffset, SCSIConstants.GROUP1_TIMEOUT);
}

// Synchronize cache (flush)
public static void synchronizeCache(SCSIDevice dev)
    throws SCSIException, Exception, InterruptedException {
    CDB cdb = new CDBSynchronizeCache10();
    dev.executeCommand(cdb, null, 0, SCSIConstants.GROUP1_TIMEOUT);
}

// Read mode page (for write protect, cache settings)
public static ModePageData modeSense(SCSIDevice dev, int pageCode)
    throws SCSIException, Exception, InterruptedException {
    Memory data = allocateBuffer(256);
    CDB cdb = new CDBModeSense6(pageCode, data.size());
    dev.executeCommand(cdb, data, 0, SCSIConstants.GROUP1_TIMEOUT);
    return new ModePageData(data);
}
```

---

### 3.7 USBStorageSCSIDriver — Block Device Operations

**Implement**:
```java
public void read(long devOffset, ByteBuffer dest) throws IOException {
    processChanged();  // Handles media change detection
    int sectorSize = getSectorSize();
    int lba = (int)(devOffset / sectorSize);
    int blocks = (int)((dest.remaining() + sectorSize - 1) / sectorSize);
    Memory data = allocateMemory(blocks * sectorSize);
    MMCUtils.readData(device, lba, blocks, data, 0);
    // Copy from Memory to ByteBuffer
    dataToByteBuffer(data, dest);
}

public void write(long devOffset, ByteBuffer src) throws IOException {
    processChanged();
    int sectorSize = getSectorSize();
    int lba = (int)(devOffset / sectorSize);
    int blocks = (int)((src.remaining() + sectorSize - 1) / sectorSize);
    Memory data = allocateMemory(blocks * sectorSize);
    byteBufferToData(src, data);
    MMCUtils.writeData(device, lba, blocks, data, 0);
}

public void flush() throws IOException {
    MMCUtils.synchronizeCache(device);
}

// processChanged() — re-enabled from commented code:
private void processChanged() throws IOException {
    if (changed) {
        capacity = null;
        try {
            // TEST UNIT READY first
            device.testUnit();
            // Then READ CAPACITY
            capacity = MMCUtils.readCapacity(device);
            changed = false;
        } catch (SCSIException e) {
            if (e.getSenseKey() == SenseKey.NOT_READY && e.getASC() == 0x3A) {
                throw new IOException("No media");
            }
            if (e.getSenseKey() == SenseKey.UNIT_ATTENTION && e.getASC() == 0x28) {
                // Media changed - will retry on next call
                changed = true;
                throw new IOException("Media changed");
            }
            throw new IOException("Device not ready", e);
        }
    }
}
```

---

### 3.8 VFS Integration — FSImpl.mount()

**Design change (approved 2026-08-08):** Replaced the original `USBMassStorageFileSystem` wrapper with a **`BlockIO` adapter + the existing `FatFileSystem`**. The FS project already ships a complete, tested FAT filesystem (`org.jnode.fs.jfat.FatFileSystem implements jx.fs.FileSystem`, constructed over `jx.devices.bio.BlockIO`) and a working VFS (`vfs.FSImpl`, mount pattern in `test/fs/FSDomain.java`). A bespoke `FileSystem` wrapper would have duplicated what FAT already provides.

**Design**: Create `USBStorageBlockIO implements BlockIO` that wraps `USBStorageSCSIDriver`.

```java
// org.jnode.driver.block.usb.storage.scsi.USBStorageBlockIO
public class USBStorageBlockIO implements BlockIO {
    // getCapacity()   -> (int) (driver.getLength() / sectorSize)
    // getSectorSize() -> driver.getSectorSize()
    // readSectors(startSector, n, buf, sync):
    //     driver.read(startSector * sectorSize, ByteBuffer.wrap(byte[]));
    //     buf.copyFromByteArray(data, 0, 0, n * sectorSize);
    // writeSectors(startSector, n, buf, sync):
    //     data[i] = buf.get8(i);
    //     driver.write(startSector * sectorSize, ByteBuffer.wrap(data));
    // I/O errors -> throw new Error(...)  (BlockIO has no checked exceptions; JX convention)
}
```

Note: `jx.zero.Memory` (Zero.jar) has `copyFromByteArray` but NOT `copyToByteArray`, so the write path uses `get8` loops.

**Mount flow**:
```java
// In a USB mount helper (Task 10 QEMU integration):
USBDevice usbDev = ... // enumerate, find mass storage interface
USBStorageSCSIHostDriver hostDriver = new USBStorageSCSIHostDriver();
hostDriver.startDevice(usbDev);                       // runs INQUIRY, creates USBStorageSCSIDevice
USBStorageSCSIDriver scsiDriver = new USBStorageSCSIDriver();
scsiDriver.startDevice(hostDriver.getScsiDevice());   // accessor added in Task 9
BlockIO bio = new USBStorageBlockIO(scsiDriver);
FileSystem fs = new FatFileSystem(bio);
FSImpl fsImpl = new FSImpl();
fsImpl.mountRoot(fs, false);                          // per FSDomain pattern
```

---

## 4. New Classes Required

### 4.1 CDBWrite10 (`org.jnode.driver.bus.scsi.cdb.mmc.CDBWrite10`)
```java
public class CDBWrite10 extends CDB {
    public CDBWrite10(int lba, int nrBlocks, int blockSize) {
        super(10, 0x2A);
        setInt32(2, lba);
        setInt16(7, nrBlocks);
        this.blockSize = blockSize;
    }
    @Override public int getDataTransfertCount() { return nrBlocks * blockSize; }
}
```

### 4.2 CDBSynchronizeCache10 (`org.jnode.driver.bus.scsi.cdb.mmc.CDBSynchronizeCache10`)
```java
public class CDBSynchronizeCache10 extends CDB {
    public CDBSynchronizeCache10() { super(10, 0x35); }
    @Override public int getDataTransfertCount() { return 0; }
}
```

### 4.3 CDBModeSense6 / CDBModeSelect6 (`org.jnode.driver.bus.scsi.cdb.spc.*`)

### 4.4 SenseData / ModePageData parsers

### 4.5 USBStorageBlockIO (replaces USBMassStorageFileSystem / USBMassStorageRootNode)

VFS integration uses the existing `org.jnode.fs.jfat.FatFileSystem` over a new `USBStorageBlockIO implements jx.devices.bio.BlockIO` adapter (see §3.8). No bespoke `FileSystem`/`Node` classes are needed.

---

## 5. Error Handling

| Scenario | Detection | Response |
|----------|-----------|----------|
| CSW status = 0x01 (failed) | `csw.getStatus()` | Auto-REQUEST SENSE, parse sense key/ASC/ASCQ |
| CSW status = 0x02 (phase error) | `csw.getStatus()` | Reset transport, retry once |
| USB stall/timeout | `USBException` | Clear endpoint halt, retry |
| Sense: NOT_READY/0x3A | Sense key/ASC | "No media" — return EOF/IOException |
| Sense: UNIT_ATTENTION/0x28 | Sense key/ASC | Set `changed=true`, retry on next op |
| Sense: WRITE_PROTECT/0x27 | Sense key/ASC | Return "read-only" error on write |

---

## 6. Testing Strategy

1. **Unit Tests** (mock USB pipes):
   - CBW/CSW construction/parsing
   - SCSIBuffer read/write
   - CDB transfer count calculations

2. **Integration Test** (QEMU):
   ```bash
   qemu-system-x86_64 -drive file=disk.img,format=raw,if=none,id=hd0 \
     -device usb-storage,drive=hd0
   ```
   - Verify INQUIRY, READ CAPACITY, READ(10), WRITE(10)

3. **Manual Test**:
   - Real USB flash drive
   - Mount via VFS, read/write files

---

## 7. Implementation Order

1. `SCSIBuffer` — all methods
2. Fix existing CDB `getDataTransfertCount()` + add `blockSize` to `CDBRead10`
3. New CDB classes: `CDBWrite10`, `CDBSynchronizeCache10`, `CDBModeSense6`, `CDBModeSelect6`
4. `ITransport` interface update
5. `USBStorageBulkTransport.transport()` — full BOT with data phase
6. `USBStorageSCSIDevice.executeCommand()` — wire transport + auto-REQUEST SENSE
7. `MMCUtils` — add `writeData()`, `synchronizeCache()`, `modeSense()`
8. `USBStorageSCSIDriver` — implement `read()`, `write()`, `flush()`, `processChanged()`
9. `USBStorageBlockIO` (BlockIO adapter) + `FatFileSystem` VFS integration
10. Boot-time enumeration wiring + QEMU test — `USBHubMonitor.USBDeviceAttachListener` (OS) fired on attach; `USBStorageMount` (test/FS) mounts the device; `USBMassStorageTest` (test/FS) self-verifies; `AI` (test/APP) registers the listener and starts the test

---

## 8. Open Questions (Resolved)

- [x] Block size from READ CAPACITY
- [x] Split transfers at 64KB
- [x] Sync USB requests
- [x] VFS integration design

---

## 9. Acceptance Criteria

- [ ] USB flash drive enumerates as mass storage (class 0x08, subclass 0x06, protocol 0x50)
- [ ] INQUIRY returns vendor/product strings
- [ ] READ CAPACITY returns correct sector count and size
- [ ] READ(10) reads correct data from device
- [ ] WRITE(10) writes correct data to device
- [ ] SYNCHRONIZE CACHE flushes write cache
- [ ] Media change detected (UNIT_ATTENTION/0x28) triggers re-read of capacity
- [ ] No media (NOT_READY/0x3A) returns appropriate error
- [ ] Device mounts via `FSImpl.mount()` and files can be read/written via VFS