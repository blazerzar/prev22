# prev22

This is a compiler done as part of the undergraduate course
**Compilers** at *Faculty of Computer and Information Science* in
Ljubljana.

## Compiler

Compiler is written in Java and compiles prev22 source code into MMIX
assembly. MMIX assembler *mmixal* is used to compile assembly code into
machine code, which is run using simulator *mmix*. ANTLR library is used for
lexical, syntax and abstract analysis.

## Specification

Specification for the language is written in [prev22.pdf](prev22.pdf).

## Usage

First the compiler needs to be compiled using
```
make
```
in the root directory. Then prev22 programs in the [prg](/prg) directory can
be compiled and run using
```
make run FILE=file NREGS=registers
```
in the same directory as the program itself. Here `file` is the name of the
source code file without the .p22 extension and `registers` is the number
of registers used by the final program and at least 2. To successfully
compile and run the final program [mmix](/prg/mmix) and
[mmixal](/prg/mmixal) need to have execute permission.

## Links

- ANTLR: https://www.antlr.org/
- MMIX:  https://mmix.cs.hm.edu/