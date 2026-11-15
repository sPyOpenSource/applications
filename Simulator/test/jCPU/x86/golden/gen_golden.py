#!/usr/bin/env python3
"""Generate x86_64.golden: instruction snippets with the result the real CPU gives.

Needs an x86-64 Linux machine with gcc and binutils.  See README.md.
"""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REGS = ['rax', 'rcx', 'rdx', 'rbx', 'rsp', 'rbp', 'rsi', 'rdi',
        'r8', 'r9', 'r10', 'r11', 'r12', 'r13', 'r14', 'r15']

CF, PF, AF, ZF, SF, DF, OF = 0x1, 0x4, 0x10, 0x40, 0x80, 0x400, 0x800
STATUS = CF | PF | AF | ZF | SF | OF

# Registers at the start of every case, unless the case overrides them.
# rbp, rsi, rdi, r12 and r13 point into the data region; r14 and r15 are
# small so they can serve as index registers.
DEFAULT = dict(rax=0x1122334455667788, rcx=0x00000000000000C3, rdx=0xFEDCBA9876543210,
               rbx=0x8000000000000001, rsp=0x308000, rbp=0x200300, rsi=0x200200,
               rdi=0x200100, r8=0xFFFFFFFFFFFFFFFF, r9=0x7FFFFFFFFFFFFFFF,
               r10=0x0000000080000000, r11=0x00000000FFFFFFFF, r12=0x200400,
               r13=0x200500, r14=3, r15=5)
# A second set with values at the edges.
EDGE = dict(rax=0, rdx=0xFFFFFFFFFFFFFFFF, rbx=0x7FFFFFFFFFFFFFFF, rcx=1,
            r8=0x8000000000000000, r9=1, r10=0xFFFFFFFF, r11=0x7FFFFFFF)

cases = []


def case(asm, regs=None, flags=0, undef=0, code=None):
    """asm: GNU as, Intel syntax.  undef: flags the instruction leaves undefined."""
    cases.append(dict(asm=asm, regs=dict(regs or {}), flags=flags, undef=undef, code=code))


def both(asm, flags=0, undef=0):
    case(asm, flags=flags, undef=undef)
    case(asm, regs=EDGE, flags=flags, undef=undef)


# ---------------------------------------------------------------- ALU
for op in ['add', 'or', 'adc', 'sbb', 'and', 'sub', 'xor', 'cmp']:
    undef = AF if op in ('or', 'and', 'xor') else 0
    for operands in [
            'al, bl', 'ah, bl', 'bl, ah', 'sil, dl', 'r9b, r8b', 'ax, dx', 'eax, edx',
            'rax, rdx', 'r8, r9', 'r10d, r11d', 'r15w, ax', 'rax, r15', 'r12, rbx',
            '[rdi], rax', 'dword ptr [rsi+8], edx', 'byte ptr [r12], bl',
            'word ptr [r13+0x10], dx', '[rdi+r14*8+0x10], rbx',
            'rax, [rbp]', 'edx, [r12+r15*4]', 'bl, [rsi+0x1234]', 'r9w, [r13]',
            'al, 0x7f', 'ax, 0x8001', 'eax, 0x12345678', 'rax, -0x12345678',
            'rbx, 1', 'ebx, -1', 'bx, 0x7fff', 'bl, 0x80', 'r8, 1', 'r11d, 0x80000000',
            'word ptr [rdi], 0x1234', 'byte ptr [rsi], 0x80', 'qword ptr [rdi+8], -2',
            'dword ptr [r12+r14*4], 0x7fffffff', 'qword ptr [rbp+0x100], 0x12345678']:
        for flags in ([0, CF] if op in ('adc', 'sbb') else [0]):
            both('%s %s' % (op, operands), flags=flags, undef=undef)

for op in ['test']:
    for operands in ['al, bl', 'eax, edx', 'rax, rdx', 'r8w, r9w', '[rdi], rbx',
                     'byte ptr [rsi], 0x80', 'al, 0x81', 'eax, 0x80000000', 'rax, -1',
                     'ebx, 0x10', 'rbx, 0x7fffffff', 'bh, 0x80', 'dword ptr [r12], 0xff00']:
        both('test ' + operands, flags=CF | OF, undef=AF)

