# RISC-V
A simple RISC-V instruction set architecture simulator written in Java. The program supports reading in programs as binary files and it produces a small text file indicating the content of each of the registers in the register file. The simulator is not pipelined - it implements a simple single-cycle processor.

Guide
=====
The simulator runs a given program when it is run from the command line. The simulator can be set up using a set of constants stored in the top of the IsaSim.java file. These constants are
- ```FILEPATH``` (string) indicating the relative path to the binary file to execute (no default value is provided)
- ```INITIAL_PC``` (integer) indicating the initial value of the program counter (default is ```0```, as programs are read into memory starting from memory address 0)
- ```INITIAL_SP``` (integer) indicating the initial value of the stack pointer (default is ```2^31 - 1 = Integer.MAX_VALUE```)
- ```DEBUGGING``` (boolean) indicating whether debugging prints to the terminal are wanted or not (default is ```true```)

When a program has been executed, the program prints a .txt file and a binary file in the directory in which the test program is stored. The files are named after the program executed followed by ```_reg```. The .txt file contains both the post execution register content from the simulator as well as the expected/correct register content provided to the program in corresponding binary .res files. The binary file contains only the post execution register content.

# 8051

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
