package jCPU.RiscV;

import static jCPU.RiscV.Esp32c3MemoryMap.GPIO_BASE;
import static jCPU.RiscV.Esp32c3MemoryMap.SRAM_BASE;
import static jCPU.RiscV.Esp32c3MemoryMap.SRAM_SIZE;
import static jCPU.RiscV.Esp32c3MemoryMap.TIMER_BASE;
import static jCPU.RiscV.Esp32c3MemoryMap.UART_BASE;

import java.nio.file.Files;
import java.nio.file.Paths;

public final class Esp32c3Loader {
    private Esp32c3Loader() {}

    public static IsaSim loadImage(byte[] image) throws BusErrorException {
        Esp32c3MemoryMap mem = new Esp32c3MemoryMap();
        UartPeripheral uart = new UartPeripheral();
        GpioPeripheral gpio = new GpioPeripheral();
        TimerPeripheral timer = new TimerPeripheral();
        mem.registerPeripheral(UART_BASE, 0x1000, uart);
        mem.registerPeripheral(GPIO_BASE, 0x1000, gpio);
        mem.registerPeripheral(TIMER_BASE, 0x1000, timer);

        IsaSim sim = new IsaSim();
        sim.setMemoryMap(mem);

        // Copy image into SRAM
        for (int i = 0; i < Math.min(image.length, SRAM_SIZE); i++) {
            mem.writeByte(SRAM_BASE + i, image[i]);
        }
        // Also copy into IsaSim's code Memory so step()/go() can fetch the instructions
        for (int i = 0; i < Math.min(image.length, SRAM_SIZE); i++) {
            sim.code.write(SRAM_BASE + i, image[i]);
        }
        sim.setTimer(timer);
        sim.reset();
        return sim;
    }

    public static void main(String[] args) throws Exception {
        /*if (args.length < 1) {
            System.err.println("Usage: Esp32c3Loader <firmware.bin>");
            System.exit(1);
        }
        byte[] image = Files.readAllBytes(Paths.get(args[0]));*/
        byte[] image = Files.readAllBytes(Paths.get("/Users/xuyi/Downloads/ESP32-C3FH4_FlintJVM_Release.bin"));
        IsaSim sim = loadImage(image);
        // The firmware image is loaded at SRAM_BASE, but reset() parks the PC at
        // the reset vector (0x40000000) where nothing is mapped, so point the PC
        // at the loaded image before starting execution.
        sim.pc = Esp32c3MemoryMap.SRAM_BASE;
        sim.go(0);
    }
}
