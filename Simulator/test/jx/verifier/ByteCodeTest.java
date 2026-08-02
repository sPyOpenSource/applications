package jx.verifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import jx.verifier.bytecode.BCBranch;
import jx.verifier.bytecode.BCLinkList;
import jx.verifier.bytecode.ByteCode;
import jx.verifier.bytecode.ByteIterator;

/**
 * Unit tests for {@link ByteCode}: instruction parsing, opcode dispatch,
 * size/name/argument handling, and CFG target linking.
 */
public class ByteCodeTest {

    @Test
    public void parsesZeroArgumentInstruction() throws VerifyException {
        ByteCode bc = new ByteCode(new byte[]{0x03}); // iconst_0
        assertEquals(ByteCode.ICONST_0, bc.getOpCode());
        assertEquals("iconst_0", bc.getBCName());
        assertEquals(1, bc.getSize());
        assertNull(bc.getByteArgs());
    }

    @Test
    public void parsesOneArgumentInstruction() throws VerifyException {
        ByteCode bc = new ByteCode(new byte[]{0x10, 0x7f}); // bipush 127
        assertEquals(ByteCode.BIPUSH, bc.getOpCode());
        assertEquals("bipush", bc.getBCName());
        assertEquals(2, bc.getSize());
        assertArrayEquals(new byte[]{0x7f}, bc.getByteArgs());
    }

    @Test
    public void parsesTwoArgumentInstruction() throws VerifyException {
        ByteCode bc = new ByteCode(new byte[]{0x11, 0x12, 0x34}); // sipush 0x1234
        assertEquals(ByteCode.SIPUSH, bc.getOpCode());
        assertEquals(3, bc.getSize());
        assertArrayEquals(new byte[]{0x12, 0x34}, bc.getByteArgs());
    }

    @Test
    public void serializesBackToOriginalBytes() throws VerifyException {
        byte[] original = {0x11, 0x12, 0x34}; // sipush 0x1234
        ByteCode bc = new ByteCode(original);
        byte[] out = new byte[4];
        int end = bc.toByteArray(0, out);
        assertEquals(3, end);
        byte[] roundTrip = new byte[end];
        System.arraycopy(out, 0, roundTrip, 0, end);
        assertArrayEquals(original, roundTrip);
    }

    @Test
    public void newByteCodeDispatchesPlainOpcode() {
        ByteCode bc = ByteCode.newByteCode(new ByteIterator(new byte[]{0x60}), null, null); // iadd
        assertEquals(ByteCode.IADD, bc.getOpCode());
        assertFalse(bc instanceof BCBranch);
    }

    @Test
    public void newByteCodeDispatchesBranchOpcode() {
        ByteCode bc = ByteCode.newByteCode(new ByteIterator(new byte[]{(byte) 0x99, 0x00, 0x05}), null, null); // ifeq
        assertTrue(bc instanceof BCBranch);
        assertEquals(5, ((BCBranch) bc).getTargetAddress()); // address 0 + offset 5
    }

    @Test
    public void newByteCodeRequiresConstantPoolForCpArgOps() {
        try {
            ByteCode.newByteCode(new ByteIterator(new byte[]{(byte) 0xb6, 0x00, 0x01}), null, null); // invokevirtual
            fail("expected Error for cPool == null");
        } catch (Error e) {
            assertTrue(e.getMessage().contains("cPool"));
        }
    }

    @Test(expected = Error.class)
    public void newByteCodeRejectsOutOfRangeOpcode() {
        ByteCode.newByteCode(new ByteIterator(new byte[]{(byte) 0xff}), null, null);
    }

    @Test
    public void terminalInstructionLinksNoTargets() throws VerifyException {
        BCLinkList list = new BCLinkList(new byte[]{(byte) 0xac}, null); // ireturn
        assertEquals(0, list.getFirst().getTargets().length);
    }

    @Test
    public void ordinaryInstructionLinksFallThroughTarget() throws VerifyException {
        BCLinkList list = new BCLinkList(new byte[]{0x03, 0x04, (byte) 0xac}, null); // iconst_0 iconst_1 ireturn
        ByteCode first = list.getFirst();
        assertEquals(1, first.getTargets().length);
        assertEquals(1, first.getTargets()[0].getAddress());
    }

    @Test
    public void conditionalBranchLinksTwoTargets() throws VerifyException {
        // iconst_0; ifeq +4; iconst_1; ireturn  → branch target is address 5
        BCLinkList list = new BCLinkList(new byte[]{0x03, (byte) 0x99, 0x00, 0x04, 0x04, (byte) 0xac}, null);
        ByteCode ifeq = list.getBCAt(1);
        assertEquals(2, ifeq.getTargets().length);
        assertEquals(4, ifeq.getTargets()[0].getAddress());  // fall-through
        assertEquals(5, ifeq.getTargets()[1].getAddress());  // branch target
    }

    @Test(expected = VerifyException.class)
    public void branchToInvalidAddressThrows() throws VerifyException {
        // iconst_0; goto +2 → target 3, which is mid-instruction (not a boundary)
        new BCLinkList(new byte[]{0x03, (byte) 0xa7, 0x00, 0x02}, null);
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
