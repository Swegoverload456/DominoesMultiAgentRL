package src.main.java.src;

import java.util.ArrayList;
import java.util.Collections;

public class Player{
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

    public double[] getState(ArrayList<Tile> board, int leftEnd, int rightEnd) {
        double[] state = new double[490];
        int idx = 0;
    
        // Board: 56 elements, padded with -1, one-hot encoded to 448
        int[] boardSeq = new int[56];
        for (int i = 0; i < board.size(); i++) {
            boardSeq[i * 2] = board.get(i).getA();
            boardSeq[i * 2 + 1] = board.get(i).getB();
        }
        for (int i = board.size() * 2; i < 56; i++) {
            boardSeq[i] = -1;
        }
        for (int i = 0; i < 56; i++) {
            int val = boardSeq[i] + 1; // -1 to 6 -> 0 to 7
            state[idx + val] = 1.0;
            idx += 8;
        }
    
        // Hand: 28 binary
        for (Tile tile : hand) {
            int tileIdx = Game.allTiles.indexOf(tile);
            state[idx + tileIdx] = 1.0;
        }
        idx += 28;
    
        // Left end: 7 one-hot
        if (leftEnd != -1) state[idx + leftEnd] = 1.0;
        idx += 7;
    
        // Right end: 7 one-hot
        if (rightEnd != -1) state[idx + rightEnd] = 1.0;
    
        return state;
    }

    public boolean[] getValidActions(int leftEnd, int rightEnd) {
        boolean[] valid = new boolean[57];
        ArrayList<Tile> playable = getPlayableTiles(leftEnd, rightEnd);
        if (playable.isEmpty()) {
            valid[56] = true; // Pass
            return valid;
        }
        for (Tile tile : hand) {
            int idx = Game.allTiles.indexOf(tile);
            int a = tile.getA(), b = tile.getB();
            if (leftEnd == -1 && rightEnd == -1) {
                valid[idx] = true; // Left
                valid[28 + idx] = true; // Right
            } else {
                if (a == leftEnd || b == leftEnd) valid[idx] = true;
                if (a == rightEnd || b == rightEnd) valid[28 + idx] = true;
            }
        }
        return valid;
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