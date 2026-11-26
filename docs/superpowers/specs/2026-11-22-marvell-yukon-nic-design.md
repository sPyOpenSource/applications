# Marvell Yukon 88E8055 NIC Emulation — Design

## Goal

Add a minimal-functionality emulated Marvell Yukon 88E8055 PCI-E Gigabit Ethernet
controller to the x86 simulator, connected to the host network via pcap, so guest
software can send/receive real Ethernet packets.

## Decisions

- **Hardware identity:** Marvell Yukon 88E8055 (vendor `0x11AB`, device ID for the
  88E8055). Registers presented as a simplified register set — not full Yukon
  register fidelity, but enough for a guest driver to detect and use the NIC.
- **Interface:** I/O ports `0x300–0x31F` via the existing `jCPU.Peripheral`
  interface (`read(int port)` / `write(int port, byte)`). No new core interfaces.
- **Host connectivity:** pcap on a background daemon thread. Host interface selected
  at construction time; if pcap cannot be opened, the NIC is still present but the
  RX queue stays empty and TX sends are dropped with an error status.
- **Scope:** Minimal functional NIC. No memory-mapped registers, no descriptor
  rings, no new IRQ/PIC system. Interrupts are exposed as a status register bit
  rather than a CPU interrupt line.
- **Integration:** Instantiated and port-registered in `CPU.java` constructor,
  alongside VGA, UART, IDE, and UHCI.

## Component changes

### New file: `Simulator/src/jCPU/x86/X86EthernetNic.java`

```text
package jCPU.x86;

implements jCPU.Peripheral
```

**Register map (ports `0x300–0x31F`):**

| Port  | Access | Purpose                                      |
|-------|--------|----------------------------------------------|
| 0x300 | R      | Vendor ID low  (`0xAB` = Marvell)            |
| 0x301 | R      | Vendor ID high (`0x11`)                      |
| 0x302 | R      | Device ID low  (Yukon 88E8055)               |
| 0x303 | R      | Device ID high (Yukon 88E8055)               |
| 0x304–0x309 | R/W | MAC address (6 bytes)                     |
| 0x310 | W      | TX data port — write one byte into TX buffer |
| 0x311 | W      | TX length low                              |
| 0x312 | W      | TX length high                             |
| 0x313 | W      | TX command (`0x01` = send packet)          |
| 0x314 | R      | TX status (`0x01` ready, `0x02` sending, `0x04` error) |
| 0x320 | R      | RX data port — read one byte from RX buffer |
| 0x321 | R      | RX length low                              |
| 0x322 | R      | RX length high                             |
| 0x323 | W      | RX command (`0x01` = acknowledge consumed) |
| 0x324 | R      | RX status (`0x01` = packet available)      |
| 0x330 | R/W    | Interrupt status (read/clear; bit 0 = RX, bit 1 = TX complete) |

**Internal state:**
- `byte[] txBuffer` / `int txLength` — guest-assembled transmit packet
- `ConcurrentLinkedQueue<Byte> rxQueue` — packets received from host
- `byte[] rxBuffer` / `int rxLength` — current packet being read by guest
- `int interruptStatus` — pending interrupt bits
- pcap handle (opened in constructor, read on daemon thread)
- `boolean pcapAvailable` — false if pcap could not be opened

**PCAP backend:**
- Constructor accepts a host interface name (or `null` for no network).
- Opens pcap handle for live capture + injection.
- Daemon thread loops `pcap_next_ex()`, enqueuing each Ethernet frame's bytes
  into `rxQueue` and setting the RX-available interrupt bit.
- TX send: when guest writes command `0x01` at `0x313`, the NIC drains
  `txBuffer[0..txLength)` and injects via `pcap_sendpacket()`, then sets the
  TX-complete interrupt bit.

### Modified file: `Simulator/src/jCPU/x86/CPU.java`

- In the constructor, instantiate `X86EthernetNic` and register ports `0x300–0x31F`.
- Pass the host interface name (constructor parameter or system property; `null`
  by default so existing behavior is unchanged).

No other source files are changed.

## Data flow

**Guest transmit:**
1. Guest writes packet bytes one at a time to port `0x310` (fills `txBuffer`).
2. Guest writes packet length to ports `0x311` (low) and `0x312` (high).
3. Guest writes `0x01` to port `0x313` (TX command).
4. NIC injects `txBuffer[0..txLength)` into the host network via pcap.
5. NIC sets TX-complete bit in `interruptStatus` and clears TX-ready bit.

**Host receive:**
1. PCAP daemon thread reads an Ethernet frame from the host interface.
2. Frame bytes are enqueued into `rxQueue`.
3. NIC sets RX-available bit in `interruptStatus`.
4. Guest reads packet length from ports `0x321/0x322`, then bytes from `0x320`,
   then writes `0x01` to port `0x323` to acknowledge consumption.

**Interrupts:**
- Guest reads port `0x330` to see pending interrupt bits.
- Guest writes to port `0x330` to clear bits (write value = bitmask to clear).

## Error handling

| Condition                        | Behavior                                                        |
|----------------------------------|-----------------------------------------------------------------|
| pcap interface cannot be opened  | NIC registers remain readable; RX queue empty; TX sends dropped; TX error bit set |
| TX packet exceeds buffer size    | Set TX error bit; drop packet                                   |
| RX queue overflow                | Drop oldest enqueued packet; set RX error bit                   |
| Unknown port read                | Return 0                                                        |
| Unknown port write               | Ignore                                                          |
| pcap read/write error            | Log to stderr; continue (never crash emulator)                  |

## Testing

**Unit test: `Simulator/test/jCPU/x86/X86EthernetNicTest.java`**
- `testRegisterReads` — vendor ID reads back `0x11AB`, device ID correct
- `testMacAddressReadWrite` — write MAC bytes, read them back
- `testTransmitCycle` — write TX data + length + command; TX status transitions; TX-complete interrupt bit set
- `testReceiveCycle` — enqueue RX bytes directly (bypass pcap); RX-available status; read data; acknowledge clears status
- `testInterruptClear` — write to interrupt-status port clears bits

No pcap in unit tests (requires a host interface). pcap behavior is verified
manually by booting jxcore and checking that NIC registers are readable from the
guest.

## Out of scope

- Full Marvell Yukon register set (SK-Ge / Yukon-2 / Yukon-Extreme)
- Descriptor-based DMA and ring buffers
- CPU interrupt line / PIC integration
- Guest-side NIC driver
- VLAN tagging, offloading, link-state negotiation

## Files

- Create: `Simulator/src/jCPU/x86/X86EthernetNic.java`
- Create: `Simulator/test/jCPU/x86/X86EthernetNicTest.java`
- Modify: `Simulator/src/jCPU/x86/CPU.java`