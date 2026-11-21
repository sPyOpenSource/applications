/*
 * Smallest useful x64 UEFI application: prints through ConOut->OutputString,
 * does some arithmetic so the CPU has real work, then returns.
 * Built with clang + lld-link (see make-image.sh); no gnu-efi needed.
 */
typedef unsigned long long u64;
typedef unsigned short u16;
typedef u64 status;

struct text_out {
    status (*reset)(struct text_out *, int);
    status (*output_string)(struct text_out *, const u16 *);
};

struct system_table {
    char hdr[24];
    u16 *firmware_vendor;
    unsigned firmware_revision;
    void *console_in_handle;
    void *con_in;
    void *console_out_handle;
    struct text_out *con_out;
};

static void print(struct system_table *st, const u16 *s)
{
    st->con_out->output_string(st->con_out, s);
}

static void print_hex(struct system_table *st, u64 v)
{
    u16 buf[19];
    buf[0] = '0';
    buf[1] = 'x';
    for (int i = 0; i < 16; i++) {
        int d = (v >> (60 - 4 * i)) & 15;
        buf[2 + i] = d < 10 ? '0' + d : 'a' + d - 10;
    }
    buf[18] = 0;
    print(st, buf);
}

status efi_main(void *image, struct system_table *st)
{
    (void) image;
    print(st, u"Hello from BOOTX64.EFI\r\n");
    u64 a = 0, b = 1;
    for (int i = 0; i < 90; i++) {
        u64 t = a + b;
        a = b;
        b = t;
    }
    print(st, u"fib(90) = ");
    print_hex(st, a);
    print(st, u"\r\n");
    return 0;
}
