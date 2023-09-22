
package jCPU.JavaVM.vm;

/**
 *
 * @author spy
 */
public class VmStackFrame {
    static final int MAX = 1000;
    /* Stack Frame */
    public static int STACK_ENTRY_NONE   = 0;
    public static int STACK_ENTRY_INT    = 1;
    public static int STACK_ENTRY_REF    = 2;
    public static int STACK_ENTRY_LONG   = 3;
    public static int STACK_ENTRY_DOUBLE = 4;
    public static int STACK_ENTRY_FLOAT  = 5;
    
        private int top;
        
        VmStackEntry a[] = new VmStackEntry[MAX]; // Maximum size of Stack 

        boolean isEmpty()
        { 
            return (top < 0);
        } 
        
        VmStackFrame() 
        { 
            top = -1;
        }

        public boolean push(VmStackEntry x)
        { 
            if (top >= (MAX - 1)) { 
                System.out.println("Stack Overflow"); 
                return false;
            } else {
                a[++top] = x; 
                System.out.println(x + " pushed into stack"); 
                return true;
            }
        }

        public VmStackEntry pop()
        {
            if (top < 0) {
                System.out.println("Stack Underflow");
                return null;
            } else {
                VmStackEntry x = a[top--];
                return x;
            }
        }

        VmStackEntry peek()
        {
            if (top < 0) {
                System.out.println("Stack Underflow");
                return null;
            } else {
                VmStackEntry x = a[top];
                return x;
            }
        }

        public void push(int i) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        public void push(double i) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
        
        public double get_double_parameter(VmCP cp)
        {
            double value;
            if (is_ref_entry()) {
                int index = popInt();
                value = cp.getDouble(index);
                //System.out.print("index %d\n", index);
                //System.out.print("get value from constant pool = %f\n", value);
            } else {
                value = popDouble();
                //System.out.print("get value from stack = %f\n", value);
            }
            return value;
        }

    private boolean is_ref_entry() {
        return a[top].type == STACK_ENTRY_REF;
    }

    private int popInt() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private double popDouble() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
