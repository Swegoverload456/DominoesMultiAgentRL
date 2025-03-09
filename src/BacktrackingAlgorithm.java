package src;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BacktrackingAlgorithm {

    public static Tile bestMove(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<MoveResult>> futures = new ArrayList<>();

        // Submit tasks for each possible move
        for (Tile tile : hand) {
            if (canPlayTile(tile, leftEnd, rightEnd)) {
                futures.add(executor.submit(new MoveEvaluator(board, hand, tile, leftEnd, rightEnd, true)));
                futures.add(executor.submit(new MoveEvaluator(board, hand, tile, leftEnd, rightEnd, false)));
            }
        }

        // Find the best move from the results
        Tile bestTile = null;
        double maxWinRate = -1;

        for (Future<MoveResult> future : futures) {
            try {
                MoveResult result = future.get();
                if (result.getWinRate() > maxWinRate) {
                    maxWinRate = result.getWinRate();
                    bestTile = result.getTile();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        // If no tile with a higher win rate is found, return the first playable tile
        if (bestTile == null) {
            for (Tile tile : hand) {
                if (canPlayTile(tile, leftEnd, rightEnd)) {
                    return tile;
                }
            }
        }

        return bestTile;
    }

    private static boolean canPlayTile(Tile tile, int leftEnd, int rightEnd) {
        return (tile.getA() == leftEnd || tile.getB() == leftEnd || tile.getA() == rightEnd || tile.getB() == rightEnd);
    }
}