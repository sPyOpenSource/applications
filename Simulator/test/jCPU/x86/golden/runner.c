/*
 * Runs x86-64 code snippets on the real CPU and prints the resulting
 * registers, flags and memory changes.  Used by gen_golden.py to produce the
 * expected values in x86_64.golden; see README.md.
 *
 * Input, one case per line:   <code hex> <16 registers hex> <flags hex>
 * Output, one line per case:  <16 registers hex> <flags hex> <addr=byte,...|->
 *
 * The memory layout must match X86Amd64GoldenTest.java.
 */
#define _GNU_SOURCE
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>

#define CODE_ADDR  0x100000UL
#define CODE_SIZE  0x10000UL
#define DATA_ADDR  0x200000UL
#define DATA_SIZE  0x10000UL
#define STACK_ADDR 0x300000UL
#define STACK_SIZE 0x10000UL

/* CF PF AF ZF SF DF OF */
#define FLAG_MASK 0xCD5UL

struct state { uint64_t reg[16]; uint64_t flags; };

struct state st_in, st_out;
uint64_t host_rsp;
uint64_t code_entry = CODE_ADDR;

void run_case(void);
void ret_stub(void);

__asm__(
    ".intel_syntax noprefix\n"
    ".text\n"
    ".globl run_case\n"
    "run_case:\n"
    "    push rbx\n    push rbp\n    push r12\n    push r13\n    push r14\n    push r15\n"
    "    mov [rip+host_rsp], rsp\n"
    "    push qword ptr [rip+st_in+128]\n"
    "    popfq\n"
    "    mov rcx, [rip+st_in+8]\n    mov rdx, [rip+st_in+16]\n    mov rbx, [rip+st_in+24]\n"
    "    mov rbp, [rip+st_in+40]\n   mov rsi, [rip+st_in+48]\n    mov rdi, [rip+st_in+56]\n"
    "    mov r8,  [rip+st_in+64]\n   mov r9,  [rip+st_in+72]\n    mov r10, [rip+st_in+80]\n"
    "    mov r11, [rip+st_in+88]\n   mov r12, [rip+st_in+96]\n    mov r13, [rip+st_in+104]\n"
    "    mov r14, [rip+st_in+112]\n  mov r15, [rip+st_in+120]\n"
    "    mov rsp, [rip+st_in+32]\n"
    "    mov rax, [rip+st_in+0]\n"
    "    jmp qword ptr [rip+code_entry]\n"
    ".globl ret_stub\n"
    "ret_stub:\n"
    "    mov [rip+st_out+0], rax\n   mov [rip+st_out+8], rcx\n    mov [rip+st_out+16], rdx\n"
    "    mov [rip+st_out+24], rbx\n  mov [rip+st_out+32], rsp\n   mov [rip+st_out+40], rbp\n"
    "    mov [rip+st_out+48], rsi\n  mov [rip+st_out+56], rdi\n   mov [rip+st_out+64], r8\n"
    "    mov [rip+st_out+72], r9\n   mov [rip+st_out+80], r10\n   mov [rip+st_out+88], r11\n"
    "    mov [rip+st_out+96], r12\n  mov [rip+st_out+104], r13\n  mov [rip+st_out+112], r14\n"
    "    mov [rip+st_out+120], r15\n"
    "    mov rsp, [rip+host_rsp]\n"
    "    pushfq\n"
    "    pop rax\n"
    "    mov [rip+st_out+128], rax\n"
    "    cld\n"
    "    pop r15\n    pop r14\n    pop r13\n    pop r12\n    pop rbp\n    pop rbx\n"
    "    ret\n"
    ".att_syntax prefix\n"
);

/* Initial content of the data region; the same formula is in the Java test. */
static uint8_t pattern(uint64_t i) { return (uint8_t)((i * 31 + 7) ^ (i >> 8)); }

static void *map(unsigned long addr, unsigned long size, int prot) {
    void *p = mmap((void *)addr, size, prot,
                   MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED_NOREPLACE, -1, 0);
    if (p != (void *)addr) { perror("mmap"); exit(1); }
    return p;
}

int main(void) {
    uint8_t *code  = map(CODE_ADDR, CODE_SIZE, PROT_READ | PROT_WRITE | PROT_EXEC);
    uint8_t *data  = map(DATA_ADDR, DATA_SIZE, PROT_READ | PROT_WRITE);
    uint8_t *stack = map(STACK_ADDR, STACK_SIZE, PROT_READ | PROT_WRITE);
    static char line[1 << 20];

    while (fgets(line, sizeof line, stdin)) {
        char *tok = strtok(line, " \n");
        size_t n = strlen(tok) / 2;
        if (n + 14 > CODE_SIZE) { fprintf(stderr, "code too long\n"); return 1; }
        memset(code, 0xCC, CODE_SIZE);
        for (size_t i = 0; i < n; i++) {
            unsigned b;
            sscanf(tok + 2 * i, "%2x", &b);
            code[i] = (uint8_t)b;
        }
        /* jmp [rip+0] to the address that follows */
        static const uint8_t jmp[6] = { 0xFF, 0x25, 0, 0, 0, 0 };
        uint64_t target = (uint64_t)ret_stub;
        memcpy(code + n, jmp, 6);
        memcpy(code + n + 6, &target, 8);

        for (int i = 0; i < 16; i++) st_in.reg[i] = strtoull(strtok(NULL, " \n"), NULL, 16);
        st_in.flags = (strtoull(strtok(NULL, " \n"), NULL, 16) & FLAG_MASK) | 0x202;

        for (uint64_t i = 0; i < DATA_SIZE; i++) data[i] = pattern(i);
        memset(stack, 0, STACK_SIZE);

        run_case();

        for (int i = 0; i < 16; i++) printf("%llx ", (unsigned long long)st_out.reg[i]);
        printf("%llx ", (unsigned long long)(st_out.flags & FLAG_MASK));
        int any = 0;
        for (uint64_t i = 0; i < DATA_SIZE; i++)
            if (data[i] != pattern(i)) printf("%s%lx=%02x", any++ ? "," : "", DATA_ADDR + i, data[i]);
        for (uint64_t i = 0; i < STACK_SIZE; i++)
            if (stack[i] != 0) printf("%s%lx=%02x", any++ ? "," : "", STACK_ADDR + i, stack[i]);
        printf("%s\n", any ? "" : "-");
        fflush(stdout);
    }
    return 0;
}
