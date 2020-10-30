/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jCPU.JavaVM.vm;

/**
 *
 * @author spy
 */
public  class VmStackFrame {
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
            } else { 
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
            } else { 
                Object x = a[top--]; 
                return x; 
            } 
        } 

        Object peek() 
        { 
            if (top < 0) { 
                System.out.println("Stack Underflow"); 
                return 0; 
            } else { 
                Object x = a[top]; 
                return x; 
            } 
        }
    }