for op in ['inc', 'dec', 'neg', 'not']:
    for operand in ['al', 'bh', 'bx', 'ebx', 'rbx', 'r8', 'r9d', 'r10b', 'byte ptr [rdi]',
                    'word ptr [rsi+2]', 'dword ptr [r12]', 'qword ptr [r13+r14*8]']:
        both('%s %s' % (op, operand), flags=CF)
        both('%s %s' % (op, operand), flags=0)

# ---------------------------------------------------------------- shifts and rotates
BITS = {'al': 8, 'bh': 8, 'bx': 16, 'edx': 32, 'rax': 64, 'r8': 64, 'r9d': 32,
        'byte ptr [rdi]': 8, 'word ptr [rsi]': 16, 'dword ptr [r12]': 32,
        'qword ptr [r13]': 64}
for op in ['rol', 'ror', 'rcl', 'rcr', 'shl', 'shr', 'sar']:
    for operand, bits in BITS.items():
        def shift_case(count, text, regs=None):
            masked = count & (63 if bits == 64 else 31)
            undef = 0
            if masked != 0:
                if masked != 1:
                    undef |= OF
                if op in ('shl', 'shr', 'sar'):
                    undef |= AF
                    if masked >= bits:
                        undef |= CF
            # The carry flag is an input only for RCL and RCR.
            for flags in ((0, CF) if op in ('rcl', 'rcr') else (0,)):
                case('%s %s, %s' % (op, operand, text), regs=regs, flags=flags, undef=undef)
                if regs is None:
                    case('%s %s, %s' % (op, operand, text), regs=EDGE, flags=flags, undef=undef)
        for count in (1, 3, 7, 8, 31, 32, 33, 63):
            shift_case(count, str(count))
        case('%s %s, 1' % (op, operand), flags=OF | CF,
             undef=AF if op in ('shl', 'shr', 'sar') else 0)
        for count in (0, 1, 4, 9, 17, 31, 32, 37, 63, 64, 65, 255):
            shift_case(count, 'cl', regs=dict(rcx=count))

# ---------------------------------------------------------------- addressing modes
MEMS = ['[rdi]', '[rdi+8]', '[rdi-8]', '[rdi+0x1000]', '[rsp]', '[rsp+8]', '[rsp-0x100]',
        '[rbp]', '[rbp+8]', '[rbp+0x400]', '[r12]', '[r12+0x7f]', '[r13]', '[r13-0x80]',
        '[r13+0x80]', '[rdi+r14]', '[rdi+r14*2]', '[rdi+r14*4+4]', '[rdi+r14*8+0x100]',
        '[r12+r15*8]', '[r13+r14*4]', '[rbp+r15*2]', '[rsp+r15*8]', '[r14*8+0x200000]',
        '[r15*4+0x200020]', '[0x200040]', '[rip+0x10]', '[rip+0x200]', '[rsi+rdi*1-0x200000]',
        '[edi]', '[edi+r14d*4]', '[r12d+8]', '[eip+0x10]']
for mem in MEMS:
    case('lea rax, %s' % mem)
    case('lea eax, %s' % mem)
    case('lea r9w, %s' % mem)
    case('mov rax, %s' % mem)
    case('mov r10d, dword ptr %s' % mem)
    case('mov bx, word ptr %s' % mem)
    case('mov dl, byte ptr %s' % mem)
    case('movzx eax, byte ptr %s' % mem)
    if 'rip' not in mem and 'eip' not in mem:
        case('mov %s, rdx' % mem)
        case('mov dword ptr %s, ebx' % mem)
        case('mov word ptr %s, r8w' % mem)
        # AH cannot be combined with a REX prefix, which r8..r15 need.
        case('mov byte ptr %s, %s' % (mem, 'bl' if 'r1' in mem else 'ah'))
        case('mov qword ptr %s, -0x12345678' % mem)
        case('mov dword ptr %s, 0x89abcdef' % mem)
        case('mov word ptr %s, 0x1234' % mem)
        case('mov byte ptr %s, 0x5a' % mem)
        case('add byte ptr %s, 0x11' % mem)
        case('cmp qword ptr %s, 0x7f' % mem)
        case('imul eax, dword ptr %s, 0x1234' % mem, undef=SF | ZF | AF | PF)
        case('shl qword ptr %s, 5' % mem, undef=OF | AF)
        case('test dword ptr %s, 0x8000' % mem, undef=AF)

