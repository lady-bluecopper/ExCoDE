package eu.unitn.disi.db.excode.bucketization;

import java.util.Random;
import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class PRNG {
    
    private static long prime = 4294967291l;
    
    public static int randInt(int x) {
        long residue = (x * x) % prime;
        return (int)((x <= prime / 2) ? residue : prime - residue);
    }
    
    public static int[] randIntVec(int start, int length) {
        int[] rands = new int[length];
        IntStream.range(0, length).forEach(i -> rands[i] = randInt(start + i));
        shuffle(rands);
        return rands;
    }
    
    public static void shuffle(int[] array) {
        Random rand = new Random(prime);
        IntStream.range(0, array.length).map(i -> array.length - i - 1)
                .forEach(i -> {
                    int j = rand.nextInt(i + 1);
                    int temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                });
    }
}