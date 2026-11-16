/*
 * Small integer-only functions, compiled by gen_golden.py at several
 * optimisation levels and run both on the real CPU and in the emulator.
 * They give the emulator instruction sequences as a compiler emits them.
 *
 * Rules: no calls to other functions (self-recursion is fine), no global
 * data, no floating point.  Arguments are (a, b, p) where p points into the
 * data region of the test.
 */
typedef unsigned long u64;
typedef long i64;
typedef unsigned int u32;
typedef unsigned char u8;

u64 fn_sum(u64 a, u64 b, u8 *p) {
    u64 s = 0;
    for (u64 i = 0; i < 200; i++) s += p[i] * (i + a);
    return s ^ b;
}

u64 fn_fib(u64 a, u64 b, u8 *p) {
    a &= 15;
    return a < 2 ? a : fn_fib(a - 1, b, p) + fn_fib(a - 2, b, p);
}

u64 fn_strlen_copy(u64 a, u64 b, u8 *p) {
    u8 *dst = p + 0x1000;
    u64 n = 0;
    while (p[n] != 0x55 && n < 300) { dst[n] = p[n] ^ (u8)a; n++; }
    return n;
}

u64 fn_sort(u64 a, u64 b, u8 *p) {
    u32 *v = (u32 *)(p + 0x2000);
    for (int i = 0; i < 24; i++) v[i] = (u32)(a * (i + 1) * 2654435761u) >> (i & 7);
    for (int i = 0; i < 24; i++)
        for (int j = 0; j + 1 < 24 - i; j++)
            if (v[j] > v[j + 1]) { u32 t = v[j]; v[j] = v[j + 1]; v[j + 1] = t; }
    return v[0] + v[23];
}

u64 fn_divmod(u64 a, u64 b, u8 *p) {
    i64 x = (i64)a, y = (i64)(b | 1);
    u64 r = (u64)(x / y) + (u64)(x % y) * 3;
    r += a / (b | 1) + a % 1000 + (u32)a / 7u + (u64)((int)a / 13) + (u64)((i64)a % 10);
    return r;
}

u64 fn_bits(u64 a, u64 b, u8 *p) {
    u64 r = 0;
    for (int i = 0; i < 64; i++) {
        r = (r << 1) | ((a >> i) & 1);
        if (b & (1ul << (i & 31))) r ^= a >> (i & 15);
        else r += (u64)(short)b * (u64)(signed char)(a >> i);
    }
    return r;
}

u64 fn_switch(u64 a, u64 b, u8 *p) {
    u64 r = b;
    for (int i = 0; i < 40; i++) {
        switch ((a + i) % 7) {
            case 0: r += i; break;
            case 1: r ^= a; break;
            case 2: r = (r << 3) | (r >> 61); break;
            case 3: r -= p[i]; break;
            case 4: r *= 31; break;
            case 5: r = ~r; break;
            default: r >>= 1; break;
        }
    }
    return r;
}

u64 fn_struct(u64 a, u64 b, u8 *p) {
    struct rec { u8 tag; short s; u32 w; u64 q; i64 n; } *r = (struct rec *)(p + 0x3000);
    for (int i = 0; i < 10; i++) {
        r[i].tag = (u8)(a + i);
        r[i].s = (short)(b - i * 1000);
        r[i].w = (u32)(a >> i);
        r[i].q = a * b + i;
        r[i].n = -(i64)(a >> (i + 1));
    }
    u64 sum = 0;
    for (int i = 9; i >= 0; i--) sum += r[i].tag + r[i].s + r[i].w + r[i].q + (u64)r[i].n;
    return sum;
}

u64 fn_mul128(u64 a, u64 b, u8 *p) {
    unsigned __int128 x = (unsigned __int128)a * b;
    __int128 y = (__int128)(i64)a * (i64)b;
    return (u64)(x >> 64) ^ (u64)(y >> 64) ^ (u64)x;
}