# ---------------------------------------------------------------- mov and friends
for asm in ['mov rax, 0x1122334455667788', 'mov r15, 0xfedcba9876543210', 'mov rax, -1',
            'mov rbx, 0x7fffffff', 'mov eax, 0x80000000', 'mov r9d, 0xffffffff',
            'mov ax, 0x1234', 'mov r10w, 0xffff', 'mov al, 0x12', 'mov ah, 0x34',
            'mov bh, 0x56', 'mov sil, 0x78', 'mov r9b, 0x9a', 'mov spl, 1',
            'mov rbx, rax', 'mov rax, rbx', 'mov r8, rax', 'mov rax, r8', 'mov r15, r14',
            'mov ebx, eax', 'mov r9d, edx', 'mov edx, r9d', 'mov bx, dx', 'mov r8w, ax',
            'mov bl, al', 'mov ah, bl', 'mov bl, ah', 'mov ch, dh', 'mov dil, al',
            'mov al, dil', 'mov r8b, bl', 'mov bl, r8b', 'mov r15b, r14b',
            'movabs al, [0x200010]', 'movabs ax, [0x200010]', 'movabs eax, [0x200010]',
            'movabs rax, [0x200010]', 'movabs [0x200010], al', 'movabs [0x200010], ax',
            'movabs [0x200010], eax', 'movabs [0x200010], rax',
            'movzx eax, bl', 'movzx eax, bh', 'movzx rax, bl', 'movzx ax, bl',
            'movzx eax, bx', 'movzx rax, r8w', 'movzx r9d, r8b', 'movzx r9, word ptr [rdi]',
            'movsx eax, bl', 'movsx eax, bh', 'movsx rax, bl', 'movsx ax, bl',
            'movsx eax, bx', 'movsx rax, r8w', 'movsx r9d, r8b', 'movsx r9, word ptr [rdi]',
            'movsx rdx, byte ptr [rsi+1]', 'movsxd rax, ebx', 'movsxd rax, r10d',
            'movsxd r9, dword ptr [rdi]', 'movsxd rdx, r11d',
            'xchg eax, ebx', 'xchg rax, rbx', 'xchg rax, r8', 'xchg r8d, eax', 'xchg ax, bx',
            'xchg rbx, rdx', 'xchg ebx, edx', 'xchg bl, dl', 'xchg ah, bl', 'xchg [rdi], rbx',
            'xchg dword ptr [rsi], edx', 'xchg byte ptr [r12], al', 'xchg r9, r10',
            'nop', 'xchg ax, ax', 'pause', 'nop dword ptr [rax+rax*1+0]',
            'nop word ptr [rax+rax*1+0x100]', 'nop dword ptr [rax]', 'nop qword ptr [rip+0]',
            '.byte 0x2e\n mov eax, ebx', '.byte 0x3e\n mov [rdi], rbx', 'lock add [rdi], eax',
            'lock inc qword ptr [rsi]', 'cs nop word ptr [rax+rax*1+0]']:
    both(asm)

