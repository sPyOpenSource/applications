package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** Preload hints (PLD/PLI/PLDW) are architecturally NOPs and must not fault. */
public class PreloadHintTest {

    private static final int[] PRELOAD_HINTS = {
        0xF5D0F000, // pld [r0]          (op1=93)
        0xF5D0F008, // pld [r0, #8]
        0xF7D0F001, // pld [r0, r1]      (op1=125)
        0xF7D0F101, // pld [r0, r1, lsl #2]
        0xF4D0F000, // pli [r0]          (op1=77)
        0xF4D0F008, // pli [r0, #8]
        0xF6D0F001, // pli [r0, r1]      (op1=109)
    };

    @Test
    public void testPreloadHintsAreNoOps() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        for (int iword : PRELOAD_HINTS) {
            cpu.executeARM(iword);
        }
    }
}
