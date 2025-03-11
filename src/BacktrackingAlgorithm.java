package src;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BacktrackingAlgorithm {
    private static final ConcurrentHashMap<String, Double> memo = new ConcurrentHashMap<>();

    // Helper class to store tile, win rate, and placement side
    private static class MoveEvaluation {
        Tile tile;
        double winRate;
        boolean playOnLeft;

        MoveEvaluation(Tile tile, double winRate, boolean playOnLeft) {
            this.tile = tile;
            this.winRate = winRate;
            this.playOnLeft = playOnLeft;
        }
    }

    public static Tile bestMove(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<MoveResult>> futures = new ArrayList<>();
        ArrayList<Tile> remainingTiles = getRemainingTiles(board, hand);

        // Submit tasks for each possible move
        for (Tile tile : hand) {
            if (canPlayTile(tile, leftEnd, rightEnd)) {
                futures.add(executor.submit(new MoveEvaluator(board, hand, tile, leftEnd, rightEnd, true, remainingTiles)));
                futures.add(executor.submit(new MoveEvaluator(board, hand, tile, leftEnd, rightEnd, false, remainingTiles)));
            }
        }

        // Find the best move and track placement side
        Tile bestTile = null;
        double bestWinRate = -1.0;
        boolean bestPlayOnLeft = false; // Track the side for the best move
        List<MoveEvaluation> evaluations = new ArrayList<>();

        for (Future<MoveResult> future : futures) {
            try {
                MoveResult result = future.get(5, TimeUnit.SECONDS);
                boolean playOnLeft = futures.indexOf(future) % 2 == 0; // Even indices are left, odd are right
                evaluations.add(new MoveEvaluation(result.getTile(), result.getWinRate(), playOnLeft));

                // Print with side information
                String side = playOnLeft ? "Left" : "Right";
                //System.out.println("Tile: " + result.getTile() + " WinRate: " + result.getWinRate() + " (" + side + ")");

                if (result.getWinRate() > bestWinRate) {
                    bestWinRate = result.getWinRate();
                    bestTile = result.getTile();
                    bestPlayOnLeft = playOnLeft;
                    if(!playOnLeft){
                        bestTile.setSide(2);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // If no best tile found, return first playable tile
        if (bestTile == null) {
            for (Tile tile : hand) {
                if (canPlayTile(tile, leftEnd, rightEnd)) {
                    return tile;
                }
            }
        }

        // Print the best move with recommended side
        System.out.println("Your best theoretical move here is: " + bestTile + " (Play on " + (bestPlayOnLeft ? "Left" : "Right") + ")");
        return bestTile;
    }

    public static ArrayList<Tile> getRemainingTiles(ArrayList<Tile> board, ArrayList<Tile> playerTiles) {
        Player temp = new Player();
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j <= i; j++) {
                temp.add(new Tile(j, i));
            }
        }

        for (Tile tile : board) {
            temp.remove(tile.getA(), tile.getB());
        }

        for (Tile tile : playerTiles) {
            temp.remove(tile.getA(), tile.getB());
        }

        return temp.getHand();
    }

    private static boolean canPlayTile(Tile tile, int leftEnd, int rightEnd) {
        return (tile.getA() == leftEnd || tile.getB() == leftEnd ||
                tile.getA() == rightEnd || tile.getB() == rightEnd);
    }
}