# ---------------------------------------------------------------- stack
for asm in ['push rax', 'push r12', 'push rsp', 'push bx', 'push 0x12345678', 'push -1',
            'push -0x80000000', 'push 0x7f', 'push qword ptr [rdi]', 'push qword ptr [rsp]',
            'push word ptr [rsi]',
            'push rax\n pop rbx', 'push rdx\n pop r15', 'push rax\n pop rsp',
            'push rdx\n pop qword ptr [rsi]', 'push rdx\n pop qword ptr [rsp]',
            'push rdx\n pop qword ptr [rsp+8]', 'push dx\n pop bx', 'push rdx\n pop word ptr [rdi]',
            'pushfq\n pop rax', 'push 0x8d5\n popfq', 'push 0\n popfq', 'pushfq\n pop rbx\n push rbx\n popfq',
            'push rbp\n mov rbp, rsp\n sub rsp, 0x20\n mov [rbp-8], rax\n leave',
            'sub rsp, 0x18\n mov [rsp+8], rdx\n add rsp, 0x18',
            'call 1f\n 1: pop rax', 'call 1f\n jmp 2f\n 1: mov ebx, 1\n ret\n 2: nop',
            'push rdx\n push rax\n call 1f\n jmp 2f\n 1: mov ebx, [rsp+8]\n ret 16\n 2: nop',
            'lea rax, [rip+1f]\n call rax\n jmp 2f\n 1: mov ebx, 7\n ret\n 2: nop',
            'lea rax, [rip+1f]\n mov [rdi], rax\n call qword ptr [rdi]\n jmp 2f\n 1: mov ebx, 7\n ret\n 2: nop',
            'lea r9, [rip+1f]\n jmp r9\n mov ebx, 1\n 1: nop',
            'lea rax, [rip+1f]\n mov [rsi+r14*8], rax\n jmp qword ptr [rsi+r14*8]\n mov ebx, 1\n 1: nop',
            'lea rax, [rip+1f]\n push rax\n ret\n mov ebx, 1\n 1: nop',
            'jmp 1f\n mov ebx, 1\n 1: nop', 'jmp 1f\n .skip 300, 0x90\n mov ebx, 1\n 1: nop',
            'mov ecx, 3\n 1: jmp 2f\n 2: dec ecx\n jnz 1b',
            'sub rsp, 0x18\n call 1f\n add rsp, 0x18\n jmp 2f\n 1: mov rax, 0xc277\n ret\n 2: nop']:
    both(asm)

# ---------------------------------------------------------------- conditions
CC = ['o', 'no', 'b', 'ae', 'e', 'ne', 'be', 'a', 's', 'ns', 'p', 'np', 'l', 'ge', 'le', 'g']
FLAG_SETS = [0, CF, ZF, SF, OF, PF, CF | ZF, SF | OF, ZF | SF, ZF | OF, CF | SF | OF,
             STATUS, AF, SF | PF, OF | PF | CF]
for cc in CC:
    for flags in FLAG_SETS:
        case('j%s 1f\n mov ebx, 1\n 1: nop' % cc, flags=flags)
        case('j%s 1f\n .skip 200, 0x90\n mov ebx, 1\n 1: nop' % cc, flags=flags)
        case('set%s al' % cc, flags=flags)
        case('set%s byte ptr [rdi+1]' % cc, flags=flags)
        case('set%s r9b' % cc, flags=flags)
        case('cmov%s rax, rdx' % cc, flags=flags)
        case('cmov%s eax, edx' % cc, flags=flags)
        case('cmov%s ax, dx' % cc, flags=flags)
        case('cmov%s r9, [rdi]' % cc, flags=flags)
    for a, b in [(1, 2), (2, 1), (5, 5), (0x8000000000000000, 1), (1, 0x8000000000000000),
                 (0xFFFFFFFFFFFFFFFF, 0), (0x7FFFFFFFFFFFFFFF, 0xFFFFFFFFFFFFFFFF)]:
        case('cmp rax, rdx\n j%s 1f\n mov ebx, 1\n 1: nop' % cc, regs=dict(rax=a, rdx=b))
        case('cmp eax, edx\n set%s bl' % cc, regs=dict(rax=a, rdx=b))
        case('cmp al, dl\n cmov%s rbx, rdi' % cc, regs=dict(rax=a, rdx=b))

for count in (0, 1, 3):
    case('1: inc eax\n loop 1b', regs=dict(rcx=count or 4))
    case('jrcxz 1f\n mov ebx, 1\n 1: nop', regs=dict(rcx=count))
    case('jecxz 1f\n mov ebx, 1\n 1: nop', regs=dict(rcx=count << 32))
    for flags in (0, ZF):
        case('loope 1f\n mov ebx, 1\n 1: nop', regs=dict(rcx=count + 1), flags=flags)
        case('loopne 1f\n mov ebx, 1\n 1: nop', regs=dict(rcx=count + 1), flags=flags)

# ---------------------------------------------------------------- multiply and divide
MULU = SF | ZF | AF | PF
for operand in ['bl', 'bh', 'bx', 'ebx', 'rbx', 'r8', 'r9d', 'r10w', 'byte ptr [rdi]',
                'word ptr [rsi]', 'dword ptr [r12]', 'qword ptr [r13]']:
    both('mul ' + operand, undef=MULU)
    both('imul ' + operand, undef=MULU)
