package src.main.java.src;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.learning.config.Adam;


public class Game{
    
    public static ArrayList<Tile> set = new ArrayList<>();

    public static MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
    .updater(new Adam(0.001))
    .list()
    .layer(0, new DenseLayer.Builder().nIn(490).nOut(256).activation(Activation.RELU).build())
    .layer(1, new DenseLayer.Builder().nIn(256).nOut(128).activation(Activation.RELU).build())
    .layer(2, new OutputLayer.Builder().nIn(128).nOut(57).activation(Activation.IDENTITY)
        .lossFunction(LossFunctions.LossFunction.MSE).build())
    .build();

    public static MultiLayerNetwork qNetwork = new MultiLayerNetwork(conf);
    qNetwork.init();
    public static MultiLayerNetwork targetNetwork = qNetwork.clone();

    public static ArrayList<Tile> allTiles = new ArrayList<>();
    static {
        for (int i = 0; i < 7; i++) {
            for (int j = i; j < 7; j++) {
                allTiles.add(new Tile(i, j));
            }
        }
    }

    public static Player[] players = new Player[4];

    public static ArrayList<Tile> board = new ArrayList<>();

    public static int leftEnd = -1, rightEnd = -1; 

    public static final String RESET = "\033[0m";  // Reset to default
    public static final String BLACK = "\033[0;30m";   
    public static final String RED = "\033[0;31m";     
    public static final String GREEN = "\033[0;32m";   
    public static final String YELLOW = "\033[0;33m";  
    public static final String BLUE = "\033[0;34m";    
    public static final String PURPLE = "\033[0;35m";  
    public static final String CYAN = "\033[0;36m";    
    public static final String WHITE = "\033[0;37m";  

    public static Scanner sc = new Scanner(System.in);

    /*public static final String RESET = "";  // Reset to default
    public static final String BLACK = "";   
    public static final String RED = "";     
    public static final String GREEN = "";   
    public static final String YELLOW = "";  
    public static final String BLUE = "";    
    public static final String PURPLE = "";  
    public static final String CYAN = "";    
    public static final String WHITE = "";  */

