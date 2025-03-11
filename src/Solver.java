package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Solver {
    
    public static ArrayList<Tile> board = new ArrayList<>();
    public static Player player = new Player();

    public static int leftEnd = -1;
    public static int rightEnd = -1;

    public static final String RESET = "\033[0m";  // Reset to default
    public static final String BLACK = "\033[0;30m";   
    public static final String RED = "\033[0;31m";     
    public static final String GREEN = "\033[0;32m";   
    public static final String YELLOW = "\033[0;33m";  
    public static final String BLUE = "\033[0;34m";    
    public static final String PURPLE = "\033[0;35m";  
    public static final String CYAN = "\033[0;36m";    
    public static final String WHITE = "\033[0;37m"; 
    public static void main(String[] args) {
         
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your tiles 1 by 1 in the format [a:b]");
        for(int i = 0; i < 7; i++){
            System.out.println("Enter tile: ");
            String[] t = sc.nextLine().strip().split(":");
            player.add(new Tile(Integer.parseInt(t[0]), Integer.parseInt(t[1])));

        }

        while(board.size() < 28){
            
            sc.nextLine();
            System.out.println("Enter 'b' for board operation or 'p' for player operation:");
            String op = sc.nextLine().strip().toLowerCase();

            if(op.equals("b")){
                System.out.println("Enter tile: ");
                String[] t = sc.nextLine().strip().split(":");
                Tile temp = new Tile(Integer.parseInt(t[0]), Integer.parseInt(t[1]));
                
                int side = 0;

                if(leftEnd == rightEnd || 
                   temp.getA() == leftEnd && temp.getB() == rightEnd && leftEnd != rightEnd || 
                   temp.getA() == rightEnd && temp.getB() == leftEnd && leftEnd != rightEnd){

                    System.out.println("Enter 'l' if you want to place your tile on the left\nEnter 'r' if you want to place your tile on the right: ");

                    String s = sc.nextLine().strip().toLowerCase();
                    if(s.equals("l")){
                        side = 1;
                    }
                    else{
                        side = 2;
                    }
                    
                }

                addToBoard(temp, side);
            }

            else if(op.equals("p")){
                ArrayList<Tile> playableTiles = player.getPlayableTiles(leftEnd, rightEnd);

                System.out.println("Enter the index of the tile you want to play. You can only play: " + PURPLE + playableTiles.toString() + RESET);

                Tile bestTile = BacktrackingAlgorithm.bestMove(board, player.getHand(), leftEnd, rightEnd);

                String bestSide = "";

                if(bestTile.getSide() == 1){
                    bestSide = "\tSide: left";
                }
                else if(bestTile.getSide() == 2){
                    bestSide = "\tSide: right";
                }
                
                System.out.println("Your best theoretical move here is: " + bestTile.toString() + bestSide);

                System.out.println("Player action: ");

                int play = 0;

                try{
                    play = sc.nextInt()-1;
                }
                catch(Exception e){
                    sc.nextLine();
                }

                while(play < 0 || play > playableTiles.size()){
                    System.out.println("Please enter a valid number between 1 and " + playableTiles.size() + ".");
                    play = sc.nextInt()-1;
                }

                if(leftEnd == rightEnd ||
                   player.getHand().get(play).getA() == leftEnd && player.getHand().get(play).getB() == rightEnd && leftEnd != rightEnd || 
                   player.getHand().get(play).getA() == rightEnd && player.getHand().get(play).getB() == leftEnd && leftEnd != rightEnd){
                    
                    int side = 0;
                    System.out.println("Enter 'l' if you want to place your tile on the left\nEnter 'r' if you want to place your tile on the right: ");
                    sc.nextLine();
                    String s = sc.nextLine().toLowerCase();
                    if(s.equals("l")){
                        side = 1;
                    }
                    else{
                        side = 2;
                    }
  
                    addToBoard(player.getHand().get(play), side);

                }
                else{
                    addToBoard(player.getHand().get(play), 0);
                }

                player.remove(playableTiles.get(play).getA(), playableTiles.get(play).getB());

                System.out.println("Added your piece.");

            }

            System.out.println("\n\n---------------------------------------------------------------");

            System.out.println("\nBoard State: " + YELLOW + board.toString() + RESET);
            System.out.println("\n---------------------------------------------------------------");


        }

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
