# Episode 5 — "I/O, Interrupts & Running a Program"

## Metadata
- **Title:** Build an x86 CPU Emulator in Java — Ep.5: I/O, Interrupts & a Real Program
- **Length target:** 7:00
- **Git tag:** `ep05` (Season 1 finale)

## Thumbnail concept
Screen showing program output "Hi.." in a terminal with a CPU chip fading in. Text: "It Runs!". Subtext: "Ep.5 Finale".

## YouTube description
```
Season 1 finale: we add console I/O (OUT), a timer interrupt with an IVT handler, and load & run a real program from memory. The CPU speaks.

Repo: https://github.com/<handle>/java-x86-emulator  (tag: ep05)
Next: Season 2 (memory operands, stack, calls) — subscribe!
```

## Script (~1,080 words)

**[0:00 — RECAP + FINALE HOOK]**
"Final episode of Season 1. So far: registers, memory, the fetch-decode-execute loop, MOV and ADD. Today the CPU talks to the outside world — I/O and interrupts — and we run a real program. Let's finish."

**[0:45 — PLAIN ENGLISH: I/O]**
"Plain English: **I/O** is how a CPU talks to devices — keyboard, screen, disk. In x86, the `OUT` instruction sends a byte to a **port** (a numbered channel). Port 1 = our console."

**[1:15 — Implement OUT]**
```java
case 0xE6: { // OUT imm8, AL
    int port = fetch();
    int value = gpr[0] & 0xFF; // AL
    if (port == 1) System.out.print((char) value);
    else System.out.println("OUT to unknown port " + port);
    break;
}
```

"And we need MOV AL, imm (8-bit) to load a character:"

```java
case 0xB0: case 0xB1: case 0xB2: case 0xB3:
case 0xB4: case 0xB5: case 0xB6: case 0xB7: {
    int regNum = opcode - 0xB0;
    int imm = fetch();
    gpr[regNum] = (gpr[regNum] & ~0xFF) | (imm & 0xFF);
    break;
}
```

**[2:20 — PLAIN ENGLISH: interrupts]**
"Plain English: an **interrupt** is a tap on the CPU's shoulder: 'stop what you're doing, run this handler, then come back.' A timer interrupt fires every N instructions. The **IVT** (Interrupt Vector Table) at address 0 maps interrupt numbers to handler addresses."

**[2:55 — IVT + dispatch]**
```java
public int irqPending = -1;     // -1 = none
public int timerCounter = 0;
public int timerReload = 5;

// run() loop (HLT waits for an interrupt, then resumes)
while (true) {
    if (halted) {
        if (timerReload == 0) break;          // HLT with no timer -> stop
        while (irqPending < 0) {             // wait for timer IRQ
            timerCounter--;
            if (timerCounter <= 0) { irqPending = 0; timerCounter = timerReload; }
        }
        dispatchIrq(); halted = false; continue;
    }
    timerCounter--;
    if (timerCounter <= 0) { irqPending = 0; timerCounter = timerReload; }
    if (irqPending >= 0) { dispatchIrq(); continue; }
    int op = fetch();
    // ... cases; case 0xF4: halted = true; timerReload = 0; break;
    count++;
    if (irqPending >= 0) dispatchIrq();
}
```

```java
private void dispatchIrq() {
    int vec = irqPending;
    irqPending = -1;
    // push eflags, eip (simplified, no CS)
    push32(eflags);
    push32(eip);
    eip = readDword(vec * 4);   // IVT entry
}
```

"push32/pop32 manage the stack via ESP."

```java
private void push32(int v){ setReg(Reg.ESP, reg(Reg.ESP)-4); writeDword(reg(Reg.ESP), v); }
private int pop32(){ int v=readDword(reg(Reg.ESP)); setReg(Reg.ESP, reg(Reg.ESP)+4); return v; }
```

**[4:00 — IRET]**
"The handler returns with IRET (0xCF): pop eip and eflags."

```java
case 0xCF: { // IRET
    eip = pop32();
    eflags = pop32();
    break;
}
```

**[4:20 — The demo program]**
"We load a program at 0x0100 that prints 'Hi', and an interrupt handler at 0x0200 that prints '.'. IVT[0] = 0x0200."

```java
// IVT entry 0 -> 0x0200
writeDword(0, 0x0200);
// Handler at 0x0200: print '.', IRET
mem[0x0200]=(byte)0xB0; mem[0x0201]=(byte)'.';
mem[0x0202]=(byte)0xE6; mem[0x0203]=1;
mem[0x0204]=(byte)0xCF;
// Program at 0x0100
int p=0x0100;
mem[p++]=(byte)0xB0; mem[p++]='H';
mem[p++]=(byte)0xE6; mem[p++]=1;
mem[p++]=(byte)0xB0; mem[p++]='i';
mem[p++]=(byte)0xE6; mem[p++]=1;
mem[p++]=(byte)0xF4;
cpu.eip = 0x0100;
cpu.run();
```

"Run. Output: `Hi.` — the dot is the timer interrupt firing after the program halts; the handler printed it. The CPU printed, and the interrupt interrupted. 🎉"

**[5:30 — Season 1 recap]**
"Five episodes ago we had an empty file. Now: registers, memory, fetch-decode-execute, MOV, ADD, OUT, interrupts, and a running program. That's a real, if tiny, CPU emulator. You did it."

**[5:55 — What's next / CTA]**
"Season 2 ideas: memory operands (ModR/M fully), stack, CALL/RET, a tiny assembler, loading .com files. Subscribe and comment what you want next. Repo link in description. Checkpoint `git checkout ep05`."

**[6:30 — TRY IT]**
"Try it: change the timer reload to 2 and watch more dots. Or print your name. See you in Season 2!"

## Production notes
- Show output panel with `.Hi.` appearing char by char.
- Highlight IVT table (address 0, 4 bytes).
- End with a montage of all 5 episodes' key moments.

## Final additions (ep05)
- `push32`, `pop32`, `dispatchIrq`, timer logic in `run()`.
- `OUT 0xE6`, `MOV r8,imm` (0xB0..0xB7), `IRET 0xCF`.
- `HLT 0xF4` sets `halted=true` and `timerReload=0` so the program ends after the last interrupt.
- Demo loader in `Main`.
