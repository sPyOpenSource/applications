# Episode 2 — "Registers & Memory in Java"

## Metadata
- **Title:** Build an x86 CPU Emulator in Java — Ep.2: Registers & Memory
- **Length target:** 7:00
- **Git tag:** `ep02`

## Thumbnail concept
IDE screenshot with two highlighted boxes: "REGISTERS (8 slots)" in cyan and "MEMORY (64KB array)" in green. Text: "Registers & Memory". Subtext: "Ep.2".

## YouTube description
```
Episode 2: we give our CPU its working memory — registers (the fast scratch slots) and RAM (the bigger, slower desk). We model both in plain Java.

Repo: https://github.com/<handle>/java-x86-emulator  (tag: ep02)

Timestamps:
0:00 Recap
1:00 What are registers?
2:30 Modeling registers in Java (enum + array)
4:00 Memory: a byte array
5:15 Read/write helpers
6:15 Checkpoint + exercise
```

## Script (~1,040 words)

**[0:00 — RECAP]**
"Welcome back. Last episode we set up the project and made an empty `Cpu` class. Today we give it a brain's working memory: **registers** and **RAM**. These are the two places a CPU stores data, and modeling them is surprisingly simple."

**[0:45 — PLAIN ENGLISH: registers]**
"Plain English: **registers** are tiny, super-fast storage slots inside the CPU. Think of them as the CPU's hands — it can only do math on things it's holding. Memory (RAM) is the desk: bigger, but slower to reach."

**[1:30 — Which registers?]**
"For a beginner emulator we don't need all 20+ x86 registers. We'll implement the classic eight general-purpose ones: EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP — plus EIP (instruction pointer) and EFLAGS. That's enough to run real-ish code."

**[2:15 — Modeling registers in Java]**
(Screen: IDE.)

"We could use eight separate `int` fields, but an array is cleaner and lets us index by register number — which matters when an instruction says 'use register 3'. Let's use an enum for names and an int array for values."

```java
package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];   // general purpose
    public int eip;                        // instruction pointer
    public int eflags;                     // flags

    public int reg(Reg r) { return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v) { gpr[r.ordinal()] = v; }
}
```

"See how `Reg.EAX.ordinal()` gives us 0, EBX gives 1, and so on? That maps perfectly to x86's register numbering. Clean."

**[3:30 — Memory]**
"Now memory. In a real machine, RAM is a giant array of bytes. We'll emulate 64 KB — small, but plenty for learning. One `byte[]` of size 65536."

```java
public final byte[] mem = new byte[65536]; // 64 KB RAM
```

"But Java bytes are signed (-128..127), and memory is unsigned (0..255). We'll write helpers that mask to unsigned."

**[4:30 — Read/write helpers]**
```java
public int readByte(int addr) {
    return mem[addr] & 0xFF;          // unsigned
}
public void writeByte(int addr, int value) {
    mem[addr] = (byte)(value & 0xFF);
}
// 16-bit and 32-bit helpers follow the same pattern
public int readWord(int addr) {
    return (readByte(addr)) | (readByte(addr+1) << 8);
}
```

"Little-endian note: x86 stores the low byte first. That's why `addr` is the low byte and `addr+1` is shifted left 8. We'll revisit this."

**[5:30 — reset()]**
```java
public void reset() {
    eip = 0;
    eflags = 0;
    for (int i = 0; i < gpr.length; i++) gpr[i] = 0;
    // ESP starts at top of memory in many emulators
    setReg(Reg.ESP, 0x10000);
}
```

**[6:00 — Recap + checkpoint]**
"We now have registers, memory, and read/write helpers. That's the 'desk' and the 'hands'. Next episode: the heartbeat — the fetch-decode-execute loop that ties them together.

Checkpoint: `git checkout ep02`."

**[6:30 — TRY IT]**
"Try it: add a `readDword`/`writeDword` (32-bit) helper. Hint: four bytes, little-endian. See you next episode!"

## Production notes
- Highlight the `& 0xFF` trick with a red box; explain signed bytes.
- Show a memory hex-dump visualization (a small table) as B-roll.

## Final code state (ep02)
```java
package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];
    public int eip;
    public int eflags;
    public final byte[] mem = new byte[65536];

    public int reg(Reg r){ return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v){ gpr[r.ordinal()] = v; }

    public int readByte(int addr){ return mem[addr] & 0xFF; }
    public void writeByte(int addr, int value){ mem[addr]=(byte)(value & 0xFF); }
    public int readWord(int addr){ return readByte(addr)|(readByte(addr+1)<<8); }
    public void writeWord(int addr,int value){ writeByte(addr,value); writeByte(addr+1,value>>8); }

    public void reset(){
        eip=0; eflags=0;
        for(int i=0;i<gpr.length;i++) gpr[i]=0;
        setReg(Reg.ESP,0x10000);
    }
    public void run(){ /* episode 3 */ }
}
```
