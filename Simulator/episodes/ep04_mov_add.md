# Episode 4 — "Your First Real Instructions (MOV, ADD)"

## Metadata
- **Title:** Build an x86 CPU Emulator in Java — Ep.4: MOV & ADD
- **Length target:** 7:00
- **Git tag:** `ep04`

## Thumbnail concept
Code snippet `MOV EAX, 2` then `ADD EAX, 3` with `EAX = 5` highlighted in green. Text: "First Instructions". Subtext: "Ep.4".

## YouTube description
```
Episode 4: we implement our first real x86 instructions — MOV and ADD — and run a tiny program that computes 2 + 3. The CPU is alive.

Repo: https://github.com/<handle>/java-x86-emulator  (tag: ep04)
```

## Script (~1,060 words)

**[0:00 — RECAP + HOOK]**
"Last episode the engine ran but did nothing. Today we give it two real instructions — MOV and ADD — and run a program that computes 2 + 3. This is the moment the CPU comes alive."

**[0:45 — PLAIN ENGLISH: opcode & immediate]**
"Plain English: an **immediate** is a number baked directly into the instruction. `MOV EAX, 5` means 'put 5 into EAX' — the 5 travels with the instruction. We'll read those bytes right after the opcode."

**[1:20 — MOV r32, imm32]**
"x86 encodes 'move immediate into register' as 0xB8 through 0xBF — one opcode per register. The low 3 bits tell us WHICH register. Then 4 bytes follow as the immediate."

```java
case 0xB8: case 0xB9: case 0xBA: case 0xBB:
case 0xBC: case 0xBD: case 0xBE: case 0xBF: {
    int regNum = opcode - 0xB8;          // 0..7
    int imm = readDword(eip);            // 4 bytes, little-endian
    eip += 4;
    gpr[regNum] = imm;
    break;
}
```

"Clean. `readDword` reads 4 bytes little-endian — we added it in ep2's exercise; here's the implementation if you skipped it."

```java
public int readDword(int addr){
    return readByte(addr) | (readByte(addr+1)<<8)
         | (readByte(addr+2)<<16) | (readByte(addr+3)<<24);
}
```

**[2:45 — ADD EAX, imm32]**
"0x05 means 'add immediate to EAX'. Read 4 bytes, add, store back."

```java
case 0x05: { // ADD EAX, imm32
    int imm = readDword(eip); eip += 4;
    gpr[0] += imm;            // EAX is index 0
    updateFlags(gpr[0]);
    break;
}
```

**[3:30 — ADD reg, reg (0x01)]**
"Now the fun one: adding two registers. Opcode 0x01 is followed by a **ModR/M byte** — a single byte that says 'source register' and 'destination register'. For register-to-register we only need mod=11."

```java
case 0x01: { // ADD r/m32, r32
    int modrm = fetch();
    int mod = (modrm >> 6) & 3;
    int src = (modrm >> 3) & 7;
    int dst = modrm & 7;
    if (mod == 3) { // register to register
        gpr[dst] += gpr[src];
        updateFlags(gpr[dst]);
    } else {
        System.out.println("Memory operands not yet supported");
        halted = true;
    }
    break;
}
```

"ModR/M looks scary but it's just bit fields. We'll do memory operands in a later series."

**[4:30 — updateFlags]**
"Flags tell us about the result — was it zero? Did it overflow? We'll track just Zero Flag for now."

```java
private void updateFlags(int result) {
    if (result == 0) eflags |= 0x40;   // ZF (bit 6)
    else eflags &= ~0x40;
}
```

**[5:00 — Run a real program]**
"Hand-assemble in `Main`: MOV EAX, 2; ADD EAX, 3 (via immediate). Let's use immediates for clarity."

Bytes:
```
B8 02 00 00 00   ; MOV EAX, 2
05 03 00 00 00   ; ADD EAX, 3
F4               ; HLT
```

```java
byte[] prog = {
    (byte)0xB8, 2,0,0,0,
    (byte)0x05, 3,0,0,0,
    (byte)0xF4
};
for (int i=0;i<prog.length;i++) cpu.mem[i]=prog[i];
cpu.run();
System.out.println("EAX = " + cpu.reg(Cpu.Reg.EAX));  // 5
```

"Run it. `EAX = 5`. The CPU computed. 🎉"

**[5:50 — Recap + checkpoint]**
"Recap: MOV r32,imm loads a constant; ADD adds. We read immediates as 4 bytes little-endian, and decoded a ModR/M byte for reg-reg. Our CPU now actually computes.

Checkpoint: `git checkout ep04`."

**[6:25 — TRY IT]**
"Try it: implement SUB (0x2D for EAX, immediate) mirroring ADD. Print EAX. See you next episode — I/O and interrupts!"

## Production notes
- Show the byte-level program as a hex dump panel.
- Highlight EAX changing from 0 → 2 → 5 on screen.
- Animate ModR/M bit fields.

## Final code state (ep04) — added to switch
```java
case 0xB8: case 0xB9: case 0xBA: case 0xBB:
case 0xBC: case 0xBD: case 0xBE: case 0xBF: {
    int regNum = opcode - 0xB8;
    int imm = readDword(eip); eip += 4;
    gpr[regNum] = imm; break;
}
case 0x05: {
    int imm = readDword(eip); eip += 4;
    gpr[0] += imm; updateFlags(gpr[0]); break;
}
case 0x01: {
    int modrm = fetch();
    int mod=(modrm>>6)&3, src=(modrm>>3)&7, dst=modrm&7;
    if (mod==3){ gpr[dst]+=gpr[src]; updateFlags(gpr[dst]); }
    else { System.out.println("mem operands unsupported"); halted=true; }
    break;
}
```
Plus `readDword` and `updateFlags`.
