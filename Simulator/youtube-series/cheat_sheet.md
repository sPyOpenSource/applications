# Cheat Sheet — Java x86 Emulator (Season 1)

## Registers (8 GPRs + EIP + EFLAGS)
| Index | Name | Index | Name |
|---|---|---|---|
| 0 | EAX (AL=low byte) | 4 | ESI |
| 1 | EBX | 5 | EDI |
| 2 | ECX | 6 | EBP |
| 3 | EDX | 7 | ESP |

## Memory
- 64 KB byte array, little-endian, unsigned access via `& 0xFF`.

## Opcodes implemented
| Opcode | Mnemonic | Effect |
|---|---|---|
| 0xB0..0xB7 | MOV r8, imm8 | load byte into register low byte |
| 0xB8..0xBF | MOV r32, imm32 | load 32-bit immediate |
| 0x05 | ADD EAX, imm32 | EAX += imm |
| 0x01 | ADD r/m32, r32 | reg-reg add (mod=3) |
| 0xE6 | OUT imm8, AL | port 1 = console char |
| 0xCD? (no) | — | — |
| 0xCF | IRET | return from interrupt |
| 0xF4 | HLT | halt |

## Key helpers
```java
int readByte(int a){ return mem[a]&0xFF; }
void writeByte(int a,int v){ mem[a]=(byte)(v&0xFF); }
int readWord(int a){ return readByte(a)|(readByte(a+1)<<8); }
int readDword(int a){ return readByte(a)|(readByte(a+1)<<8)|(readByte(a+2)<<16)|(readByte(a+3)<<24); }
void writeDword(int a,int v){ writeByte(a,v); writeByte(a+1,v>>8); writeByte(a+2,v>>16); writeByte(a+3,v>>24); }
```

## Fetch-decode-execute
```java
int op = fetch(); // readByte(eip++); 
switch(op){ ... }
```

## ModR/M (reg-reg only)
```
bits 7-6: mod (11 = reg-reg)
bits 5-3: source register
bits 2-0: destination register
```

## Interrupts (minimal)
- IVT at address 0, 4 bytes per vector.
- Timer decrements each instruction; on 0 sets irqPending=0.
- Dispatch: push eflags, eip; eip = readDword(vec*4).
- IRET restores eip, eflags.
