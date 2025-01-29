/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models.vm;

/**
 *
 * @author spy
 */
public class VmStackFrame {
    /* Stack Frame */
    public static int STACK_ENTRY_NONE   = 0;
    public static int STACK_ENTRY_INT    = 1;
    public static int STACK_ENTRY_REF    = 2;
    public static int STACK_ENTRY_LONG   = 3;
    public static int STACK_ENTRY_DOUBLE = 4;
    public static int STACK_ENTRY_FLOAT  = 5;
    
    static final int MAX = 1000; 
    int top; 
    Object a[] = new Object[MAX]; // Maximum size of Stack 

    boolean isEmpty() 
    { 
        return (top < 0); 
    }
    
    VmStackFrame() 
    { 
        top = -1; 
    } 

    public boolean push(Object x) 
    { 
        if (top >= (MAX - 1)) { 
            System.out.println("Stack Overflow"); 
            return false; 
        } 
        else { 
            a[++top] = x; 
            System.out.println(x + " pushed into stack"); 
            return true; 
        } 
    } 

    public Object pop() 
    { 
        if (top < 0) { 
            System.out.println("Stack Underflow"); 
            return 0; 
        } 
        else { 
            Object x = a[top--]; 
            return x; 
        } 
    } 

    Object peek() 
    { 
        if (top < 0) { 
            System.out.println("Stack Underflow"); 
            return 0; 
        } 
        else { 
            Object x = a[top]; 
            return x; 
        } 
    }
    
    public int popInt() {
        return (Integer)pop();
    }

    public double popDouble() {
        return (Double)pop();
    }

    public void pushInt(int i) {
        push(i);
    }
    
    public void pushRef(int i) {
        push(i);
    }
    
    public void pushDouble(double i) {
        push(i);
    }
    
    public VmStackEntry popEntry(){
        return (VmStackEntry)pop();
    }
    
    public boolean isRef() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
