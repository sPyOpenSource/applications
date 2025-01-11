
package jx.zero.classfile;

/**
 *
 * @author xuyi
 */
public interface ExceptionHandlerData {

    public int getCatchTypeCPIndex();

    public int getStartBCIndex();

    public int getEndBCIndex();

    public int getHandlerBCIndex();
    
}
