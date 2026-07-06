# SFR listener mechanism: how peripherals talk to the 8051 CPU

The user asked to learn about SFR (Special Function Registers). They've been working with peripherals in the J51 simulator (VGA, ports, UART) and needed to understand the communication backbone.

We traced the SFR architecture from top to bottom:

1. **MCS51Constants.java** defines symbolic address constants (ACC=0xE0, P0=0x80, etc.)
2. **SfrPage** (extends VolatileMemory) manages 256 SfrRegister objects
3. **VolatileMemory.write()** is the hot path — sets the byte value, then iterates `MemoryByte.mw[]` to notify listeners
4. **MemoryWriteListenerSfr** adapts low-level `MemoryWriteListener` to high-level `SfrWriteListener`
5. Peripherals (Uart, G128x64, BytePort) implement `SfrWriteListener.sfrWrite(reg, val)` and register via `cpu.addSfrWriteListener(addr, this)`

Key insight: the adapter chain decouples the general memory-write interface (which includes oldValue) from the peripheral-friendly SFR interface (register + value only). The `writeBusy` flag prevents recursive listener re-entry.

**Implications:** Next lesson could cover the instruction dispatch itself — how `MCS51.java`'s 256-entry opcode table maps incoming bytes to `AbstractOpcode` subclasses (the 8051 counterpart to RISC-V's `switch` in `IsaSim.java`).