    public static void main(String[] args) {

        for(int i = 0; i < 7; i++){
            for(int j = 0; j <= i; j++){
                set.add(new Tile(j, i));
            }
        }

        System.out.println(set.toString());
        Collections.shuffle(set);

        Random r = new Random();

        leftEnd = -1;
        rightEnd = -1; 

        int consecutivePasses = 0;

        boolean flag = true;
        int[] wins = new int[4];

        int c = 0;

        for(int i = 0; i < 4; i++){

            players[i] = new Player();

            for(int j = 0; j < 7; j++){
                players[i].add(set.get(c));
                c++;
            }
            players[i].sort();
            System.out.println("Player [" + i + "]: " + players[i].getHand().toString());
        }

        int turn = r.nextInt(4);
        //System.out.println("Starting: " + turn);
        int offset = turn;
        flag = true;
        while(flag){
            for(int i = turn; i < 4; i++){
                System.out.println("\n\n---------------------------------------------------------------");

                System.out.println("\nBoard State: " + YELLOW + board.toString() + RESET);
                System.out.println("\n---------------------------------------------------------------");

                System.out.println("\n\nPlayer " + BLUE +  i + RESET + " tiles: " + RED + players[i].toString() + RESET + "\n");

                if(leftEnd != -1 && rightEnd != -1){
                    System.out.println("You must play either a " + GREEN + leftEnd + RESET + " or " + GREEN + rightEnd + RESET + "\n");
                }
                ArrayList<Tile> temp = players[i].getPlayableTiles(leftEnd, rightEnd);

                if (temp.size() == 0) {
                    System.out.println("Player " + i + " passes.");
                    consecutivePasses++;
                } 
                else {
                    System.out.println("Enter the index of the tile you want to play. Player " + BLUE + i + RESET + " can only play: " + PURPLE + temp.toString() + RESET);
                    //System.out.println("Your best theoretical move here is: " + bestTile.toString());
                    System.out.println("Player action: ");
                    
                    int play = 0;

                    try{
                        play = sc.nextInt()-1;
                    }
                    catch(Exception e){
                        sc.nextLine();
                    }

                    while(play < 0 || play > temp.size()){
                        System.out.println("Please enter a valid number between 1 and " + temp.size() + ".");
                        play = sc.nextInt()-1;
                    }

                    int side = 0;
                    if(temp.get(play).getA() == leftEnd && temp.get(play).getB() == rightEnd || 
                    temp.get(play).getA() == rightEnd && temp.get(play).getB() == leftEnd 
                    && leftEnd != rightEnd){
                        
                        System.out.println("Enter " + PURPLE + 1 + RESET + " if you want to place your tile on the " + CYAN + "left" + RESET + " or " + PURPLE + 2 + RESET + " if you want your tile on the " + CYAN + "right" + RESET + ": ");
                        side = sc.nextInt();
                    }

                    addToBoard(temp.get(play), side);


                    players[i].remove(temp.get(play).getA(), temp.get(play).getB());
                    consecutivePasses = 0;

                    if(players[i].size() == 0){
                        System.out.println("Player " + i + " won!!!!");
                        flag = false;

                        int winner = i;

                        int pos = 0;

                        for(int j = 0; j < 4; j++){
                            if(offset == winner){
                                j = 4;
                            }
                            else{
                                offset++;
                                pos++;
                                if(offset > 3){
                                    offset = 0;
                                }
                            }
                        }
                        
                        wins[pos]++;
                        System.out.println("\n\nEND GAME DATA: ");
                        for(int j = 0; j < 4; j++){
                            System.out.println(players[j].sum());
                        }
                        break;
                    }
                }

                if(consecutivePasses == 4){
                    System.out.println("The game ends in a stalemate, nobody wins!!!");
                    
                    System.out.println("\n\nEND GAME DATA: ");
                    flag = false;
                    int winnerRelative = offset;
                    for(int j = 0; j < 4; j++){
                        System.out.println(players[j].sum());
                        if(players[j].sum() < players[winnerRelative].sum()){
                            winnerRelative = j;
                        }
                    }
                    int winner = winnerRelative;
                    int pos = 0;

                    for(int j = 0; j < 4; j++){
                        if(offset == winner){
                            j = 4;
                        }
                        else{
                            offset++;
                            pos++;
                            if(offset > 3){
                                offset = 0;
                            }
                        }
                    }
                    wins[pos]++;
                    break;
                }
            }
            turn = 0;
        
        }
        System.out.println("DONE");
        sc.close();
        

    }

