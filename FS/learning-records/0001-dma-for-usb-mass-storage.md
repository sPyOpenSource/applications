# Learning Record 0001: DMA for USB Mass Storage

## Date
2026-08-11

## Lesson
0001-dma-for-usb-mass-storage.html

## Key Insights

1. **Three address spaces**: Virtual (CPU code), Physical (MMU), Bus/DMA (controller). On bare metal without MMU, VA=PA=DMA_ADDR but alignment/cache rules still apply.

2. **DMA buffer requirements**:
   - Physically contiguous (controller can't follow page tables)
   - DMA-accessible (some SoCs restrict DMA to certain RAM regions)
   - Cache-coherent (CPU cache vs DMA writes)
   - Properly aligned (UHCI: TD=16B, QH=32B, Frame List=4KB)

3. **Two DMA mapping types**:
   - **Coherent** (dma_alloc_coherent): For descriptor rings (TD/QH) — small, long-lived, fixed mapping
   - **Streaming** (dma_map_single/unmap_single): For data buffers — reused, larger, direction-specific

4. **Cache coherency**:
   - TX (DMA_TO_DEVICE): Clean cache before DMA (write-back)
   - RX (DMA_FROM_DEVICE): Invalidate cache after DMA
   - Linux dma_map/unmap handles this; bare metal needs explicit SCB_CleanDCache/InvalidateDCache

5. **UHCI-specific**:
   - Frame List: 1024 entries × 4 bytes = 4KB, must be 4KB-aligned
   - TD: 32 bytes, 16-byte aligned
   - QH: 32 bytes, 32-byte aligned

## Quiz Results
- Q1: ✓ malloc vs dma_alloc_coherent — physically contiguous requirement
- Q2: ✓ dma_map_single for streaming buffers
- Q3: ✓ Invalidate cache after DMA_FROM_DEVICE
- Q4: ✓ Frame list 4KB alignment for frame indexing

## Connected to Mission
This directly enables the USB mass storage BOT implementation:
- CBW/CSW buffers need DMA mapping
- Data phase buffers (up to 64KB chunks) need streaming DMA
- TD/QH rings need coherent allocation
- The breakpoint crashes in the UHCI driver were likely DMA memory issues

## Next Steps
- Audit existing UHCI driver for proper DMA allocation
- Implement dma_alloc_coherent / dma_map_single equivalents for bare metal
- Test with actual hardware (QEMU + USB storage)

## Questions for Next Session
1. How to implement dma_alloc_coherent on bare metal Cortex-M?
2. What's the DMA region on STM32F4/STM32H7?
3. How to handle scatter-gather for >64KB transfers?