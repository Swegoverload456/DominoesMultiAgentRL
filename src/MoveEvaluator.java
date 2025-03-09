package src;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class MoveEvaluator implements Callable<MoveResult> {
    private ArrayList<Tile> board;
    private ArrayList<Tile> hand;
    private Tile tile;
    private int leftEnd;
    private int rightEnd;
    private boolean playOnLeft;

    public MoveEvaluator(ArrayList<Tile> board, ArrayList<Tile> hand, Tile tile, int leftEnd, int rightEnd, boolean playOnLeft) {
        this.board = new ArrayList<>(board);
        this.hand = new ArrayList<>(hand);
        this.tile = tile;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.playOnLeft = playOnLeft;
    }

    @Override
    public MoveResult call() {
        // Simulate playing the tile
        ArrayList<Tile> newBoard = new ArrayList<>(board);
        int newLeftEnd = leftEnd;
        int newRightEnd = rightEnd;
        playTile(newBoard, tile, playOnLeft, newLeftEnd, newRightEnd);

        // Evaluate the game state after playing this tile
        double winRate = evaluateGameState(newBoard, hand, newLeftEnd, newRightEnd, 0);

        // Return the result
        return new MoveResult(tile, winRate);
    }

    private void playTile(ArrayList<Tile> board, Tile tile, boolean playOnLeft, int leftEnd, int rightEnd) {
        if (playOnLeft) {
            if (tile.getA() == leftEnd) {
                board.add(0, new Tile(tile.getB(), tile.getA()));
                leftEnd = tile.getB();
            } else if (tile.getB() == leftEnd) {
                board.add(0, new Tile(tile.getA(), tile.getB()));
                leftEnd = tile.getA();
            }
        } else {
            if (tile.getA() == rightEnd) {
                board.add(new Tile(tile.getA(), tile.getB()));
                rightEnd = tile.getB();
            } else if (tile.getB() == rightEnd) {
                board.add(new Tile(tile.getB(), tile.getA()));
                rightEnd = tile.getA();
            }
        }
    }

    private double evaluateGameState(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd, int depth) {
        if (depth > (28 - board.size())) {
            return 0.5;
        }

        if (hand.isEmpty()) {
            return 1.0;
        }

        double totalWinRate = 0.0;
        int validMoves = 0;

        for (Tile tile : hand) {
            if (canPlayTile(tile, leftEnd, rightEnd)) {
                ArrayList<Tile> newBoard = new ArrayList<>(board);
                int newLeftEnd = leftEnd;
                int newRightEnd = rightEnd;
                playTile(newBoard, tile, true, newLeftEnd, newRightEnd);

                double winRate = evaluateGameState(newBoard, hand, newLeftEnd, newRightEnd, depth + 1);
                totalWinRate += winRate;
                validMoves++;

                newBoard = new ArrayList<>(board);
                newLeftEnd = leftEnd;
                newRightEnd = rightEnd;
                playTile(newBoard, tile, false, newLeftEnd, newRightEnd);

                winRate = evaluateGameState(newBoard, hand, newLeftEnd, newRightEnd, depth + 1);
                totalWinRate += winRate;
                validMoves++;
            }
        }

        if (validMoves == 0) {
            return 0.0;
        }

        return totalWinRate / validMoves;
    }

    private boolean canPlayTile(Tile tile, int leftEnd, int rightEnd) {
        return (tile.getA() == leftEnd || tile.getB() == leftEnd || tile.getA() == rightEnd || tile.getB() == rightEnd);
    }
}