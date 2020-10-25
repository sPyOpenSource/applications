BETA TEST
=========
J51 a Java Intel 8051 family emulator.

run : java -jar j51.jar

A j51.conf must exist with the supported cpu class names.

FILES on binary distribution
============================
j51.jar		J51 Emulator
j51.conf	Default configuration
diseqc.hex	Software for diseqc emulator
blink.hex	RT Example program require standard P8051
mcb900.hex	Example from KEIL MCB900 IDE 
cmon51.hex	CMON51 compiled with shared memory
tetris52.hex	Tetris for CMON51 or P8051 emulator (run at 0x8000)
lpc936.hex	RT Example for LPC936

Limitations
===========
The J51 disassembler do not work with self modification code like CMON51.

File extension
==============
.flash	- Is for flash memory
.eeprom	- Is for eeprom memory
.misc	- Is for special function memory
