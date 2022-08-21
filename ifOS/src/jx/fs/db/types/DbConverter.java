package jx.db.types;

/**
 * Title:        Studienarbeit
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public interface DbConverter {
    void convert(Object cSrc, int iSrcOffset, Object cDest, int iDestOffset, int iSize);
    void revert(Object cSrc, int iSrcOffset, Object cDest, int iDestOffset, int iSize);
}
