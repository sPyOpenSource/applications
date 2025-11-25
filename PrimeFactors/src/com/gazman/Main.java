
package com.gazman;

import com.gazman.factor.Logger;
import com.gazman.factor.QuadraticSieve;

import java.math.BigInteger;
import java.util.Random;

public class Main extends Logger {

    private static final Random random = new Random();

    public static void main(String[] args) {
        int length = 50;
        BigInteger a = BigInteger.probablePrime(length + 1, random);
        BigInteger b = BigInteger.probablePrime(length - 1, random);

        BigInteger input = a.multiply(b);
        System.out.println(input);
        //new QuadraticSieve(new BigInteger("15347")).start();
        //new QuadraticSieve(new BigInteger("378773223823")).start();
        //new QuadraticSieve(new BigInteger("10564552020050085806002346595749373582609207089")).start();
        new QuadraticSieve(new BigInteger("1192729626363545334371466409576203777052945105817717")).start();
    }
    
}
