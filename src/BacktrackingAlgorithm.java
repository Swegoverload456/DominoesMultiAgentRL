package src;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BacktrackingAlgorithm {
    public static Tile bestMove(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<MoveResult>> futures = new ArrayList<>();
        ArrayList<Tile> remainingTiles = getRemainingTiles(board, hand);

        // If the board is empty, any tile can be played
        if (board.isEmpty()) {
            for (Tile tile : hand) {
                // Since there's no side preference on an empty board, simulate both orientations
                futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, true, remainingTiles)));
                futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, false, remainingTiles)));
            }
        } else {
            // Existing logic for non-empty board
            for (Tile tile : hand) {
                if (canPlayTile(tile, leftEnd, rightEnd)) {
                    futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, true, remainingTiles)));
                    futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, false, remainingTiles)));
                }
            }
        }

        // Find the best move
        Tile bestTile = null;
        double bestWinRate = -1.0;
        boolean bestPlayOnLeft = false;
        
        for (Future<MoveResult> future : futures) {
            try {
                MoveResult result = future.get(5, TimeUnit.SECONDS);
                boolean playOnLeft = futures.indexOf(future) % 2 == 0;
                String side = playOnLeft ? "Left" : "Right";
                //System.out.println("Tile: " + result.getTile() + " WinRate: " + result.getWinRate() + " (" + side + ")");

                if (result.getWinRate() > bestWinRate) {
                    bestWinRate = result.getWinRate();
                    bestTile = result.getTile();
                    bestPlayOnLeft = playOnLeft;
                    if (!playOnLeft) {
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

        // Fallback if no best tile found
        if (bestTile == null) {
            return hand.get(0); // Any tile can be played on an empty board
        }

        //System.out.println("Your best theoretical move here is: " + bestTile + (board.isEmpty() ? "" : " (Play on " + (bestPlayOnLeft ? "Left" : "Right") + ")"));
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