for operands in ['rax, rdx', 'eax, edx', 'ax, dx', 'r8, r9', 'r10d, r11d', 'rbx, [rdi]',
                 'ebx, [rsi+4]', 'rax, rdx, 7', 'rax, rdx, -7', 'eax, edx, 0x12345678',
                 'ax, dx, 0x1234', 'r9, [rdi], -0x12345678', 'ebx, ebx, 100', 'rbx, rbx, -1',
                 'r8w, [r12], 3']:
    both('imul ' + operands, undef=MULU)
for a, b in [(0, 5), (3, 5), (0xFFFFFFFF, 2), (0x7FFFFFFF, 2), (0x40000000, 2), (0xFFFFFFFFFFFFFFFF, 0xFFFFFFFFFFFFFFFF),
             (0x8000000000000000, 0xFFFFFFFFFFFFFFFF), (0x100000000, 0x100000000), (0x7F, 2), (0x80, 0xFF)]:
    for asm in ['mul rdx', 'mul edx', 'mul dx', 'mul dl', 'imul rdx', 'imul edx', 'imul dx',
                'imul dl', 'imul rax, rdx', 'imul eax, edx', 'imul ax, dx']:
        case(asm, regs=dict(rax=a, rdx=b), undef=MULU)

DIVS = [  # (asm, rax, rdx, rbx)
    ('div bl', 0x1234, 0, 0x7F), ('div bl', 0xFE01, 0, 0xFF), ('div bh', 0x00FF, 0, 0x1000),
    ('idiv bl', 0xFF80, 0, 3), ('idiv bl', 0x007F, 0, 0xFF), ('idiv bl', 0xC080, 0, 0x7F),
    ('div bx', 0x5678, 0x1234, 0x9000), ('idiv bx', 0x0001, 0xFFFF, 0x7), ('idiv bx', 0x8000, 0xFFFF, 0xFFFE),
    ('div ebx', 0x12345678, 0x1, 0x10000), ('div ebx', 0xFFFFFFFF, 0xFFFFFFFE, 0xFFFFFFFF),
    ('idiv ebx', 0xFFFFFF00, 0xFFFFFFFF, 0x10), ('idiv ebx', 0x80000000, 0, 0xFFFFFFFE),
    ('idiv ebx', 0x7FFFFFFF, 0, 0x80000000),
    ('div rbx', 0x123456789ABCDEF0, 0x1, 0x1234567890), ('div rbx', 0xFFFFFFFFFFFFFFFF, 0, 1),
    ('div rbx', 0, 0xFFFFFFFFFFFFFFFE, 0xFFFFFFFFFFFFFFFF),
    ('idiv rbx', 0xFFFFFFFFFFFFFF00, 0xFFFFFFFFFFFFFFFF, 0x10),
    ('idiv rbx', 0x8000000000000000, 0xFFFFFFFFFFFFFFFF, 2),
    ('idiv rbx', 0x123456789ABCDEF0, 0x12, 0xFFFFFFFF00000000),
    ('cqo\n idiv rbx', 0x8000000000000001, 0, 0xFFFFFFFFFFFFFFFF),
    ('cdq\n idiv ebx', 0x80000001, 0x55, 7), ('cwd\n idiv bx', 0x8001, 0x55, 7),
    ('xor edx, edx\n div rbx', 0xFFFFFFFFFFFFFFFF, 5, 10),
    ('mov qword ptr [rdi], 12345\n cqo\n idiv qword ptr [rdi]', 0xFFFFFFFFFFF00000, 0, 0),
    ('mov dword ptr [rsi], 77\n xor edx, edx\n div dword ptr [rsi]', 0xFFFFFFFF, 0, 0),
    ('mov byte ptr [r12], 9\n div byte ptr [r12]', 0x0456, 0, 0),
    ('mov r9, 1000\n xor edx, edx\n div r9', 0x123456789, 0, 0),
]
for asm, a, d, b in DIVS:
    case(asm, regs=dict(rax=a, rdx=d, rbx=b), undef=STATUS)

