package src;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BacktrackingAlgorithm {
    public static Tile bestMove(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<MoveResult>> futures = new ArrayList<>();
        ArrayList<Tile> remainingTiles = getRemainingTiles(board, hand);

        if (board.isEmpty()) {
            for (Tile tile : hand) {
                futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, true, remainingTiles)));
                futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, false, remainingTiles)));
            }
        } else {
            for (Tile tile : hand) {
                boolean canPlayLeft = canPlayTileOnSide(tile, leftEnd);
                boolean canPlayRight = canPlayTileOnSide(tile, rightEnd);

                if (canPlayLeft) {
                    futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, true, remainingTiles)));
                }
                if (canPlayRight) {
                    futures.add(executor.submit(new MonteCarloEvaluator(board, hand, tile, leftEnd, rightEnd, false, remainingTiles)));
                }
            }
        }

        Tile bestTile = null;
        double bestWinRate = -1.0;
        boolean bestPlayOnLeft = false;

        for (Future<MoveResult> future : futures) {
            try {
                MoveResult result = future.get(5, TimeUnit.SECONDS);
                boolean playOnLeft = futures.indexOf(future) % 2 == 0 || !canPlayTileOnSide(result.getTile(), rightEnd);
                String side = playOnLeft ? "Left" : "Right";
                System.out.println("Tile: " + result.getTile() + " WinRate: " + result.getWinRate() + " (" + side + ")");

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

        if (bestTile == null) {
            return hand.get(0);
        }

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
        return canPlayTileOnSide(tile, leftEnd) || canPlayTileOnSide(tile, rightEnd);
    }

    private static boolean canPlayTileOnSide(Tile tile, int end) {
        return tile.getA() == end || tile.getB() == end;
    }
}