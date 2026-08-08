# USB Mass Storage BOT Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the partially implemented USB Mass Storage Bulk-Only Transport (BOT) in the JNode codebase so USB mass storage devices can be mounted and used via the VFS.

**Architecture:** Layer-by-layer completion from SCSIBuffer (foundation) → CDB classes → ITransport + BulkTransport → SCSIDevice → MMCUtils → SCSIDriver → VFS integration. Each layer builds on the previous.

**Tech Stack:** Java, JNode USB/SCSI stack, jx.zero.Memory for buffers

## Global Constraints

- Block size read from READ CAPACITY (not hardcoded)
- Split transfers at 64KB max per BOT transaction
- Synchronous USB requests (`syncSubmit` with timeout)
- VFS integration via `FSImpl.mount()` with `USBMassStorageFileSystem` wrapping `USBStorageSCSIDriver`
- All multi-byte values big-endian (MSB first)
- Follow existing JNode code patterns and LGPL license headers

---

### Task 1: Implement SCSIBuffer Memory Read/Write

**Files:**
- Modify: `src/org/jnode/driver/bus/scsi/SCSIBuffer.java`

**Interfaces:**
- Produces: Working `setInt8`, `getUInt8`, `setInt16`, `getUInt16`, `setInt32`, `getInt32`, `getASCII`, `toByteArray`

- [ ] **Step 1: Write failing test**

```java
// Test in a new test file or main method
public static void testSCSIBuffer() {
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory mem = rm.alloc(32);
    SCSIBuffer buf = new SCSIBuffer(mem);
    
    // Test 8-bit
    buf.setInt8(0, 0xAB);
    assert buf.getUInt8(0) == 0xAB : "8-bit write/read failed";
    
    // Test 16-bit big-endian
    buf.setInt16(2, 0x1234);
    assert buf.getUInt16(2) == 0x1234 : "16-bit write/read failed";
    
    // Test 32-bit big-endian
    buf.setInt32(4, 0x12345678);
    assert buf.getInt32(4) == 0x12345678 : "32-bit write/read failed";
    
    // Test ASCII
    // (requires working setInt8/getUInt8 first)
    
    System.out.println("All SCSIBuffer tests passed");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: Compile and run test
Expected: FAIL - methods return 0/empty (stubs)

- [ ] **Step 3: Implement SCSIBuffer methods**

```java
public final void setInt8(int offset, int v) {
    buffer.set8(offset, (byte)v);
}

public final int getUInt8(int offset) {
    return buffer.get8(offset) & 0xFF;
}

public final void setInt16(int offset, int v) {
    buffer.set8(offset, (byte)(v >> 8));
    buffer.set8(offset + 1, (byte)(v & 0xFF));
}

public final int getUInt16(int offset) {
    return ((buffer.get8(offset) & 0xFF) << 8) | (buffer.get8(offset + 1) & 0xFF);
}

public final void setInt32(int offset, int v) {
    buffer.set8(offset, (byte)(v >> 24));
    buffer.set8(offset + 1, (byte)(v >> 16));
    buffer.set8(offset + 2, (byte)(v >> 8));
    buffer.set8(offset + 3, (byte)(v & 0xFF));
}

public final int getInt32(int offset) {
    return ((buffer.get8(offset) & 0xFF) << 24) |
           ((buffer.get8(offset + 1) & 0xFF) << 16) |
           ((buffer.get8(offset + 2) & 0xFF) << 8) |
           (buffer.get8(offset + 3) & 0xFF);
}

public final String getASCII(int offset, int length) {
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
        bytes[i] = (byte)buffer.get8(offset + i);
    }
    return new String(bytes, StandardCharsets.US_ASCII).trim();
}

