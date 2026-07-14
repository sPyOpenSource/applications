package flint;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import jx.classfile.FieldData;

public class JObject extends ListNode {

    static JObject fromRef(int objRef) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static JObject fromHandle(int handle) {
        return new JObject();
    }
    
    public JObject() {
        super();
        this.size = 0;
        this.type = null;
        this.data = new byte[0];
    }
    
    private int size;
    private byte prot = 0x02;
    private JClass type;
    private int monitorCount = 0;
    private int ownId = 0;
    private byte[] data;

    public JObject(int size, JClass type) {
        super();
        this.size = size;
        this.type = type;
        this.data = new byte[size];
    }

    public String getTypeName() {
        if (type == null)
            return "java/lang/Class";
        return type.getTypeName();
    }

    private static FieldData newFieldData(byte[] rawData) {
        return new FieldData(null, new DataInputStream(new ByteArrayInputStream(rawData)), null);
    }

    public boolean initFields(FExec ctx, ClassLoader loader) {
        return false;
    }

    private static void throwNoSuchFieldError(FExec ctx, String clsName, String name) {
        JClass excpCls = Flint.findClass(ctx, "java/lang/NoSuchFieldError");
        ctx.throwNew(excpCls, "Could not find the field %s.%s", clsName, name);
    }

    public FieldValue getField(FExec ctx, ConstField field) {
        FieldValue ret = new FieldValue();
        if (ret == null || ctx != null)
            throwNoSuchFieldError(ctx, field.className, field.nameAndType.name);
        return null;
    }

    public FieldValue getField(FExec ctx, String name) {
        FieldValue ret = new FieldValue();
        if (ret == null || ctx != null)
            throwNoSuchFieldError(ctx, getTypeName(), name);
        return null;
    }

    public FieldValue getFieldByIndex(int index) {
        return new FieldValue();
    }

    public void clearData() {
        Arrays.fill(data, (byte) 0);
    }

    public boolean isArray() {
        return (type != null && type.isArray());
    }

    public void clearProtected() {
        prot = 0;
    }

    public void setProtected() {
        prot = 1;
    }

    public byte getProtected() {
        return prot;
    }

    public int getSize() {
        return size;
    }

    public JClass getType() {
        return type;
    }

    public int getIntElement(int index) {
        int offset = index * Integer.BYTES;
        return (data[offset] & 0xFF) << 24
             | (data[offset + 1] & 0xFF) << 16
             | (data[offset + 2] & 0xFF) << 8
             | (data[offset + 3] & 0xFF);
    }

    public void setIntElement(int index, int value) {
        int offset = index * Integer.BYTES;
        data[offset]     = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    public long getLongElement(int index) {
        long lo = getIntElement(index * 2) & 0xFFFFFFFFL;
        long hi = getIntElement(index * 2 + 1) & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }

    public void setLongElement(int index, long value) {
        setIntElement(index * 2, (int) (value & 0xFFFFFFFFL));
        setIntElement(index * 2 + 1, (int) ((value >>> 32) & 0xFFFFFFFFL));
    }

    public byte getByteElement(int index) {
        return data[index];
    }

    public void setByteElement(int index, byte value) {
        data[index] = value;
    }

    public short getShortElement(int index) {
        int offset = index * Short.BYTES;
        return (short) ((data[offset] & 0xFF) << 8 | (data[offset + 1] & 0xFF));
    }

    public void setShortElement(int index, short value) {
        int offset = index * Short.BYTES;
        data[offset]     = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    public JObject getObjElement(int index) {
        int ref = getIntElement(index);
        return JObject.fromRef(ref);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (type == null || !type.isArray()) {
                // FieldData cleanup handled by GC
            }
        } finally {
            super.finalize();
        }
    }
    
}
