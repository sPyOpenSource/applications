# Resources: USB Mass Storage

## Specifications (Primary Sources)

| Resource | Type | Link | Notes |
|----------|------|------|-------|
| **USB Mass Storage Class Specification v1.0** | Spec | [usb.org](https://www.usb.org/document-library/mass-storage-class-specification-10) | The definitive spec for MSC. Covers Bulk-Only Transport (BOT). |
| **SCSI Primary Commands - 4 (SPC-4)** | Spec | [t10.org](https://www.t10.org/members/w_spc4.htm) | INQUIRY, REQUEST SENSE, TEST UNIT READY, etc. |
| **SCSI Block Commands - 3 (SBC-3)** | Spec | [t10.org](https://www.t10.org/members/w_sbc3.htm) | READ(10), WRITE(10), READ CAPACITY(10), SYNCHRONIZE CACHE. |
| **USB 2.0 Specification** | Spec | [usb.org](https://www.usb.org/document-library/usb-20-specification) | Chapter 9 (device framework), bulk endpoints. |

## Reference Implementations (High Trust)

| Resource | Type | Link | Notes |
|----------|------|------|-------|
| **Linux kernel `drivers/usb/storage/`** | Source | [elixir.bootlin.com](https://elixir.bootlin.com/linux/latest/source/drivers/usb/storage) | Production-grade host driver. See `protocol.c` for BOT, `scsi.c` for SCSI translation. |
| **TinyUSB `src/class/msc/msc_host.c`** | Source | [github.com](https://github.com/hathach/tinyusb/tree/master/src/class/msc) | Clean embedded host implementation. Good for bare-metal. |
| **Zephyr `subsys/usb/host/class/usbh_msc.c`** | Source | [github.com](https://github.com/zephyrproject-rtos/zephyr/tree/main/subsys/usb/host/class) | RTOS-integrated host MSC. |
| **FreeBSD `sys/dev/usb/storage/`** | Source | [cgit.freebsd.org](https://cgit.freebsd.org/src/tree/sys/dev/usb/storage) | Alternative clean implementation. |

## Learning Resources

| Resource | Type | Link | Notes |
|----------|------|------|-------|
| **USB in a Nutshell - Mass Storage** | Tutorial | [beyondlogic.org](https://www.beyondlogic.org/usbnutshell/usb4.shtml) | Excellent visual explanation of BOT protocol. |
| **SCSI Commands Reference (Seagate)** | Reference | [seagate.com](https://www.seagate.com/staticfiles/support/disc/manuals/Interface%20Manuals/100293068j.pdf) | Practical SCSI command reference. |
| **Implementing a USB Mass Storage Host** | Article | [interrupt.memfault.com](https://interrupt.memfault.com/blog/usb-mass-storage-host) | Memfault blog - practical embedded perspective. |
| **OSDev Wiki - USB Mass Storage** | Wiki | [wiki.osdev.org](https://wiki.osdev.org/USB_Mass_Storage) | Hobbyist OS perspective, good for block device layer. |

## Tools for Learning

| Tool | Purpose |
|------|---------|
| **Wireshark + USBPcap** | Capture USB traffic on Windows |
| **usbmon + Wireshark** | Capture USB traffic on Linux |
| **Beagle USB 480** | Hardware protocol analyzer (if budget allows) |
| **QEMU USB mass storage emulation** | Test without hardware: `-device usb-storage,drive=hd0` |