# Episode 3 — "The Fetch-Decode-Execute Loop"

## Metadata
- **Title:** Build an x86 CPU Emulator in Java — Ep.3: Fetch-Decode-Execute
- **Length target:** 7:00
- **Git tag:** `ep03`

## Thumbnail concept
Animated loop diagram: three arrows FETCH → DECODE → EXECUTE circling a CPU. Text: "The Heartbeat". Subtext: "Ep.3".

## YouTube description
```
Episode 3: the heartbeat of every CPU — the fetch-decode-execute loop. We write the main loop in Java and dispatch opcodes through a switch. No instructions yet, but the engine runs.

Repo: https://github.com/<handle>/java-x86-emulator  (tag: ep03)
```

## Script (~1,050 words)

**[0:00 — RECAP]**
"Last time we built registers and memory. Today: the heartbeat. Every CPU on Earth runs the same three-step loop, and by the end of this episode ours will too."

**[0:45 — PLAIN ENGLISH: fetch-decode-execute]**
"Plain English: **Fetch** = read the next instruction from memory. **Decode** = figure out what it means. **Execute** = do it. Then repeat, forever, until halt. That's the whole job of a CPU."

**[1:20 — The loop skeleton]**
(Screen: IDE.)

"Here's the loop. Watch how small it is."

```java
public void run() {
    boolean halted = false;
    while (!halted) {
        int opcode = fetch();        // step 1
        switch (opcode) {           // step 2 + 3
            case 0x00: /* ... */ break;
            default:
                System.out.println("Unknown opcode: 0x" + Integer.toHexString(opcode));
                halted = true;
        }
    }
}
```

"The `while` runs forever. `fetch()` grabs one byte. The `switch` decodes and executes. Unknown opcode? We halt — safety first."

**[2:15 — fetch()]**
```java
private int fetch() {
    int b = readByte(eip);   // read byte at instruction pointer
    eip++;                   // advance pointer
    return b;
}
```

"Two things happen: read the byte at EIP, then bump EIP. The CPU always knows where the next instruction is because of EIP."

**[3:00 — Why a switch? ]**
"x86 instructions are identified by a one-byte **opcode** — a number like 0xB8 means 'move immediate into EAX'. So a `switch` on the opcode byte is the natural dispatch. We'll fill in cases next episode."

**[3:45 — Add a halt opcode]**
"Let's give ourselves a way to stop. We'll use 0xF4 (x86's HLT) as our halt."

```java
case 0xF4: // HLT
    halted = true;
    break;
```

**[4:15 — Test the loop]**
"Before we have real instructions, let's hand-assemble a tiny program: just HLT. We'll poke bytes into memory in `Main`."

```java
public static void main(String[] args) {
    Cpu cpu = new Cpu();
    cpu.reset();
    cpu.mem[0] = (byte)0xF4;   // HLT at address 0
    cpu.run();
    System.out.println("CPU halted. EIP=" + cpu.eip);
}
```

"Run it. You should see 'CPU halted. EIP=1'. The loop fetched 0xF4, halted, and EIP advanced once. The engine works!"

**[5:15 — Step mode (bonus)]**
"Debugging tip: add a `step()` method that runs ONE iteration, so you can print state. Super useful."

```java
public void step() {
    int op = fetch();
    // dispatch single (reuse logic) ...
}
```

"We'll keep it simple: `run()` loops, `step()` does one. Viewers can use either."

**[5:45 — Recap + checkpoint]**
"Recap: fetch reads a byte and bumps EIP; decode+execute is a switch on the opcode; HLT stops. Our engine now runs. Next episode: real instructions — MOV and ADD — so the CPU actually computes.

Checkpoint: `git checkout ep03`."

**[6:20 — TRY IT]**
"Try it: add a counter that prints how many instructions executed. Hint: increment inside the while loop. See you next episode!"

## Production notes
- Animate the fetch-decode-execute loop with three colored arrows.
- Zoom on `eip++` — emphasize the pointer advancing.
- Show the "Unknown opcode" path by feeding a random byte first.

## Final code state (ep03)
```java
package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];
    public int eip, eflags;
    public final byte[] mem = new byte[65536];

    public int reg(Reg r){ return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v){ gpr[r.ordinal()] = v; }
    public int readByte(int addr){ return mem[addr] & 0xFF; }
    public void writeByte(int addr, int value){ mem[addr]=(byte)(value & 0xFF); }

    public void reset(){ eip=0; eflags=0; for(int i=0;i<gpr.length;i++) gpr[i]=0; setReg(Reg.ESP,0x10000); }

    private int fetch(){ int b=readByte(eip); eip++; return b; }

    public void run(){
        boolean halted=false; int count=0;
        while(!halted){
            int op=fetch();
            switch(op){
                case 0xF4: halted=true; break;
                default:
                    System.out.println("Unknown opcode: 0x"+Integer.toHexString(op));
                    halted=true;
            }
            count++;
        }
        System.out.println("Executed "+count+" instruction(s).");
    }
}
```
