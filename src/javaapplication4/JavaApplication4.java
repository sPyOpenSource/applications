/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication4;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author spy
 * @param <T>
 */
public class JavaApplication4 <T>{
    public  static <T> T aMethod(T multiDimentionalArray) {
        return null;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int[][] a = new int[][] {{1,2,3,4},{2}};
        aMethod(a);
        LocalDateTime now = LocalDateTime.now();

        // Larger window of time
        LocalDateTime startTime1 = now.minusSeconds(15);
        LocalDateTime endTime1   = now;

        // Smaller window of time
        LocalDateTime startTime2 = now.plusSeconds(1);
        LocalDateTime endTime2   = now.plusSeconds(5);

        long i;
        i = getDifferenceInSeconds(startTime2, endTime2, startTime1, endTime1);
        System.out.println(i);
    }
    
    public static long getDifferenceInSeconds(LocalDateTime startTime2, LocalDateTime endTime2, LocalDateTime startTime1, LocalDateTime endTime1) {

    LocalDateTime minStartTime = (startTime2.isAfter(startTime1) ? startTime2 : startTime1);
    LocalDateTime minEndTime = (endTime2.isBefore(endTime1) ? endTime2 : endTime1);

    long seconds = minStartTime.until(minEndTime, ChronoUnit.SECONDS);

    return seconds > 0 ? seconds : 0;
}
}