# ---------------------------------------------------------------- string instructions
for size, suffix in [(1, 'b'), (2, 'w'), (4, 'd'), (8, 'q')]:
    for std in ('', 'std\n '):
        regs = dict(rsi=0x200200 + 0x40, rdi=0x200100 + 0x40)
        case(std + 'movs' + suffix, regs=regs)
        case(std + 'stos' + suffix, regs=regs)
        case(std + 'lods' + suffix, regs=regs)
        case(std + 'scas' + suffix, regs=regs)
        case(std + 'cmps' + suffix, regs=regs)
        for count in (0, 1, 5):
            r = dict(regs, rcx=count)
            case(std + 'rep movs' + suffix, regs=r)
            case(std + 'rep stos' + suffix, regs=r)
            case(std + 'rep lods' + suffix, regs=r)
            case(std + 'repe cmps' + suffix, regs=r, flags=ZF)
            case(std + 'repne cmps' + suffix, regs=r)
            case(std + 'repe scas' + suffix, regs=r)
            case(std + 'repne scas' + suffix, regs=r, flags=ZF)
case('mov ecx, 32\n rep movsb\n mov esi, 0x200200\n mov edi, 0x200100\n mov ecx, 40\n repe cmpsb')
case('mov al, [rdi+9]\n mov ecx, 100\n repne scasb')
case('xor eax, eax\n mov ecx, 16\n rep stosq\n mov edi, 0x200100\n mov ecx, 64\n repe scasw')
case('mov ecx, 3\n rep movsq\n mov ecx, 3\n rep movsd\n mov ecx, 3\n rep movsw')
case('addr32 rep movsb', regs=dict(rcx=0x100000004, rsi=0x100200200, rdi=0x100200100))
case('addr32 stosd', regs=dict(rdi=0x100200100))

# ---------------------------------------------------------------- the rest
for asm in ['cbw', 'cwde', 'cdqe', 'cwd', 'cdq', 'cqo']:
    for a in (0x7F, 0x80, 0x7FFF, 0x8000, 0x7FFFFFFF, 0x80000000, 0x1122334455667788,
              0x8000000000000000, 0xFFFFFFFFFFFFFFFF):
        case(asm, regs=dict(rax=a))
for flags in (0, CF, STATUS, ZF | SF, DF | CF):
    for asm in ['lahf', 'clc', 'stc', 'cmc', 'cld', 'std', 'mov ah, 0xd5\n sahf',
                'mov ah, 0x2a\n sahf', 'mov ah, 0x41\n sahf']:
        case(asm, flags=flags)
for asm in ['bswap eax', 'bswap rax', 'bswap r9', 'bswap r10d', 'bswap rdx']:
    both(asm)

BT_UNDEF = OF | SF | AF | PF
for op in ['bt', 'bts', 'btr', 'btc']:
    for operands in ['eax, 5', 'rax, 63', 'ax, 3', 'rdx, 40', 'r9, 62', 'eax, 35',
                     'dword ptr [rdi], 7', 'qword ptr [rsi], 33', 'word ptr [r12], 15',
                     'dword ptr [r13], 100']:
        both('%s %s' % (op, operands), undef=BT_UNDEF)
    for off in (0, 1, 31, 32, 63, 64, 100, 1000, 0xFFFFFFFFFFFFFFFF, 0xFFFFFFFFFFFFFF00,
                0xFFFFFFFFFFFFFC18, 0xFFFFFFFF, 0x80000000):
        for operands in ['rax, rdx', 'eax, edx', 'ax, dx', 'r8, rdx', '[rbp], rdx',
                         '[rbp], edx', '[rbp], dx', '[r12+r14*8], rdx']:
            # With a memory operand these offsets reach hundreds of megabytes away.
            if '[' in operands and off in (0xFFFFFFFF, 0x80000000):
                continue
            case('%s %s' % (op, operands), regs=dict(rdx=off), undef=BT_UNDEF)

BS_UNDEF = CF | OF | SF | AF | PF
for v in (1, 0x8000, 0x80000000, 0x8000000000000000, 0x0000F00000000000, 0x1234567800000000,
          0xFFFFFFFFFFFFFFFF, 0x10000):
    for asm in ['bsf rax, rdx', 'bsr rax, rdx', 'bsf r9, rdx', 'bsr r9, rdx']:
        case(asm, regs=dict(rdx=v), undef=BS_UNDEF)
    if v & 0xFFFFFFFF:
        case('bsf eax, edx', regs=dict(rdx=v), undef=BS_UNDEF)
        case('bsr eax, edx', regs=dict(rdx=v), undef=BS_UNDEF)
    if v & 0xFFFF:
        case('bsf ax, dx', regs=dict(rdx=v), undef=BS_UNDEF)
        case('bsr ax, dx', regs=dict(rdx=v), undef=BS_UNDEF)
