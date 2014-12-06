/**
 * Created by Adam on 11/27/2014.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Queens {

    public static void main(String[] args){
//        Test("C:/data.csv");
//        Test2();
        for (int i = 0; i < 100; i++){System.out.println(Solve(8,true,false));}
    }

    private static void Test2(){
        int[] q = {5, 7, 1, 3, 7, 7, 4, 0};
        System.out.println(ScoreMem(q));
    }

    //the number of points lost based on the number of collisions
    static int power = 2;
    //the probability that of each member of the population mutating
    static double mutate = 0.01;
    //the ratio of the size of the population to the number of genes
    static int popMult = 5;
    //the number of times that the best member of a generation clones directly into the next generation
    static int clone = 0;

    private static int Solve(int n, boolean verboseRun, boolean verboseResult){
        int i = 0;
        int[][] p = CreatePopulation(n);
        int maxScore = 4* MaxSingleDimensionScore(n);
        int[] bestMem = new int[2];
        double bestRatio = 0;
        int[] worstMem = new int[2];
        double worstRatio = 0;
        long q = 1000000;
        for (i = 1; i <= q; i++){

            //advance the generation
            p = FullStep(p);

            //calculate population metrics
            bestMem = BestScore(PopScores(p));
            bestRatio = (double)(bestMem[0])/maxScore;
            worstMem = BestScore(PopScores(p));
            worstRatio = (double)(worstMem[0])/maxScore;

            //report current status to the user
            if (verboseRun){
                System.out.printf("%,d complete; ", i);
                System.out.printf("%,d max iterations; ", q);
                System.out.printf("%,d optimal score; ", maxScore);
                System.out.printf("%,d strongest member; ", bestMem[0]);
                System.out.printf("%,d weakest member; ", worstMem[0]);
                System.out.printf("%.4f%% strong; ", bestRatio * 100);
                System.out.printf("%.4f%% weak; ", worstRatio * 100);
                System.out.printf("%.4f%% spread; ", (bestRatio - worstRatio) * 100);
                System.out.printf("\r");
            }

            //stop iterating if an optimal solution has been found
            if (bestMem[0] == maxScore){
                break;
            }
        }
        if (verboseResult){
            PrintPop(p);
            System.out.printf("\n");
            System.out.println(Arrays.toString(bestMem));
            System.out.println(Arrays.toString(p[bestMem[1]]));
            System.out.println(bestMem[0]);
            System.out.println(maxScore);
        }

        return (i);
    }

    private static int Solve (int n){
        return Solve(n,true,true);
    }

    private static int Solve(int n, int powerSet, double mutateSet, int popMultSet, int cloneSet, boolean verboseRun, boolean verboseResult){
        power = powerSet;
        mutate = mutateSet;
        popMult = popMultSet;
        clone = cloneSet;
        return Solve(n, verboseRun, verboseResult);
    }

    private static void Test(String path){
        boolean counting = true;
        String s = "n\tps\tms\tpMs\tcs\titer\n";
        int q = 0;
        int m = 0;
        do {

            if (m > 0){
                counting = false;
                q = 0;
            }

            for (int n = 10; n >= 4; n -= 2){
                for (int ps = 1; ps <= 4; ps *= 2){
                    for (double ms = .001; ms <= 1; ms *= 10){
                        for (int pMs = 1; pMs <= 8; pMs *= 2){
                            /*anecdotally hig clone rates with low mutation rates are horrendously slow.
                            * we'll limit the number of these we test. I suspect regression analysis afterwards
                            * is going to confirm that clone rates near the population size are plainly terrible*/
                            for (int cs = 0; cs <= n * pMs * .5; cs += pMs){
                                for (int i = 1; i <= 1; i++){
                                    q++;
                                    if (!counting){
                                        int gens = Solve(n, ps, ms, pMs, cs, true, false);
                                        s += (n + "\t" + ps + "\t" + ms + "\t" + pMs + "\t" + cs + "\t");
                                        s += gens;
                                        s += "\n";
                                        System.out.printf("%,d of %,d; ", q, m);
                                        System.out.printf("n = %,d; ", n);
                                        System.out.printf("power = %,d; ", ps);
                                        System.out.printf("mutate = %.1f%%; ", 100 * ms);
                                        System.out.printf("pop mult = %,d; ", pMs);
                                        System.out.printf("clone = %,d; ", cs);
                                        System.out.printf("i = %,d; ", i);
                                        System.out.printf("%,d generations; ", gens);
                                        System.out.printf("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            m = q;

        } while (counting);
        WriteFile(s, path);
        System.out.println(q);
    }

    public static void WriteFile(String content, String path){
        BufferedWriter writer = null;
        try {
            //create a temporary file
            File logFile = new File(content);

            // This will output the full path where the file will be written to...
            System.out.println(logFile.getPath());

            writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (IOException ignored){
            }
        }
    }

    private static int[][] FullStep(int[][] Pop){
        for (int i = 0; i < popMult; i++){
            Pop = BestEatsWorst(Pop);
            Pop = Mutation(Pop);
        }
        Pop = NewGeneration(Pop);
        return Pop;
    }

    private static int[][] Mutation(int[][] Pop){
        int g = Pop[0].length;
        int q = Pop.length;
        /*separating the used value from the parameter up front
        * this allows us to vary the probability of mutation based
        * on the current status of the population*/
        double mProb = mutate;

        /*note some information about the current state of the
        * population.*/
        int maxScore = 4* MaxSingleDimensionScore(g);
        int[] scores = PopScores(Pop);
        int[] bestMem = BestScore(scores);
        int[] worstMem = BestScore(scores);

        /*manipulate mProb according to state of population
        * as the weakest member of the population gets closer to
        * the optimal value we will increase the probability of mutation
        * this should keep the evolution from stagnating due the
        * strongest members over producing*/
        double worstRatio = (double)(worstMem[0])/maxScore;
        mProb /= (1-worstRatio);

        for (int i = 0; i < q; i++){

            /*start off by assuming that all members can be
            * mutated on any given generation. then apply logic
            * to make a member exempt in some situations. it
            * feels like the best member of the population
            * should be exempt from mutation, especially under
            * a variable mutation probability setup.*/
            boolean exempt = false;
            //best member is always exempt
            if (i == bestMem[1]){
                exempt = true;
            }
            //members tied for best are exempt some of the time
            if (scores[i] == bestMem[0]){
                if (Math.random() < .5){
                    exempt = true;
                }
            }

            if (Math.random() < mProb & !exempt){
                int gene = (int) (g * Math.random());
                int newValue = (int) (g * Math.random());
                Pop[i][gene] = newValue;
            }
        }

        return Pop;
    }

    private static int[][] NewGeneration(int[][] Pop){
        int q = Pop.length;
        int[][] newPop = new int[q][q];
        for (int i = 0; i < q; i++){
            if (i < clone){
                newPop[i] = Pop[BestScore(PopScores(Pop))[1]];
            } else {
                int[] Parent0 = Pop[(int) (q * Math.random())];
                int[] Parent1 = Pop[(int) (q * Math.random())];
                newPop[i] = Breed(Parent0, Parent1);
            }
        }
        return newPop;
    }

    private static int[] Breed(int[] Mem0, int[] Mem1){
        int q = Mem0.length;
        int[] score = new int[2];
        score[0] = ScoreMem(Mem0);
        score[1] = ScoreMem(Mem1);
        //set p as probability of gene from parent 0
        double p = (score[0]/(score[0]+score[1]));
        int[] baby = new int[q];
        for (int j = 0; j < q; j++){
            if (Math.random() < p){
                baby[j] = Mem0[j];
            } else {
                baby[j] = Mem1[j];
            }
        }
        return baby;
    }

    private static int[][] BestEatsWorst(int[][] Pop){
        int[] scores = PopScores(Pop);
        int[] max = BestScore(scores);
        int[] min = WorstScore(scores);
        Pop[min[1]] = Pop[max[1]];
        return Pop;
    }

    private static int[] PopScores(int[][] Pop){
        int q = Pop.length;
        int[] scores = new int[q];
        for (int j = 0; j < q; j++){
            scores[j] = ScoreMem(Pop[j]);
        }
        return scores;
    }

    private static int[] BestScore(int[] scores){
        int q = scores.length;
        /*Use an arrays to hold the maximum score
        * from the population. [0] is the score of member. [1] is index*/
        int[] max = {-1,-1};
        int maxCount = 0;
        for (int i = 0; i < q; i++){
            if (scores[i] > max[0]){
                max[0] = scores[i];
                maxCount = 1;
                max[1] = i;
            } else if (scores[i] == max[0]){
                maxCount++;
                if (maxCount * Math.random() < 1){max[1] = i;}
            }
        }
        return max;
    }

    private static int[] WorstScore(int[] scores){
        int q = scores.length;
        /*Use an arrays to hold the minimum score
        * from the population. [0] is the score of member. [1] is index*/
        int[] min = {4* MaxSingleDimensionScore(q)+1,-1};
        int minCount = 0;
        for (int i = 0; i < q; i++){
            if (scores[i] < min[0]){
                min[0] = scores[i];
                minCount = 1;
                min[1] = i;
            } else if (scores[i] == min[0]){
                minCount++;
                if (minCount * Math.random() < 1){min[1] = i;}
            }
        }
        return min;
    }

    private static int ScoreMem(int[] Mem){
        int score = 0;
        score += ScoreDiagU(Mem);
        score += ScoreDiagD(Mem);
        score += ScoreRow(Mem);
        score += ScoreCol(Mem);
        return score;
    }

    private static int ScoreCol(int[] Mem){
    /*Initial version of column score is very direct because
    member is expressed such that there is never more than one
    queen in any given row*/
        int temp = 0;
        int q = Mem.length;
        for (int i = 0; i < q; i++){
            temp += 0;
        }
        temp = MaxSingleDimensionScore(q) - temp;
        return temp;
    }

    private static int ScoreRow(int[] Mem){
        int temp = 0;
        int q = Mem.length;
        for (int i = 0; i < q; i++){
            int row = -1;
            for (int aMem : Mem){
                if (aMem == i){
                    row++;
                }
            }
            if (row < 0){row = 0;}
            temp += Math.pow(row,power);
        }
        temp = MaxSingleDimensionScore(q) - temp;
        return temp;
    }

    private static int ScoreDiagU(int[] Mem){
        int temp = 0;
        int q = Mem.length;
        for (int i = 0; i < q*2-1; i++){
            int diag = -1;
            for (int j = 0; j < q; j++){
                if (Mem[j] == i-j){
                    diag++;
                }
            }
            if (diag < 0){diag = 0;}
            temp += Math.pow(diag,power);
        }
        temp = MaxSingleDimensionScore(q) - temp;
        return temp;
    }

    private static int ScoreDiagD(int[] Mem){
        int temp = 0;
        int q = Mem.length;
        for (int i = 1-q; i < q; i++){
            int diag = -1;
            for (int j = 0; j < q; j++){
                if (Mem[j] == i+j){
                    diag++;
                }
            }
            if (diag < 0){diag = 0;}
            temp += Math.pow(diag,power);
        }
        temp = MaxSingleDimensionScore(q) - temp;
        return temp;
    }

    private static void PrintPop(int[][] Pop){
        System.out.println();
        for(int[] Mem : Pop){
            System.out.print(Arrays.toString(Mem) + " - ");
            System.out.println(ScoreMem(Mem));
        }
    }

    private static int[][] CreatePopulation(int n){
        int q = n*popMult;
        int[][] Pop = new int[q][n];
        for (int i = 0; i < n; i++){
            for (int j = 0; j < n*popMult; j++){
//                q++;
                Pop[j][i] = (int)(Math.random() * n);
            }
        }
        return Pop;
    }

    private static int MaxSingleDimensionScore(int n){
        return (int)(Math.pow(n,power));
    }

}
