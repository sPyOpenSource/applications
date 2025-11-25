
package utilities;

import java.util.BitSet;

public class Zeef
{
  private final int p[];
  public static void main (String[] args)
  {
    int MAX = 82589933;
    if (MAX > 90000000){
      System.out.print(MAX + " is too large! Process has be broken!");
    } else {
      Zeef zeef = new Zeef(MAX);
    }
  }
  
  public int getSize(){
      return p.length;
  }
  
  public Zeef(int MAX){
    MAX ++;
    int SQRT_MAX = (int)Math.sqrt(MAX);
    BitSet v = new BitSet(MAX);

    for (int i = 3; i < MAX; i += 2){
      v.set(i, true);
    }
    
    v.set(2, true);
    int n = 1;
    
    for (int j = 3; j <= SQRT_MAX; j++){
      if (v.get(j)){
        n++;
        for (int i = j * j; i <= MAX && i > 0; i += j){
          v.set(i, false);
        }
      }
    }

    for (int i = SQRT_MAX + 1; i <= MAX && i > 0; i++){
      if (v.get(i)){
        n++;
      }
    }
    
    p = new int[n];
    int j = 0;
    
    for (int i = 0; i < MAX; i++){
      if (v.get(i)){
        p[j] = i;
        j++;
      }
    }
    
    System.out.println("Total: " + n + "\nBiggest Prime: " + p[n - 1]);
  }
  
  public int[] getPrimes(){
    return p;
  }
}
