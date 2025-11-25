
package ecm.PrimeTest;

import java.awt.Color;
import java.awt.Dimension;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import utilities.Zeef;

/**
 *
 * @author X. Wang
 */
public class LucasLehmer extends Thread {
    static int []smallPrimes = new int[8];/*{
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 
            73, 79, 83, 89, 97, 101, 127, 521, 607, 1279, 2203, 2281, 3217, 
            4253, 9689, 9941, 11213, 19937, 21701, 23209, 44497, 86243, 
            110503, 132049, 216091, 3021377, 6972593
        };*/
    private final int prime;
    static JComponent newContentPane = new JPanel();

    public static void main(String[] args) {
        //Create and set up the window.
        JFrame frame = new JFrame("2^p - 1 prime test");
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        System.out.println("Starting to look for primes");
        //Zeef zeef = new Zeef(86243);
        Zeef zeef = new Zeef(20624);
        System.arraycopy(zeef.getPrimes(), zeef.getSize() - smallPrimes.length, smallPrimes, 0, smallPrimes.length); 
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); 
        for (int j:smallPrimes){
            LucasLehmer ll = new LucasLehmer(j);
            //PrimeThread ll = new PrimeThread(j, 1);
            pool.execute(ll);
        }
    }
    
    public LucasLehmer(int prime){
        this.prime = prime;
    }

    @Override
    public void run(){
        long now = System.nanoTime();
        if (isPrime()) {
            System.out.printf("2^%d-1 is prime\n", prime);
        } else {
            System.out.printf("2^%d-1 is not prime\n", prime);
        }
        System.out.println((System.nanoTime() - now) / 1000 / 1000);
    }
        
    // p here is the same p as in 2^p-1
    public boolean isPrime(){
        JProgressBar progressBar = new JProgressBar(0, prime - 2);
        newContentPane.add(progressBar);
        progressBar.setStringPainted(true);
        progressBar.setString("" + prime);
        progressBar.setValue(0);
        BigInteger s = BigInteger.valueOf(4L);
        BigInteger m = BigInteger.ONE.shiftLeft(prime).subtract(BigInteger.ONE); // = (1 << p) - 1
        for (int i = 1; i <= prime - 2; i++) {
            // This is the slower but straightforward way
            // s.Mod(s.Sub(s.Mul(s, s), two), m)
            // Or, use this faster way
            // s here is the same as k in this part of the wikipedia page:
            // https://en.wikipedia.org/wiki/Lucas%E2%80%93Lehmer_primality_test#Time_complexity
            s = s.pow(2).subtract(BigInteger.TWO);
            //System.out.println(s);
            while (s.compareTo(m) == 1) {
                // And is big's logical and, Rsh is right shift
                BigInteger x = s.shiftRight(prime);
                //System.out.println(x);
                BigInteger y = s.and(m);
                //System.out.println(y);
                s = y.add(x);
                //System.out.println(s);
            }
            progressBar.setValue(i);
            if (s.compareTo(m) == 0) {
                progressBar.setForeground(Color.GREEN);
                //progressBar.setString("p = " + p + " is prime");
                return true;
            }

            //s = s.mod(m);
        }
        boolean ret = s.compareTo(BigInteger.ZERO) == 0;
        if(ret){
            progressBar.setForeground(Color.GREEN);
            //progressBar.setString("p = " + p + " is prime");
        } else {
            progressBar.setForeground(Color.LIGHT_GRAY);
        }
        return ret;
    }
}

class PrimeThread extends Thread {
    private final BigInteger m;
    private final int prime;
    private final int certainty;
    private final AprtCleFinal aprt = new AprtCleFinal();
    
    PrimeThread(int prime, int certainty) {
        this.m = BigInteger.ONE.shiftLeft(prime).subtract(BigInteger.ONE);
        this.prime = prime;
        this.certainty = certainty;
    }

    @Override
    public void run() {
        /*GCDBigInteger N = new GCDBigInteger(m.toString(), false);
        if(N.isProbablePrimeRobin()){
            System.out.printf("2^%d-1 is probable prime\n", prime);
        } else {
            System.out.printf("2^%d-1 is not robin prime\n", prime);
        }*/
        /*if(aprt.isProbablePrime(N)){
            System.out.printf("2^%d-1 is probable prime\n", prime);
        } else {
            System.out.printf("2^%d-1 is not apert cle prime\n", prime);
        }*/
        if(m.isProbablePrime(certainty)){
            System.out.printf("2^%d-1 is probable prime\n", prime);
        } else {
            System.out.printf("2^%d-1 is not prime\n", prime);
        }
    }
}