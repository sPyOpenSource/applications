
package java.lang.invoke;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Member;

/**
 *
 * @author spy
 */
public interface MethodHandleInfo {

    static int REF_newInvokeSpecial = 0;
    static int REF_invokeVirtual = 0;
    static int REF_invokeSpecial = 0;
    static int REF_invokeInterface = 0;

    int getReferenceKind();

    Class<?> getDeclaringClass();

    MethodType getMethodType();

    public String getName();

    public <T extends Member> T reflectAs(Class<T> expected, Lookup lookup);
    
}