case('bsf rax, rdx', regs=dict(rdx=0), undef=BS_UNDEF)
case('bsr rax, rdx', regs=dict(rdx=0), undef=BS_UNDEF)
case('mov qword ptr [rdi], 0x500\n bsf rbx, [rdi]\n bsr rcx, [rdi]', undef=BS_UNDEF)

for operands in ['al, bl', 'ax, bx', 'eax, ebx', 'rax, rbx', 'r8, r9', 'rax, rax', 'eax, eax',
                 'bl, bl', '[rdi], al', '[rdi], bx', '[rsi], edx', '[r12], rdx', '[r13], r8']:
    both('xadd ' + operands)
for a in (0x1122334455667788, 0xFEDCBA9876543210, 0x77, 0x3210):
    for operands in ['dl, bl', 'dx, bx', 'edx, ebx', 'rdx, rbx', 'r9, r8', '[rdi], bl',
                     '[rdi], bx', '[rdi], ebx', '[rdi], rbx', 'rax, rbx', 'eax, ebx']:
        case('cmpxchg ' + operands, regs=dict(rax=a))
case('mov rax, [rdi]\n cmpxchg [rdi], rbx')
case('mov eax, [rdi]\n lock cmpxchg [rdi], ebx')
case('mov al, [rdi]\n cmpxchg [rdi], bl')
case('mov ax, [rdi]\n cmpxchg [rdi], bx')

# A few longer hand-written sequences, including the one from the UEFI image
# that first showed the instruction-length fault.
case('push rsp\n push rbx\n sub rsp, 0x18\n lea rax, [rip+1f]\n call rax\n'
     ' mov rax, 0xc277\n add rsp, 0x18\n pop rbx\n pop rcx\n jmp 2f\n'
     ' 1: mov rbx, [rsp+8]\n add rbx, rax\n test rbx, rbx\n ret\n 2: nop')
case('mov qword ptr [r13], 0x200600\n mov dword ptr [0x200600], 0x676d696d\n'
     ' mov qword ptr [0x200608], 0x40\n mov rax, [r13+0]\n test rax, rax\n jz 1f\n'
     ' cmp dword ptr [rax], 0x676d696d\n jne 1f\n mov rbx, [rax+8]\n add rbx, rax\n'
     ' test rbx, rbx\n jz 1f\n mov rdx, [rbx]\n 1: nop')
case('xor eax, eax\n mov ecx, 10\n 1: add rax, rcx\n dec rcx\n jnz 1b\n shl rax, 4\n neg rax')
case('mov rcx, 0x200100\n xor edx, edx\n 1: movzx eax, byte ptr [rcx+rdx]\n add ebx, eax\n'
     ' rol ebx, 3\n inc rdx\n cmp rdx, 64\n jb 1b')


# ---------------------------------------------------------------- compiled C functions
def compiled_cases(tmp):
    src = os.path.join(HERE, 'functions.c')
    flags = ['-ffreestanding', '-fno-stack-protector', '-mgeneral-regs-only', '-fno-jump-tables',
             '-fcf-protection=none', '-fno-pic', '-fno-asynchronous-unwind-tables',
             '-fno-builtin', '-w']
    args = [(3, 7), (0x1122334455667788, 0xFEDCBA9876543210), (0xFFFFFFFFFFFFFFFF, 2),
            (12, 0x8000000000000000)]
    for opt in ['-O0', '-O1', '-O2', '-Os']:
        obj = os.path.join(tmp, 'f%s.o' % opt)
        subprocess.run(['gcc', '-c', opt] + flags + ['-o', obj, src], check=True)
        # Link, so that calls between functions are resolved; they stay relative.
        elf = os.path.join(tmp, 'f.elf')
        subprocess.run(['ld', '-e', 'fn_sum', '-o', elf, obj], check=True)
        headers = subprocess.run(['objdump', '-h', elf], check=True, capture_output=True, text=True).stdout
        sections = [l.split() for l in headers.splitlines() if l.split()[:1] and l.split()[0].isdigit()]
        if any(sec[1].startswith(('.rodata', '.data', '.bss')) for sec in sections):
            sys.exit('functions.c %s: uses global data, cannot be used as a snippet' % opt)
        base = next(int(sec[3], 16) for sec in sections if sec[1] == '.text')
        raw = os.path.join(tmp, 'f.bin')
        subprocess.run(['objcopy', '-O', 'binary', '-j', '.text', elf, raw], check=True)
        text = open(raw, 'rb').read()
        syms = subprocess.run(['nm', '-S', elf], check=True, capture_output=True, text=True).stdout
        for line in sorted(syms.splitlines(), key=lambda l: l.split()[-1]):
            parts = line.split()
            if len(parts) != 4 or not parts[3].startswith('fn_'):
                continue
            name, start, size = parts[3], int(parts[0], 16) - base, int(parts[1], 16)
            body = text[start:start + size]
            # call fn; jmp end; fn: ...
            code = b'\xE8\x05\x00\x00\x00' + b'\xE9' + len(body).to_bytes(4, 'little') + body
            for a, b in args:
                case('gcc %s: %s(0x%x, 0x%x, 0x200100)' % (opt, name, a, b),
                     regs=dict(rdi=a, rsi=b, rdx=0x200100), code=code, undef=STATUS)


