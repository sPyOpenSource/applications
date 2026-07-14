# Learning Record 0003: ARM Implementation Comparison

## Context
Comparing the two ARM implementations in the simulator suite: JARM (`jCPU.arm.CPU`) and lxtreme ARM9 (`nl.lxtreme.arm.CPU`).

## Insight
JARM is not a separate implementation but an extension of the lxtreme ARM9. It uses the latter as a "lite" core and adds full-system features.

### Divergence Points:
- **Fidelity**: lxtreme is a "simplistic core"; JARM is a "full system emulator".
- **State**: JARM introduces `ProcessorMode` and banked registers (SP, LR, SPSR), whereas lxtreme uses a flat register file.
- **Memory**: JARM implements a two-tier memory model (`PhysicalMemorySpace` and `VirtualMemorySpace`) to support an MMU, while lxtreme uses a simple memory interface.
- **System Control**: JARM adds Coprocessors, specifically CP15 for system configuration and an FPU for floating point operations.
- **Execution**: JARM uses a cycle-budgeted execution model to better simulate timing and interrupts.

## Impact on Future Work
When adding new ARM instructions:
- Simple instructions can be added to the base `nl.lxtreme.arm.CPU` (if they are core ARM/Thumb).
- System-level instructions or those affecting processor state/MMU must be implemented in `jCPU.arm.CPU` or its associated coprocessors.