    public static void trainAgents(int episodes) {
        ArrayDeque<double[]> replayBuffer = new ArrayDeque<>(10000); // Simplified buffer
        Random rand = new Random();
        double epsilon = 1.0, epsilonMin = 0.01, epsilonDecay = 0.995;
        int batchSize = 32, targetUpdateFreq = 100;
        int step = 0;

        for (int ep = 0; ep < episodes; ep++) {
            // Reset game
            set.clear();
            for (int i = 0; i < 7; i++) for (int j = 0; j <= i; j++) set.add(new Tile(j, i));
            Collections.shuffle(set);
            for (int i = 0; i < 4; i++) players[i] = new Player();
            int c = 0;
            for (int i = 0; i < 4; i++) for (int j = 0; j < 7; j++) players[i].add(set.get(c++));
            board.clear();
            leftEnd = -1;
            rightEnd = -1;
            int consecutivePasses = 0;
            int turn = rand.nextInt(4);
            ArrayList<double[]> pending = new ArrayList<>(); // (s, a) per player

            while (true) {
                Player p = players[turn];
                double[] s = p.getState(board, leftEnd, rightEnd);
                boolean[] valid = p.getValidActions(leftEnd, rightEnd);
                INDArray qValues = qNetwork.output(Nd4j.create(s));
                int a;
                if (rand.nextDouble() < epsilon) {
                    List<Integer> validIdx = new ArrayList<>();
                    for (int i = 0; i < 57; i++) if (valid[i]) validIdx.add(i);
                    a = validIdx.get(rand.nextInt(validIdx.size()));
                } else {
                    for (int i = 0; i < 57; i++) if (!valid[i]) qValues.putScalar(i, Double.NEGATIVE_INFINITY);
                    a = qValues.argMax().getInt(0);
                }

                // Execute action
                int side = a < 28 ? 1 : (a == 56 ? 0 : 2);
                Tile tile = a == 56 ? null : Game.allTiles.get(a % 28);
                if (tile != null) {
                    addToBoard(tile, side);
                    p.remove(tile.getA(), tile.getB());
                    consecutivePasses = 0;
                } else {
                    consecutivePasses++;
                }

                // Store pending experience
                pending.add(new double[]{s, a});

                // Check game end
                int winner = -1;
                if (p.size() == 0) winner = turn;
                else if (consecutivePasses == 4) {
                    winner = 0;
                    for (int i = 1; i < 4; i++) if (players[i].sum() < players[winner].sum()) winner = i;
                }
                if (winner != -1) {
                    for (int i = 0; i < pending.size(); i++) {
                        double[] exp = pending.get(i);
                        double r = (i % 4 == winner) ? 1.0 : 0.0;
                        replayBuffer.add(new double[]{exp[0], exp[1], r, null, 1.0}); // Terminal
                    }
                    break;
                } else if (pending.size() >= 4) {
                    double[] prev = pending.remove(0);
                    double[] nextS = p.getState(board, leftEnd, rightEnd);
                    replayBuffer.add(new double[]{prev[0], prev[1], 0, nextS, 0});
                }

                turn = (turn + 1) % 4;
            }

            // Train
            if (replayBuffer.size() >= batchSize) {
                // Sample batch and update qNetwork (simplified here)
                // Use targetNetwork for stability, update periodically
                step++;
                if (step % targetUpdateFreq == 0) targetNetwork = qNetwork.clone();
            }
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        // Save model
        qNetwork.save(new File("dominoes_qnetwork.zip"));
    }

    public static void addToBoard(Tile t, int side){
        if(board.size() == 0){
            board.add(new Tile(t.getA(), t.getB()));
            leftEnd = t.getA();
            rightEnd = t.getB();
            return;
        }

        if(side > 0){
            if(side == 1){

                if(t.getA() == leftEnd){
                    board.add(0, new Tile(t.getB(), t.getA()));
                    leftEnd = t.getB();
                }
                else if(t.getB() == leftEnd){
                    board.add(0, new Tile(t.getA(), t.getB()));
                    leftEnd = t.getA();
                }
                else{
                    board.add(0, new Tile(t.getA(), t.getB()));
                    leftEnd = t.getA();
                }
                return;
            }
            else if(side == 2){
                //board.add(board.size(), new Tile(t.getA(), t.getB()));

                if(t.getA() == rightEnd){
                    board.add(board.size(), new Tile(t.getA(), t.getB()));
                    rightEnd = t.getB();
                }
                else if(t.getB() == rightEnd){
                    board.add(board.size(), new Tile(t.getB(), t.getA()));
                    rightEnd = t.getA();
                }
                else{
                    board.add(board.size(), new Tile(t.getA(), t.getB()));
                    rightEnd = t.getA();
                }
                return;
            }
        }

        if(t.getA() == leftEnd || t.getB() == leftEnd){

            if(t.getA() == leftEnd){
                board.add(0, new Tile(t.getB(), t.getA()));
                leftEnd = t.getB();
            }
            else if(t.getB() == leftEnd){
                board.add(0, new Tile(t.getA(), t.getB()));
                leftEnd = t.getA();
            }
            else{
                board.add(0, new Tile(t.getA(), t.getB()));
                leftEnd = t.getA();
            }
            return;
        }

        if(t.getA() == rightEnd || t.getB() == rightEnd){

            if(t.getA() == rightEnd){
                board.add(board.size(), new Tile(t.getA(), t.getB()));
                rightEnd = t.getB();
            }
            else if(t.getB() == rightEnd){
                board.add(board.size(), new Tile(t.getB(), t.getA()));
                rightEnd = t.getA();
            }
            else{
                board.add(board.size(), new Tile(t.getA(), t.getB()));
                rightEnd = t.getA();
            }
            return;
        }
    }

}