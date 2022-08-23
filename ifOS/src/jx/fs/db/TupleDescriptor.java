package jx.fs.db;

/**
 * Title: SA
 * Description:
 * Copyright: Copyright (c) 2001
 * Company:
 * @author Ivanich
 * @version 1.0
 */


/*fixit*/
import jx.fs.db.types.DbConverter;

public interface TupleDescriptor extends jx.zero.Portal  {
    public static final int ERR_WRONG_OFFSET = 0;
    public static final int ERR_TD_NOT_INITIALIZED = 1;
    public static final int ERR_INVALID_IID = 2;
    public static final int ERR_WRONG_OFFSET_DIM = 3;

    public int  getCount();
    public int  getSegNum();
    public int  getRelId();;
    public int getAttrSize(int iPos);
    public int getTupleSize();
    public int getType(int iPos);
    public String getName(int iPos);
    public int getFieldOffset(int iPos);
    public DbConverter getConverter(int iPos);
}
