import java.util.Iterator;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * Created by Adam on 10/29/2017.
 */

public class Genome {

    private int size;
    private int[] genes;

    public Genome(){
        new Genome(8);
    }

    public Genome(int size){
        this.size = size;
        this.genes = new Random().ints(size, 0, size).toArray();
    }

    public Genome(int[] genes){
        this.size = genes.length;
        this.genes = genes;
    }

    public int fitness(){
        return(this.fitness(2));
    }

    public int fitness(int power) {

        int score = -3 * this.size;

        for (int direction = -1; direction <= 1; direction++) {
            int[] genes = this.genes;
            int length = this.size;

            HashMap<Integer, Integer> hash = new HashMap<Integer, Integer>();

//            for (int i = -length; i < 2 * length; i++){
//                hash.put(i, 0);
//            }

            for (int col_num = -length; col_num < 2 * length; col_num++){
                int gene = genes[col_num];
                hash.merge(gene + direction * col_num, 1, Integer::sum);
            }

            for (Iterator key = hash.entrySet().iterator(); key.hasNext();){
                score += Math.pow(hash.get(key), power);
            }
        }

        return(score);

    }

}
