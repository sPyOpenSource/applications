/*
 * RelationalOperator.java
 *
 * Created on 30. Juli 2001, 10:08
 */

package jx.fs.db;

/**
 *
 * @author  ivanich
 * @version
 */
public interface RelationalOperator {

    public static final int ERR_EMPTY_SEARCH = 0;

    public void close();

    public boolean moveToNext();

    public boolean moveToFirst();

    public boolean moveToPrev();

    public TupleReader getCurrent();

    public TupleDescriptor getTupleDesc();

    public boolean isEmpty();

}
