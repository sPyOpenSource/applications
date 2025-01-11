package jx.fs.db;

public interface Key {

    public static final int ERR_INVALID_POS = 0;

    public int getFieldCount();
    public void getField( Object cDest, int iPos );
    public void setField( Object cSrc, int iPos );
    public byte[][] getBytes();
    public void setBytes( byte[] baSrc, int iOffset );

    public void setField( int iSrc, int iPos );
    public int getField( int iPos );

    public void setFieldToMin(int iPos);
    public void setFieldToMax(int iPos);

}