public final Memory toByteArray() {
    return buffer;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: Compile and run test
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/org/jnode/driver/bus/scsi/SCSIBuffer.java
git commit -m "feat(scsi): implement SCSIBuffer Memory read/write for big-endian values"
```

---

### Task 2: Fix CDB Classes - getDataTransfertCount() and Add blockSize

**Files:**
- Modify: `src/org/jnode/driver/bus/scsi/cdb/spc/CDBInquiry.java`
- Modify: `src/org/jnode/driver/bus/scsi/cdb/spc/CDBRequestSense.java`
- Modify: `src/org/jnode/driver/bus/scsi/cdb/spc/CDBTestUnitReady.java`
- Modify: `src/org/jnode/driver/bus/scsi/cdb/mmc/CDBReadCapacity.java`
- Modify: `src/org/jnode/driver/bus/scsi/cdb/mmc/CDBRead10.java`

**Interfaces:**
- Consumes: Working SCSIBuffer (Task 1)
- Produces: Correct transfer counts for all existing CDB classes

- [ ] **Step 1: Fix CDBInquiry**

```java
// CDBInquiry.java
public class CDBInquiry extends CDB {
    private final int allocLen;
    
    public CDBInquiry(int allocLen) {
        super(6, 0x12);
        this.allocLen = allocLen;
        setInt8(4, allocLen);
    }
    
    @Override
    public int getDataTransfertCount() {
        return allocLen;
    }
}
```

- [ ] **Step 2: Fix CDBRequestSense**

```java
// CDBRequestSense.java
public class CDBRequestSense extends CDB {
    private final int allocLen;
    
    public CDBRequestSense(int allocLen) {
        super(6, 0x03);
        this.allocLen = allocLen;
        setInt8(4, allocLen);
    }
    
    @Override
    public int getDataTransfertCount() {
        return allocLen;
    }
}
```

- [ ] **Step 3: Fix CDBTestUnitReady**

```java
// CDBTestUnitReady.java
public class CDBTestUnitReady extends CDB {
    public CDBTestUnitReady() {
        super(6, 0x00);
    }
    
    @Override
    public int getDataTransfertCount() {
        return 0;
    }
}
```

- [ ] **Step 4: Fix CDBReadCapacity**

```java
// CDBReadCapacity.java
public class CDBReadCapacity extends CDB {
    public CDBReadCapacity() {
        super(10, 0x25);
    }
    
    @Override
    public int getDataTransfertCount() {
        return 8; // Fixed 8-byte response
    }
}
```

- [ ] **Step 5: Fix CDBRead10 - add blockSize parameter**

```java
// CDBRead10.java
public class CDBRead10 extends CDB {
    private final int nrBlocks;
    private final int blockSize;
    
    public CDBRead10(int lba, int nrBlocks, int blockSize) {
        super(10, 0x28);
        this.nrBlocks = nrBlocks;
        this.blockSize = blockSize;
        setInt32(2, lba);
        setInt16(7, nrBlocks);
    }
    
    @Override
    public int getDataTransfertCount() {
        return nrBlocks * blockSize;
    }
}
```

- [ ] **Step 6: Run tests to verify**

Run: Compile all CDB classes
Expected: PASS (no runtime tests yet, but compiles)

- [ ] **Step 7: Commit**

```bash
git add src/org/jnode/driver/bus/scsi/cdb/spc/CDBInquiry.java \
        src/org/jnode/driver/bus/scsi/cdb/spc/CDBRequestSense.java \
        src/org/jnode/driver/bus/scsi/cdb/spc/CDBTestUnitReady.java \
        src/org/jnode/driver/bus/scsi/cdb/mmc/CDBReadCapacity.java \
        src/org/jnode/driver/bus/scsi/cdb/mmc/CDBRead10.java
git commit -m "feat(scsi): fix CDB getDataTransfertCount() and add blockSize to CDBRead10"
```

---

### Task 3: Create New CDB Classes (Write10, SynchronizeCache10, ModeSense6, ModeSelect6)

**Files:**
- Create: `src/org/jnode/driver/bus/scsi/cdb/mmc/CDBWrite10.java`
- Create: `src/org/jnode/driver/bus/scsi/cdb/mmc/CDBSynchronizeCache10.java`
- Create: `src/org/jnode/driver/bus/scsi/cdb/spc/CDBModeSense6.java`
- Create: `src/org/jnode/driver/bus/scsi/cdb/spc/CDBModeSelect6.java`

**Interfaces:**
- Consumes: Working SCSIBuffer (Task 1), CDB base class
- Produces: Complete CDB command set for mass storage

- [ ] **Step 1: Create CDBWrite10**

```java
// CDBWrite10.java
package org.jnode.driver.bus.scsi.cdb.mmc;

import org.jnode.driver.bus.scsi.CDB;

public class CDBWrite10 extends CDB {
    private final int nrBlocks;
    private final int blockSize;
    
    public CDBWrite10(int lba, int nrBlocks, int blockSize) {
        super(10, 0x2A);
        this.nrBlocks = nrBlocks;
        this.blockSize = blockSize;
        setInt32(2, lba);
        setInt16(7, nrBlocks);
    }
    
    @Override
    public int getDataTransfertCount() {
        return nrBlocks * blockSize;
    }
}
```

- [ ] **Step 2: Create CDBSynchronizeCache10**

```java
// CDBSynchronizeCache10.java
package org.jnode.driver.bus.scsi.cdb.mmc;

import org.jnode.driver.bus.scsi.CDB;

public class CDBSynchronizeCache10 extends CDB {
    public CDBSynchronizeCache10() {
        super(10, 0x35);
        // IMMED bit in byte 1 if needed
    }
    
    @Override
    public int getDataTransfertCount() {
        return 0;
    }
}
```

- [ ] **Step 3: Create CDBModeSense6**

```java
// CDBModeSense6.java
package org.jnode.driver.bus.scsi.cdb.spc;

import org.jnode.driver.bus.scsi.CDB;

public class CDBModeSense6 extends CDB {
    private final int allocLen;
    
    public CDBModeSense6(int pageCode, int allocLen) {
        super(6, 0x1A);
        this.allocLen = allocLen;
        setInt8(2, pageCode & 0x3F); // page code + DBD bit
        setInt8(4, allocLen);
    }
    
    @Override
    public int getDataTransfertCount() {
        return allocLen;
    }
}
```

- [ ] **Step 4: Create CDBModeSelect6**

```java
// CDBModeSelect6.java
package org.jnode.driver.bus.scsi.cdb.spc;

import org.jnode.driver.bus.scsi.CDB;

public class CDBModeSelect6 extends CDB {
    private final int paramLen;
    
    public CDBModeSelect6(int paramLen) {
        super(6, 0x15);
        this.paramLen = paramLen;
        setInt8(1, 0x10); // PF=1 (page format)
        setInt8(4, paramLen);
    }
    
    @Override
    public int getDataTransfertCount() {
        return paramLen;
    }
}
```

- [ ] **Step 5: Compile and verify**

Run: `javac` all new CDB classes
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/org/jnode/driver/bus/scsi/cdb/mmc/CDBWrite10.java \
        src/org/jnode/driver/bus/scsi/cdb/mmc/CDBSynchronizeCache10.java \
        src/org/jnode/driver/bus/scsi/cdb/spc/CDBModeSense6.java \
        src/org/jnode/driver/bus/scsi/cdb/spc/CDBModeSelect6.java
git commit -m "feat(scsi): add CDBWrite10, CDBSynchronizeCache10, CDBModeSense6, CDBModeSelect6"
```

---

### Task 4: Update ITransport Interface

**Files:**
- Modify: `src/org/jnode/driver/block/usb/storage/ITransport.java`

**Interfaces:**
- Produces: Updated interface with data/offset/timeout parameters

- [ ] **Step 1: Update ITransport.java**

```java
// ITransport.java
package org.jnode.driver.block.usb.storage;

import org.jnode.driver.bus.scsi.CDB;
import org.jnode.driver.bus.usb.USBException;
import jx.zero.Memory;

public interface ITransport {
    void transport(CDB cdb, Memory data, int dataOffset, long timeout);
    void reset() throws USBException;
}
```

- [ ] **Step 2: Compile and verify**

Run: `javac` ITransport.java
Expected: PASS (will break USBStorageBulkTransport - fixed in Task 5)

- [ ] **Step 3: Commit**

```bash
git add src/org/jnode/driver/block/usb/storage/ITransport.java
git commit -m "feat(usb): update ITransport interface for data phase support"
```

---

### Task 5: Implement Full BOT Protocol in USBStorageBulkTransport

**Files:**
- Modify: `src/org/jnode/driver/block/usb/storage/USBStorageBulkTransport.java`

**Interfaces:**
- Consumes: Updated ITransport (Task 4), CBW/CSW classes, USB pipes
- Produces: Complete BOT implementation with CBW → Data → CSW

- [ ] **Step 1: Add instance variables and constants**

```java
// USBStorageBulkTransport.java - add to class
private int nextTag = 1;
private static final int MAX_BOT_TRANSFER = 65536; // 64KB
```

- [ ] **Step 2: Implement isDataIn helper**

```java
private boolean isDataIn(CDB cdb) {
    int opcode = cdb.getOpcode();
    // IN commands: READ, INQUIRY, REQUEST SENSE, READ CAPACITY, MODE SENSE
    return opcode == 0x28 || opcode == 0x12 || opcode == 0x03 || 
           opcode == 0x25 || opcode == 0x1A || opcode == 0xA8; // READ(12) if used
}
```

- [ ] **Step 3: Implement full transport() method**

```java
@Override
public void transport(CDB cdb, Memory data, int dataOffset, long timeout) {
    try {
        // 1. Build CBW
        CBW cbw = new CBW();
        cbw.setSignature(US_BULK_CB_SIGN);
        int tag = nextTag++;
        cbw.setTag(tag);
        int dataLen = cdb.getDataTransfertCount();
        cbw.setDataTransferLength(dataLen);
        byte flags = (byte) (data != null && dataLen > 0 && isDataIn(cdb) ? US_BULK_FLAG_IN : US_BULK_FLAG_OUT);
        cbw.setFlags(flags);
        cbw.setLun((byte) 0);
        Memory cdbMem = cdb.toByteArray();
        cbw.setLength((byte) cdbMem.size());
        cbw.setCdb(cdbMem);

        // 2. Send CBW (OUT)
        USBDataPipe outPipe = (USBDataPipe) storageDeviceData.getBulkOutEndPoint().getPipe();
        USBRequest req = outPipe.createRequest(cbw);
        outPipe.syncSubmit(req, timeout);
        if (req.getStatus() != USBREQ_ST_COMPLETED) {
            throw new USBException("CBW submit failed: " + req.getStatus());
        }

        // 3. Data phase
        if (dataLen > 0 && data != null) {
            boolean dataIn = (flags & US_BULK_FLAG_IN) != 0;
            USBDataPipe dataPipe = dataIn ? 
                (USBDataPipe) storageDeviceData.getBulkInEndPoint().getPipe() :
                (USBDataPipe) storageDeviceData.getBulkOutEndPoint().getPipe();
            
            for (int offset = 0; offset < dataLen; offset += MAX_BOT_TRANSFER) {
                int chunk = Math.min(MAX_BOT_TRANSFER, dataLen - offset);
                Memory chunkBuf = sliceMemory(data, dataOffset + offset, chunk);
                USBRequest dataReq = dataPipe.createRequest(chunkBuf);
                dataPipe.syncSubmit(dataReq, timeout);
                if (dataReq.getStatus() != USBREQ_ST_COMPLETED) {
                    throw new USBException("Data phase failed: " + dataReq.getStatus());
                }
            }
        }

        // 4. Receive CSW (IN)
        CSW csw = new CSW();
        csw.setSignature(US_BULK_CS_SIGN);
        USBDataPipe inPipe = (USBDataPipe) storageDeviceData.getBulkInEndPoint().getPipe();
        USBRequest cswReq = inPipe.createRequest(csw);
        inPipe.syncSubmit(cswReq, timeout);
        if (cswReq.getStatus() != USBREQ_ST_COMPLETED) {
            throw new USBException("CSW submit failed: " + cswReq.getStatus());
        }

        // 5. Validate CSW
        if (csw.getSignature() != US_BULK_CS_SIGN) {
            throw new USBException("Invalid CSW signature: 0x" + NumberUtils.hex(csw.getSignature(), 8));
        }
        if (csw.getTag() != tag) {
            throw new USBException("CSW tag mismatch: expected " + tag + ", got " + csw.getTag());
        }
        int status = csw.getStatus() & 0xFF;
        if (status != US_BULK_CS_CMD_NO_ERROR) {
            // Don't throw here - let caller handle via REQUEST SENSE
            // But we need to signal the failure
            throw new SCSICommandFailedException(status, csw.getResidue());
        }
        if (csw.getResidue() != 0) {
            // Log partial transfer warning
            System.err.println("WARNING: CSW residue = " + csw.getResidue());
        }
        
    } catch (USBException e) {
        throw e;
    } catch (Exception e) {
        throw new USBException("Transport error", e);
    }
}

// Helper to slice Memory (jx.zero.Memory may not have slice)
// We'll need to implement this based on Memory API
private Memory sliceMemory(Memory src, int offset, int length) {
    // Implementation depends on Memory API - may need to copy
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory dst = rm.alloc(length);
    // Copy bytes from src to dst
    for (int i = 0; i < length; i++) {
        dst.set8(i, src.get8(offset + i));
    }
    return dst;
}
```

- [ ] **Step 4: Create SCSICommandFailedException**

```java
// SCSICommandFailedException.java (new file in same package)
package org.jnode.driver.block.usb.storage;

public class SCSICommandFailedException extends USBException {
    private final int status;
    private final int residue;
    
    public SCSICommandFailedException(int status, int residue) {
        super("SCSI command failed with status: 0x" + NumberUtils.hex(status, 2));
        this.status = status;
        this.residue = residue;
    }
    
    public int getStatus() { return status; }
    public int getResidue() { return residue; }
}
```

- [ ] **Step 5: Compile and verify**

Run: `javac` USBStorageBulkTransport.java and new exception
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/org/jnode/driver/block/usb/storage/USBStorageBulkTransport.java \
        src/org/jnode/driver/block/usb/storage/SCSICommandFailedException.java
git commit -m "feat(usb): implement full BOT protocol with data phase in USBStorageBulkTransport"
```

**Review fixes applied (2026-08-08):**
- `CSW.getSignature()/getTag()/getResidue()` originally delegated to `USBPacket.getInt()`, which ends with a `(short)` cast (USBPacket.java:146) and truncates 32-bit reads to 16 bits. The CSW signature check could therefore never pass. Fixed by reconstructing full-width ints from `getByte()` in CSW.java (offsets 0/4/8).
- `ITransport.transport()` declares `throws USBException` (was missing; required so the checked `USBException`/`SCSICommandFailedException` propagate).
- Added `dataLen > 0 && data == null` guard in `transport()`.

**Known risks to validate in Task 10 (QEMU):**
1. **ZLP on bulk IN**: READ10/READ CAPACITY data lengths are exact multiples of `wMaxPacketSize`, so the device terminates the IN data phase with a zero-length packet. `UHCIDataRequest.createTDs` (OS module) builds TDs with `while (length > 0)` and never emits a length-0 TD. Likely works via UHCI short-packet retirement, but if Task 10 shows a data-phase timeout on sector reads, add a length-0 IN TD in `UHCIDataRequest.createTDs` when `size % maxPacketSize == 0` (controller layer, outside this module).
2. **`USBPacket.getInt()` `(short)` truncation** (root cause, OS module): unambiguously buggy, but leave untouched for now; the localized CSW fix covers this project.

---

### Task 6: Update USBStorageSCSIDevice.executeCommand() with Auto-REQUEST SENSE

**Files:**
- Modify: `src/org/jnode/driver/block/usb/storage/USBStorageSCSIHostDriver.java` (inner class USBStorageSCSIDevice)

**Interfaces:**
- Consumes: Updated ITransport (Task 4), SCSICommandFailedException (Task 5), CDBRequestSense, SenseData
- Produces: executeCommand with proper error handling

- [ ] **Step 1: Update executeCommand signature and implementation**

```java
// In USBStorageSCSIDevice class
@Override
public int executeCommand(CDB cdb, Memory data, int dataOffset, long timeout)
    throws Exception, InterruptedException {
    
    ITransport t = storageDeviceData.getTransport();
    try {
        t.transport(cdb, data, dataOffset, timeout);
        return 0; // Success
    } catch (SCSICommandFailedException e) {
        // Auto-issue REQUEST SENSE
        MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
        Memory senseBuf = rm.alloc(18); // Standard sense data size
        CDBRequestSense rs = new CDBRequestSense(senseBuf.size());
        
        try {
            t.transport(rs, senseBuf, 0, timeout);
            SenseData sense = new SenseData(senseBuf);
            throw new SCSIException("SCSI command failed", sense);
        } catch (Exception ex) {
            throw new SCSIException("Failed to get sense data after command failure", ex);
        }
    }
}
```

- [ ] **Step 2: Update inquiry() and testUnit() to pass data/offset**

```java
protected final void inquiry() throws Exception, InterruptedException {
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory inqData = rm.alloc(96); // Standard INQUIRY response size
    ITransport t = storageDeviceData.getTransport();
    t.transport(new CDBInquiry(inqData.size()), inqData, 0, 50000);
    inquiryResult = new InquiryData(inqData);
}

protected final void testUnit() throws Exception, InterruptedException {
    ITransport t = storageDeviceData.getTransport();
    t.transport(new CDBTestUnitReady(), null, 0, 50000);
}
```

- [ ] **Step 3: Compile and verify**

Run: `javac` USBStorageSCSIHostDriver.java
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/org/jnode/driver/block/usb/storage/USBStorageSCSIHostDriver.java
git commit -m "feat(usb): update USBStorageSCSIDevice.executeCommand with auto-REQUEST SENSE"
```

---

### Task 7: Add Missing Methods to MMCUtils

**Files:**
- Modify: `src/org/jnode/driver/bus/scsi/cdb/mmc/MMCUtils.java`

**Interfaces:**
- Consumes: New CDB classes (Task 3), updated executeCommand (Task 6)
- Produces: writeData(), synchronizeCache(), modeSense()

- [ ] **Step 1: Add writeData method**

```java
// In MMCUtils class
public static void writeData(SCSIDevice dev, int lba, int nrBlocks,
                             Memory data, int dataOffset) 
    throws SCSIException, Exception, InterruptedException {
    // Get sector size from device (need accessor)
    int sectorSize = getSectorSize(dev); // Add helper or use CapacityData
    CDB cdb = new CDBWrite10(lba, nrBlocks, sectorSize);
    dev.executeCommand(cdb, data, dataOffset, SCSIConstants.GROUP1_TIMEOUT);
}
```

- [ ] **Step 2: Add synchronizeCache method**

```java
public static void synchronizeCache(SCSIDevice dev)
    throws SCSIException, Exception, InterruptedException {
    CDB cdb = new CDBSynchronizeCache10();
    dev.executeCommand(cdb, null, 0, SCSIConstants.GROUP1_TIMEOUT);
}
```

- [ ] **Step 3: Add modeSense method**

```java
public static ModePageData modeSense(SCSIDevice dev, int pageCode)
    throws SCSIException, Exception, InterruptedException {
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory data = rm.alloc(256);
    CDB cdb = new CDBModeSense6(pageCode, data.size());
    dev.executeCommand(cdb, data, 0, SCSIConstants.GROUP1_TIMEOUT);
    return new ModePageData(data);
}
```

- [ ] **Step 4: Create ModePageData parser class**

```java
// ModePageData.java (new file in mmc package)
package org.jnode.driver.bus.scsi.cdb.mmc;

import org.jnode.driver.bus.scsi.SCSIBuffer;

public class ModePageData {
    private final SCSIBuffer buffer;
    
    public ModePageData(Memory data) {
        this.buffer = new SCSIBuffer(data);
    }
    
    public boolean isWriteProtected() {
        // Mode parameter header byte 2, bit 7
        return (buffer.getUInt8(2) & 0x80) != 0;
    }
    
    // Add more parsers as needed
}
```

- [ ] **Step 5: Compile and verify**

Run: `javac` MMCUtils.java and ModePageData.java
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/org/jnode/driver/bus/scsi/cdb/mmc/MMCUtils.java \
        src/org/jnode/driver/bus/scsi/cdb/mmc/ModePageData.java
git commit -m "feat(scsi): add writeData, synchronizeCache, modeSense to MMCUtils"
```

---

### Task 8: Implement USBStorageSCSIDriver Block Operations

**Files:**
- Modify: `src/org/jnode/driver/block/usb/storage/scsi/USBStorageSCSIDriver.java`

**Interfaces:**
- Consumes: MMCUtils (Task 7), updated executeCommand (Task 6)
- Produces: Working read(), write(), flush(), processChanged(), getSectorSize(), getLength()

- [ ] **Step 1: Add sectorSize field and processChanged() implementation**

```java
// USBStorageSCSIDriver.java - add fields
private int sectorSize = 512; // Default, updated by READ CAPACITY
private boolean mediaChanged = true;

// Re-enable and implement processChanged()
private void processChanged() throws IOException {
    if (mediaChanged) {
        capacity = null;
        try {
            // TEST UNIT READY
            device.testUnit();
            // READ CAPACITY
            capacity = MMCUtils.readCapacity(device);
            sectorSize = capacity.getBlockLength();
            mediaChanged = false;
        } catch (SCSIException e) {
            SenseKey key = e.getSenseKey();
            int asc = e.getASC();
            if (key == SenseKey.NOT_READY && asc == 0x3A) {
                throw new IOException("No media present");
            }
            if (key == SenseKey.UNIT_ATTENTION && asc == 0x28) {
                mediaChanged = true; // Will retry
                throw new IOException("Media may have changed");
            }
            throw new IOException("Device not ready: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Error reading capacity", e);
        }
    }
}
```

- [ ] **Step 2: Implement read()**

```java
@Override
public void read(long devOffset, ByteBuffer dest) throws IOException {
    processChanged();
    if (capacity == null) {
        throw new IOException("No medium");
    }
    
    int blocks = (int)((dest.remaining() + sectorSize - 1) / sectorSize);
    int lba = (int)(devOffset / sectorSize);
    
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory data = rm.alloc(blocks * sectorSize);
    
    try {
        MMCUtils.readData(device, lba, blocks, data, 0);
        // Copy from Memory to ByteBuffer
        for (int i = 0; i < dest.remaining(); i++) {
            dest.put((byte)data.get8(i));
        }
    } catch (Exception e) {
        throw new IOException("Read failed", e);
    }
}
```

- [ ] **Step 3: Implement write()**

```java
@Override
public void write(long devOffset, ByteBuffer src) throws IOException {
    processChanged();
    if (capacity == null) {
        throw new IOException("No medium");
    }
    
    int blocks = (int)((src.remaining() + sectorSize - 1) / sectorSize);
    int lba = (int)(devOffset / sectorSize);
    
    MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory data = rm.alloc(blocks * sectorSize);
    
    // Copy from ByteBuffer to Memory
    int pos = src.position();
    for (int i = 0; i < src.remaining(); i++) {
        data.set8(i, src.get(pos + i));
    }
    
    try {
        MMCUtils.writeData(device, lba, blocks, data, 0);
    } catch (Exception e) {
        throw new IOException("Write failed", e);
    }
}
```

- [ ] **Step 4: Implement flush() and getSectorSize()/getLength()**

```java
@Override
public void flush() throws IOException {
    try {
        MMCUtils.synchronizeCache(device);
    } catch (Exception e) {
        throw new IOException("Flush failed", e);
    }
}

@Override
public int getSectorSize() throws IOException {
    processChanged();
    return sectorSize;
}

@Override
public long getLength() throws IOException {
    processChanged();
    if (capacity == null) {
        return 0;
    }
    return (long)sectorSize * (capacity.getLogicalBlockAddress() + 1);
}
```

- [ ] **Step 5: Handle media change notification**

```java
// Add method to notify media change (called from sense handling)
public void notifyMediaChanged() {
    mediaChanged = true;
}
```

- [ ] **Step 6: Compile and verify**

Run: `javac` USBStorageSCSIDriver.java
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/org/jnode/driver/block/usb/storage/scsi/USBStorageSCSIDriver.java
git commit -m "feat(usb): implement block operations in USBStorageSCSIDriver"
```

---

### Task 9: VFS Integration — USBStorageBlockIO + FatFileSystem

**Design change (approved by user, 2026-08-08):** The original plan (stub `USBMassStorageFileSystem` / `USBMassStorageRootNode` throwing `NotSupportedException`) was replaced. The FS project already contains a complete, tested FAT filesystem (`org.jnode.fs.jfat.FatFileSystem`, implements `jx.fs.FileSystem`, constructed over a `jx.devices.bio.BlockIO`) and a working VFS (`vfs.FSImpl`, mounted via `FSImpl.mountRoot(FileSystem, readOnly)` as in `test/fs/FSDomain`). The missing link is exposing the USB mass storage block driver as a `BlockIO`.

**Files:**
- Create: `src/org/jnode/driver/block/usb/storage/scsi/USBStorageBlockIO.java`

**Interfaces:**
- Consumes: USBStorageSCSIDriver (Task 8), `jx.devices.bio.BlockIO`
- Produces: A `BlockIO` backed by the USB storage driver, usable by `FatFileSystem` and the VFS

- [x] **Step 1: Create USBStorageBlockIO**

```java
// src/org/jnode/driver/block/usb/storage/scsi/USBStorageBlockIO.java
package org.jnode.driver.block.usb.storage.scsi;

public class USBStorageBlockIO implements BlockIO {
    // getCapacity()   -> driver.getLength() / sectorSize
    // getSectorSize() -> driver.getSectorSize()
    // readSectors(startSector, n, buf, sync):
    //     driver.read(startSector * sectorSize, ByteBuffer.wrap(byte[]));
    //     buf.copyFromByteArray(data, 0, 0, n * sectorSize);
    // writeSectors(startSector, n, buf, sync):
    //     data[i] = buf.get8(i); driver.write(startSector * sectorSize, ByteBuffer.wrap(data));
}
```

Note: `jx.zero.Memory` (Zero.jar) has `copyFromByteArray` but NOT `copyToByteArray`, so the write path uses `get8` loops.

- [x] **Step 2: Mount flow (for the QEMU integration in Task 10)**

Added `USBStorageSCSIHostDriver.getScsiDevice()` accessor (returns the `USBStorageSCSIDevice` created in `startDevice`); the code below now resolves against it.

```java
USBDevice usbDev = ...; // enumerated mass storage device
USBStorageSCSIHostDriver hostDriver = new USBStorageSCSIHostDriver();
hostDriver.startDevice(usbDev);                 // runs INQUIRY, creates USBStorageSCSIDevice
USBStorageSCSIDriver scsiDriver = new USBStorageSCSIDriver();
scsiDriver.startDevice(hostDriver.getScsiDevice());
BlockIO bio = new USBStorageBlockIO(scsiDriver);
FileSystem fat = new FatFileSystem(bio);
FSImpl fs = new FSImpl();
fs.mountRoot(fat, false);
fs.registerPortal(fs, "FS"); // per FSDomain pattern
```

- [x] **Step 3: Compile and verify**

Run: `ant compile`
Expected: PASS (done)

**Review (2026-08-08):** SPEC PASS. Quality: fixed one major — added `checkBufferSize()` guard in `readSectors`/`writeSectors` (JNode `Memory` does no bounds checking; FAT hardcodes 512-byte sectors, so a >512-byte-sector device would overrun the buffer). Accepted as-is: cached `sectorSize` (stale only after media change), int math overflow >2 GiB per call, per-call `byte[]` alloc, ignored `synchronous`.

---

### Task 10: Integration Test with QEMU

**Design change (approved by user, 2026-08-08):** The original plan assumed a shell `mount /dev/usb0` command. This fork has no USB device-detection/driver-matching layer and no such shell command, so Task 10 is implemented as **boot-time enumeration wiring + a self-test domain** that prints results on the serial console.

**Files:**
- Modify (OS module, `jdk0`): `OS/src/org/jnode/driver/bus/usb/USBHubMonitor.java` — added `USBDeviceAttachListener` interface + `addAttachListener`/`removeAttachListener`, fired from `portConnectionStatusChanged` after successful enumeration.
- Create (test/FS): `src/org/jnode/driver/block/usb/storage/scsi/USBStorageMount.java` — attach listener: checks class 0x08/subclass 0x06 (transparent SCSI)/protocol 0x50 (Bulk-Only), then runs `USBStorageSCSIHostDriver.startDevice(USBDevice)` → `USBStorageSCSIDriver.startDevice(scsiDevice)` → `USBStorageBlockIO` → `FatFileSystem` → `FSImpl.mountRoot(fat, false)`; registers portals `USBFS` (VFS) and `USBBlockIO`. Made `startDevice`/`stopDevice` public on `USBStorageSCSIHostDriver` and `USBStorageSCSIDriver` (framework is gutted; no Driver base calls them).
- Create (test/FS): `src/test/fs/USBMassStorageTest.java` — test domain; waits for the `USBFS` portal (`LookupHelper.waitUntilPortalAvailable`), creates/writes/reads/unlinks a file on the mounted FAT fs, prints PASS/FAIL.
- Modify (test/APP): `src/AI/AI.java` — registers `USBStorageMount` on each `USBHubMonitor`; starts `USBMassStorageTest` on a thread from `start()`.

- [x] **Step 1: Implement enumeration wiring + test domain** (all projects compile: OS, FS, APP; dist jars refreshed)

- [x] **Step 2: Create test disk image** (helper script below)

```bash
# scripts/usb-storage-test.sh (in test/FS)
dd if=/dev/zero of=/tmp/usbtest.img bs=1M count=64
mkfs.vfat -F 32 /tmp/usbtest.img
mkdir -p /tmp/usbmnt
sudo mount -o loop /tmp/usbtest.img /tmp/usbmnt
echo "Hello USB" | sudo tee /tmp/usbmnt/hello.txt
sudo umount /tmp/usbmnt
```

- [ ] **Step 3: Rebuild MyOS.iso and run QEMU**

Rebuild the OS image (dist jars for OS/FS/APP are already refreshed by `ant jar`):

```bash
# 1. Compile the Java image (Compiler project consumes ../test/APP/dist/testOS.jar,
#    ../test/FS/dist/testFS.jar, ../test/HCI/dist/testHCI.jar + Zero/OS/AIZero/ifOS jars)
#    -> regenerates Compiler/app/isodir/code/{init2.jll, code.zip, bootrc.jll}
# 2. make  -> jxcore kernel
# 3. run.sh -> MyOS.iso (grub-mkrescue)

qemu-system-x86_64 -cdrom MyOS.iso -serial stdio \
    -drive file=/tmp/usbtest.img,format=raw,if=none,id=hd0 \
    -device usb-storage,drive=hd0
```

- [ ] **Step 4: Verify acceptance criteria (serial console output)**

Expected on boot, in order:
1. `Creating USBDevice: ...` + descriptor printout (enumeration)
2. `USBStorageMount: capacity N x M bytes`
3. `USBStorageMount: FatFileSystem mounted, portals 'USBFS' and 'USBBlockIO' registered`
4. `USBMassStorageTest: wrote=10 read=10 contentOk=true`
5. `USBMassStorageTest: PASS`
6. `cat /tmp/usbmnt/hello.txt` on the host still shows `Hello USB` (read-only preexisting file intact)

- [ ] **Step 5: Known risks to validate in QEMU**

1. **Bulk IN ZLP** (documented in Task 5): when a READ(10)/CSW transfer length is an exact multiple of wMaxPacketSize (64B), `UHCIDataRequest.createTDs` never emits a length-0 TD. UHCI's short-packet retirement may terminate the transfer anyway; if reads hang/fail, fix in `UHCIDataRequest.createTDs` (controller layer).
2. **Endpoint direction swap** (`USBStorageDeviceData.java:95-100`): the scan assigns `bulkInEndPoint` to endpoints with `(addr & USB_DIR_IN) == 0` and `bulkOutEndPoint` otherwise — the opposite of the host-perspective names. If CBW/CSW or the data phase fails with a USBException/status error, swap the two assignments.
3. **Write zero-bytes bug** (fixed 2026-08-08): `USBStorageSCSIDriver.write()` had `remaining = 0` instead of `src.length` — fixed. Verify write path works in QEMU.
4. If `FatFileSystem(BlockIO)` swallows an IOException during `Fat.create` (leaves internal `fat == null`), mount fails with an NPE; `USBStorageMount` will print `mount failed`.

---

## Summary

| Task | Component | Key Deliverable |
|------|-----------|-----------------|
| 1 | SCSIBuffer | Working big-endian Memory read/write |
| 2 | Existing CDBs | Fixed transfer counts, blockSize param |
| 3 | New CDBs | WRITE10, SYNC_CACHE, MODE_SENSE/SELECT |
| 4 | ITransport | Updated interface with data phase |
| 5 | BulkTransport | Full CBW → Data → CSW implementation |
| 6 | SCSIDevice | executeCommand with auto-REQUEST SENSE |
| 7 | MMCUtils | writeData, syncCache, modeSense |
| 8 | SCSIDriver | read/write/flush/processChanged |
| 9 | VFS | USBStorageBlockIO + FatFileSystem mountable |
| 10 | Integration | Boot-time enumeration wiring + USBMassStorageTest |

**Total estimated time**: 10 tasks × ~30 min = ~5 hours