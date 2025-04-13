package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Callable;

public class MyAttempt{
    private ArrayList<Tile> board;
    private ArrayList<Tile> playerHand;
    private Tile tile;
    private int leftEnd;
    private int rightEnd;
    private boolean playOnLeft;
    private ArrayList<Tile> remainingTiles;
    private static final int MAX_PASSES = 4; // Maximum consecutive passes before stalemate

    public MyAttempt(ArrayList<Tile> board, ArrayList<Tile> playerHand, Tile tile, int leftEnd, int rightEnd, boolean playOnLeft, ArrayList<Tile> remainingTiles) {
        this.board = new ArrayList<>(board);
        this.playerHand = new ArrayList<>(playerHand);
        this.tile = tile;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.playOnLeft = playOnLeft;
        this.remainingTiles = new ArrayList<>(remainingTiles);
    }


    
    /*private double simGame(ArrayList<Tile> board, ArrayList<Tile> playerHand, int leftEnd,
    int rightEnd, ArrayList<Tile> remainingTiles, int currentPlayer,
    int depth, int consecutivePasses){

        // Base cases
        if (playerHand.size() == 1 && getPlayableTiles(playerHand, leftEnd, rightEnd).size() == 1) {
            return 1.0; // Player wins
        }

        if (remainingTiles.isEmpty() && getPlayableTiles(playerHand, leftEnd, rightEnd).isEmpty()) {
            return 0.0; // Player loses if no tiles remain and they can't play
        }

        if (consecutivePasses >= MAX_PASSES) {
            return evaluateBoardState(playerHand, remainingTiles); // Stalemate
        }
        
        

    }

    private double evaluateBoardState(ArrayList<Tile> playerHand, ArrayList<Tile> remaining){
        double playerSum = 0;
        double remainingSum = 0;

        for(int i = 0; i < playerHand.size(); i++){
            playerSum += playerHand.get(i).sum();
        }

        for(int i = 0; i < remaining.size(); i++){
            remainingSum += remaining.get(i).sum();
        }

        remainingSum /= 3; 

        if(playerSum < remainingSum){
            return 1.0;
        }

        return 0.0;
    }

    private ArrayList<Tile> getPlayableTiles(ArrayList<Tile> tiles, int leftEnd, int rightEnd) {
        ArrayList<Tile> playableTiles = new ArrayList<>();
        for (Tile tile : tiles) {
            if (canPlayTile(tile, leftEnd) || canPlayTile(tile, rightEnd)) {
                playableTiles.add(tile);
            }
        }
        return playableTiles;
    }

    private boolean canPlayTile(Tile tile, int end) {
        return tile.getA() == end || tile.getB() == end;
    }*/
}