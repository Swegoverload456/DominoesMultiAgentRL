package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class Game {
    
    public static ArrayList<Tile> set = new ArrayList<>();

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

        boolean training = false;

        boolean numCrunching = true;

        for(int i = 0; i < 7; i++){
            for(int j = 0; j <= i; j++){
                set.add(new Tile(j, i));
            }
        }

        Collections.shuffle(set);

        if(numCrunching){
            training = false;
        }

        int c = 0;

        for(int i = 0; i < 4; i++){

            players[i] = new Player();

            for(int j = 0; j < 7; j++){
                players[i].add(set.get(c));
                c++;
            }
            players[i].sort();
            if(!training){
                System.out.println("Player " + (i+1) + ": " + players[i].toString());
            }
        }

        Random r = new Random();

        leftEnd = -1;
        rightEnd = -1; 

        int consecutivePasses = 0;

        boolean flag = true;
        int iterations = 1000;
        int[] wins = new int[4];

        for(int k = 0; k < iterations; k++){ 
            int turn = r.nextInt(4);
            int offset = turn;
            flag = true;
            while(flag){
                for(int i = turn; i < 4; i++){

                    if(!training && !numCrunching){
                        System.out.println("\n\n---------------------------------------------------------------");

                        System.out.println("\nBoard State: " + YELLOW + board.toString() + RESET);
                        System.out.println("\n---------------------------------------------------------------");

                        System.out.println("\n\nPlayer " + BLUE +  i + RESET + " tiles: " + RED + players[i].toString() + RESET + "\n");

                        if(leftEnd != -1 && rightEnd != -1){
                            System.out.println("You must play either a " + GREEN + leftEnd + RESET + " or " + GREEN + rightEnd + RESET + "\n");
                        }
                    }
                    ArrayList<Tile> temp = players[i].getPlayableTiles(leftEnd, rightEnd);

                    if(training && numCrunching){
                        System.out.println("{\"board\": " + board.toString() + ", \"player_hand\": " + players[i].toString() + ", \"valid_tiles\": " + temp.toString() + ", \"current_player\": " + i + "}");
                    }

                    

                    if(temp.size() == 0){
                        if(!training && !numCrunching){
                            System.out.println("Player " + i + " passes.");
                        }
                        consecutivePasses++;
                    }
                    else{
                        Tile bestTile = null;
                        if(!training){
                            if(!numCrunching){
                                System.out.println("Enter the index of the tile you want to play. Player " + BLUE +  i + RESET + " can only play: " +  PURPLE + temp.toString() + RESET);
                            }
                            if(board.size() > 0){
                                bestTile = BacktrackingAlgorithm.bestMove(board, players[i].getHand(), leftEnd, rightEnd);
                                
                                if(!numCrunching){
                                    System.out.println("Your best theoretical move here is: " + bestTile.toString());
                                }
                            }
                            else{
                                if(numCrunching){
                                    bestTile = temp.get(r.nextInt(temp.size()));
                                    //System.out.println("Best tiel: "  + bestTile.toString());
                                }
                            }
                        }
                        else{
                            System.out.println("Player action: ");
                        }
                        
                        int play = 0;

                        if(!training && !numCrunching){

                            try{
                                play = sc.nextInt()-1;
                            }
                            catch(Exception e){
                                sc.nextLine();
                            }
                        }
                        else{
                            for(int j = 0; j < temp.size(); j++){
                                if(temp.get(j).compareTo(bestTile) == 0){
                                    play = j;
                                    if(play > temp.size()){
                                        play = temp.size()-1;
                                    }
                                    j = temp.size();
                                }
                            }
                        }

                        while(play < 0 || play > temp.size()){
                            System.out.println("Please enter a valid number between 1 and " + temp.size() + ".");
                            play = sc.nextInt()-1;
                        }

                        if(temp.get(play).getA() == leftEnd && temp.get(play).getB() == rightEnd || 
                        temp.get(play).getA() == rightEnd && temp.get(play).getB() == leftEnd 
                        && leftEnd != rightEnd){
                            int side = 0;
                            if(!training && !numCrunching){
                                System.out.println("Enter " + PURPLE + 1 + RESET + " if you want to place your tile on the " + CYAN + "left" + RESET + " or " + PURPLE + 2 + RESET + " if you want your tile on the " + CYAN + "right" + RESET + ": ");
                                side = sc.nextInt();
                            }
                            else{
                                //System.out.println("Left or Right: ");
                                side = bestTile.getSide();
                            }

                            
                            addToBoard(temp.get(play), side);

                        }
                        else{
                            addToBoard(temp.get(play), 0);
                        }

                        players[i].remove(temp.get(play).getA(), temp.get(play).getB());
                        consecutivePasses = 0;

                        if(players[i].size() == 0){
                            System.out.println("Player " + i + " won!!!!");
                            flag = false;
                            wins[offset]++;
                            System.out.println("\n\nEND GAME DATA: ");
                            for(int j = 0; j < 4; j++){
                                System.out.println(players[i].sum());
                            }
                            break;
                        }
                    }

                    if(consecutivePasses == 4){
                        System.out.println("The game ends in a stalemate, nobody wins!!!");
                        flag = false;
                        System.out.println("\n\nEND GAME DATA: ");
                        for(int j = 0; j < 4; j++){
                            System.out.println(players[i].sum());
                        }
                        break;
                    }
                }
                turn = 0;
            }
        }
        System.out.println("DONE");
        double[] wr = new double[4];
        for(int i = 0; i < 4; i++){
            wr[i] = wins[i]/iterations;
        }
        System.out.println(Arrays.toString(wr));
        sc.close();

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

class Player{
    private ArrayList<Tile> hand;

    public Player(){
        hand = new ArrayList<>();
    }

    public void add(Tile t){
        hand.add(t);
    }

    public void remove(int a, int b){
        for(int i = 0; i < hand.size(); i++){
            if(hand.get(i).getA() == a && hand.get(i).getB() == b){
                hand.remove(i);
            }
        }
    }

    public String toString(){
        return hand.toString();
    }

    public ArrayList<Tile> getHand(){
        return hand;
    }

    public int size(){
        return hand.size();
    }

    public ArrayList<Tile> getPlayableTiles(int endLeft, int endRight){

        if(endLeft == -1 && endRight == -1){
            return hand;
        }

        ArrayList<Tile> out = new ArrayList<>();

        for(int i = 0; i < size(); i++){
            if(hand.get(i).getA() == endLeft || hand.get(i).getA() == endRight ||
               hand.get(i).getB() == endLeft || hand.get(i).getB() == endRight){

                out.add(hand.get(i));

            }
        }

        return out;
    }

    public int sum(){
        int s = 0;

        for(int i = 0; i < size(); i++){
            s += hand.get(i).sum();
        }

        return s;
    }

    public void sort(){
        Collections.sort(hand);
    }

    

}