def assemble(tmp):
    src = os.path.join(tmp, 'cases.s')
    with open(src, 'w') as f:
        f.write('.intel_syntax noprefix\n.text\n')
        for i, c in enumerate(cases):
            if c['code'] is None:
                f.write('case_%d:\n %s\n' % (i, c['asm']))
        f.write('case_end:\n')
    obj = os.path.join(tmp, 'cases.o')
    subprocess.run(['as', '--64', '-o', obj, src], check=True)
    raw = os.path.join(tmp, 'cases.bin')
    subprocess.run(['objcopy', '-O', 'binary', '-j', '.text', obj, raw], check=True)
    text = open(raw, 'rb').read()
    syms = subprocess.run(['nm', obj], check=True, capture_output=True, text=True).stdout
    offsets = sorted(int(l.split()[0], 16) for l in syms.splitlines() if 'case_' in l)
    asm_cases = [c for c in cases if c['code'] is None]
    assert len(offsets) == len(asm_cases) + 1
    for c, start, end in zip(asm_cases, offsets, offsets[1:]):
        c['code'] = text[start:end]


def main():
    with tempfile.TemporaryDirectory() as tmp:
        compiled_cases(tmp)
        assemble(tmp)
        runner = os.path.join(tmp, 'runner')
        subprocess.run(['gcc', '-O1', '-o', runner, os.path.join(HERE, 'runner.c')], check=True)
        lines = []
        for c in cases:
            regs = dict(DEFAULT, **c['regs'])
            c['init'] = [regs[r] for r in REGS]
            lines.append('%s %s %x' % (c['code'].hex(), ' '.join('%x' % v for v in c['init']), c['flags']))
        run = subprocess.run([runner], input='\n'.join(lines) + '\n', capture_output=True, text=True)
        out = run.stdout.splitlines()
        if run.returncode != 0:
            sys.exit('the real CPU faulted on case %d: %s' % (len(out), cases[len(out)]['asm']))

    with open(os.path.join(HERE, 'x86_64.golden'), 'w') as f:
        f.write('# Generated by gen_golden.py; do not edit.  Fields, separated by "|":\n'
                '#   assembly | code | initial registers | initial flags | undefined flags |\n'
                '#   changed registers | expected flags | changed memory bytes\n'
                '# Initial registers not listed have these values:\n'
                '#   %s\n' % ','.join('%s=%x' % (r, DEFAULT[r]) for r in REGS))
        for c, result in zip(cases, out):
            fields = result.split()
            final = [int(v, 16) for v in fields[:16]]
            init = ','.join('%s=%x' % (r, v) for r, v in zip(REGS, c['init']) if v != DEFAULT[r])
            changed = ','.join('%s=%x' % (r, v) for r, v, v0 in zip(REGS, final, c['init']) if v != v0)
            f.write('|'.join([c['asm'].replace('\n', ';'), c['code'].hex(), init, '%x' % c['flags'],
                              '%x' % c['undef'], changed, fields[16], fields[17]]) + '\n')
    print('%d cases written' % len(cases))


if __name__ == '__main__':
    main()
