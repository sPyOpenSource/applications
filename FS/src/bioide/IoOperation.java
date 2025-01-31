package bioide;

import jx.zero.*;
import jx.zero.Debug;

/**
 * Base class for I/O operations
 * @author Michael Golm
 * @author Andreas Weissel
 */
abstract class IoOperation extends Operation {
    // possible states of an IoOperation
    public static final Object STATE_UNKNOWN = new Object();
    public static final Object STATE_RUNNING = new Object();
    public static final Object STATE_COMPLETED = new Object();

    protected Memory     buffer;      // data buffer
    protected int        offset;      // offset in buffer
    protected int        sector;      // start sector 
    protected int        count;       // number of sectors
    protected CPUState   cpuState;    // thread that waits for completion of this operation
    protected AtomicVariable state;   // current state of this operation

    private final boolean synchronous; // synchronous/asynchronous operation

    public IoOperation(Memory buffer, int count, Controller controller, Drive drive, int sector, boolean synchronous) {
    super(controller, drive);
    this.synchronous = synchronous;
    this.offset      = 0;
    this.sector      = sector;
    this.buffer      = buffer;
    this.count       = count;
    if (count > buffer.size() / 512) throw new Error();
    state = Env.cpuManager.getAtomicVariable();

    }

    @Override
    public void endOperation(boolean uptodate) {
    errors = 0;
    if (!uptodate)
        errors = 1;
    controller.nextOperation();
        if( Env.verboseIO )
        Debug.out.println("endOperation " + uptodate);
        if( Env.verboseIO )
        Debug.out.println("endOperation fertig");
    state.atomicUpdateUnblock(STATE_COMPLETED, cpuState);
    }

    @Override
    public void waitForCompletion() {
        if( Env.verboseIO )
        Env.cpuManager.dump("waitForCompletion in IoOperation:", this);
    cpuState = Env.cpuManager.getCPUState();
        if( Env.verboseIO )
        Env.cpuManager.dump("ctx in waitForCompletion:", cpuState);

    state.blockIfEqual(STATE_RUNNING);
    }


    /**
     * Fill controller registers with operation parameters (number of sectors, first sector...).
     */
    protected void ioInit() throws IDEException {
    int block = sector;

    controller.setLDHReg(drive.select);
    if (!controller.waitFor(Controller.STATUS_RDY, Controller.STATUS_BSY|Controller.STATUS_DRQ, 30)) { 
        Debug.out.println("" + drive.name + ": Geraet nicht bereit");
        endOperation(false);
        throw new Error(); // return -1;
    }

    // Select drive
    controller.setCTLReg(drive.ctl);
    // Number of sectors to be read/written
    if (count < 256)
        controller.setCountReg((byte)(count & 0xff));
    else
        controller.setCountReg((byte)0);

    if (drive.lba()) {  // LBA-Modus
        controller.setSectorReg((byte)(block & 0xff));
        controller.setLoCylReg((byte)((block >>= 8) & 0xff));
        controller.setHiCylReg((byte)((block >>= 8) & 0xff));
        controller.setLDHReg((byte)(((block >> 8) & 0x0f) | drive.select));
        /* the LBA number is coded as follows:
           28 Bit: 0000HHHH CCCCCCCC cccccccc SSSSSSSS (Bit 0)
           |--| |---------------| |------|
           Head      Cylinder      Sector
        */
    } else {
        int spur = block / drive.sect;
        int sect = block % drive.sect + 1;
        // related to ONE disk: per track drive.sect sectors, sectors are counted from 1

        controller.setSectorReg((byte)(sect & 0xff));

        int head = spur % drive.head;
        int cyl  = spur / drive.head;
        // related to ALL disks: per cylinder drive.head tracks
      
        controller.setLoCylReg((byte)(cyl & 0xff));
        controller.setHiCylReg((byte)(cyl >> 8));
        controller.setLDHReg((byte)(head | drive.select));
        /* select.all == 1 L 1 D x x x x, with L == 1 for LBA-Modus, 0 otherwise,
           D == 1 for Slave, 0 for Master, head == xxxx (0 bis 15) */
    }
    }
}
