package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Callable;

public class MoveEvaluator implements Callable<MoveResult> {
    private ArrayList<Tile> board;
    private ArrayList<Tile> playerHand;
    private Tile tile;
    private int leftEnd;
    private int rightEnd;
    private boolean playOnLeft;
    private ArrayList<Tile> remainingTiles;
    private static final int BASE_SIMULATION_DEPTH = 5; // Base depth for simulations
    private static final int BASE_SIMULATION_COUNT = 10; // Base number of simulations
    private static final int MAX_PASSES = 4; // Maximum consecutive passes before stalemate
    private static final int MAX_MOVES_TO_SAMPLE = 7; // Maximum number of moves to sample per player
    private static final Random random = new Random(); // Random instance for sampling moves

    public MoveEvaluator(ArrayList<Tile> board, ArrayList<Tile> playerHand, Tile tile, int leftEnd,
                         int rightEnd, boolean playOnLeft, ArrayList<Tile> remainingTiles) {
        this.board = new ArrayList<>(board);
        this.playerHand = new ArrayList<>(playerHand);
        this.tile = tile;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.playOnLeft = playOnLeft;
        this.remainingTiles = new ArrayList<>(remainingTiles);
    }

    @Override
    public MoveResult call() {
        double totalWinRate = 0.0;

        // Dynamically determine simulation count based on game state
        int simulationCount = determineSimulationCount();
        for (int i = 0; i < simulationCount; i++) {
            ArrayList<Tile> simBoard = new ArrayList<>(board);
            ArrayList<Tile> simPlayerHand = new ArrayList<>(playerHand);
            ArrayList<Tile> simRemainingTiles = new ArrayList<>(remainingTiles);

            simPlayerHand.remove(tile);
            int newLeftEnd = leftEnd;
            int newRightEnd = rightEnd;

            if (playOnLeft) {
                if (tile.getA() == leftEnd) {
                    simBoard.add(0, new Tile(tile.getB(), tile.getA()));
                    newLeftEnd = tile.getB();
                } else {
                    simBoard.add(0, new Tile(tile.getA(), tile.getB()));
                    newLeftEnd = tile.getA();
                }
            } else {
                if (tile.getB() == rightEnd) {
                    simBoard.add(new Tile(tile.getA(), tile.getB()));
                    newRightEnd = tile.getB();
                } else {
                    simBoard.add(new Tile(tile.getB(), tile.getA()));
                    newRightEnd = tile.getA();
                }
            }

            double winRate = simulateGame(simBoard, simPlayerHand, newLeftEnd, newRightEnd,
                    simRemainingTiles, 0, 0, 0);
            totalWinRate += winRate;
        }

        return new MoveResult(tile, totalWinRate / simulationCount);
    }

    /**
     * Determines the number of simulations to run based on the game state.
     * Early game: Fewer simulations (less uncertainty).
     * Late game: More simulations (higher impact of decisions).
     */
    private int determineSimulationCount() {
        int totalTiles = playerHand.size() + remainingTiles.size();
        if (totalTiles > 20) {
            return BASE_SIMULATION_COUNT; // Early game: fewer simulations
        } else if (totalTiles < 10) {
            return BASE_SIMULATION_COUNT * 2; // Late game: more simulations
        }
        return BASE_SIMULATION_COUNT; // Mid-game: default simulations
    }

    /**
     * Determines the simulation depth based on the game state.
     * Early game: Smaller depth (fewer tiles played, more uncertainty).
     * Late game: Larger depth (closer to end, more accuracy needed).
     */
    private int determineSimulationDepth() {
        int totalTiles = playerHand.size() + remainingTiles.size();
        if (totalTiles > 20) {
            return BASE_SIMULATION_DEPTH; // Early game: smaller depth
        } else if (totalTiles < 10) {
            return BASE_SIMULATION_DEPTH * 2; // Late game: larger depth
        }
        return BASE_SIMULATION_DEPTH; // Mid-game: default depth
    }

    private double simulateGame(ArrayList<Tile> board, ArrayList<Tile> playerHand, int leftEnd,
                                int rightEnd, ArrayList<Tile> remainingTiles, int currentPlayer,
                                int depth, int consecutivePasses) {
        // Determine the simulation depth dynamically
        int simulationDepth = determineSimulationDepth();

        // Base cases
        if (playerHand.isEmpty()) {
            return 1.0; // Player wins
        }
        if (remainingTiles.isEmpty() && getPlayableTiles(playerHand, leftEnd, rightEnd).isEmpty()) {
            return 0.0; // Player loses if no tiles remain and they can't play
        }
        if (depth >= simulationDepth) {
            return evaluateBoardState(playerHand, remainingTiles, leftEnd, rightEnd);
        }
        if (consecutivePasses >= MAX_PASSES) {
            return evaluateBoardState(playerHand, remainingTiles, leftEnd, rightEnd); // Stalemate
        }

        // Get playable tiles for current player
        ArrayList<Tile> currentHand = currentPlayer == 0 ? playerHand : remainingTiles;
        ArrayList<Tile> playableTiles = getPlayableTiles(currentHand, leftEnd, rightEnd);

        // If no playable tiles, pass the turn
        if (playableTiles.isEmpty()) {
            return simulateGame(board, playerHand, leftEnd, rightEnd, remainingTiles,
                    (currentPlayer + 1) % 4, depth, consecutivePasses + 1);
        }

        // Reset consecutive passes since a play is possible
        consecutivePasses = 0;

        // Sample a subset of moves instead of simulating all possible moves
        ArrayList<Tile> tilesToSimulate = sampleMoves(playableTiles);

        double winRateSum = 0.0;
        int simulations = 0;

        for (Tile playTile : tilesToSimulate) {
            ArrayList<Tile> newBoard = new ArrayList<>(board);
            ArrayList<Tile> newPlayerHand = new ArrayList<>(playerHand);
            ArrayList<Tile> newRemainingTiles = new ArrayList<>(remainingTiles);
            int newLeftEnd = leftEnd;
            int newRightEnd = rightEnd;

            // Remove played tile
            if (currentPlayer == 0) {
                newPlayerHand.remove(playTile);
            } else {
                newRemainingTiles.remove(playTile);
            }

            // Try playing on both ends
            for (boolean playLeft : new boolean[]{true, false}) {
                if (canPlayTile(playTile, playLeft ? leftEnd : rightEnd)) {
                    ArrayList<Tile> simBoard = new ArrayList<>(newBoard);
                    int simLeftEnd = newLeftEnd;
                    int simRightEnd = newRightEnd;

                    if (playLeft) {
                        if (playTile.getA() == simLeftEnd) {
                            simBoard.add(0, new Tile(playTile.getB(), playTile.getA()));
                            simLeftEnd = playTile.getB();
                        } else {
                            simBoard.add(0, new Tile(playTile.getA(), playTile.getB()));
                            simLeftEnd = playTile.getA();
                        }
                    } else {
                        if (playTile.getB() == simRightEnd) {
                            simBoard.add(new Tile(playTile.getA(), playTile.getB()));
                            simRightEnd = playTile.getB();
                        } else {
                            simBoard.add(new Tile(playTile.getB(), playTile.getA()));
                            simRightEnd = playTile.getA();
                        }
                    }

                    double winRate = simulateGame(simBoard, newPlayerHand, simLeftEnd, simRightEnd,
                            newRemainingTiles, (currentPlayer + 1) % 4, depth + 1, consecutivePasses);
                    winRateSum += winRate;
                    simulations++;
                }
            }
        }

        return simulations > 0 ? winRateSum / simulations : evaluateBoardState(playerHand, remainingTiles, leftEnd, rightEnd);
    }

    /**
     * Randomly samples a subset of moves to simulate, reducing computational load.
     * If the number of playable tiles is small, all are used; otherwise, a random subset is selected.
     */
    private ArrayList<Tile> sampleMoves(ArrayList<Tile> playableTiles) {
        if (playableTiles.size() <= MAX_MOVES_TO_SAMPLE) {
            return new ArrayList<>(playableTiles);
        }

        // Create a shuffled copy of playable tiles and take the first MAX_MOVES_TO_SAMPLE
        ArrayList<Tile> shuffledTiles = new ArrayList<>(playableTiles);
        Collections.shuffle(shuffledTiles, random);
        ArrayList<Tile> sampledTiles = new ArrayList<>();
        for (int i = 0; i < MAX_MOVES_TO_SAMPLE && i < shuffledTiles.size(); i++) {
            sampledTiles.add(shuffledTiles.get(i));
        }
        return sampledTiles;
    }

    /**
     * Improved heuristic evaluation that considers:
     * 1. Ratio of player's tiles to remaining tiles.
     * 2. Diversity of numbers in player's hand (higher diversity increases chances of playing).
     * 3. Matchability with current board ends (tiles that match ends are more valuable).
     */
    private double evaluateBoardState(ArrayList<Tile> playerHand, ArrayList<Tile> remainingTiles,
                                      int leftEnd, int rightEnd) {
        // Base case: avoid division by zero
        double playerTileCount = playerHand.size();
        double remainingTileCount = remainingTiles.size();
        double totalTileCount = playerTileCount + remainingTileCount;
        if (totalTileCount == 0) return 0.5;

        // Factor 1: Ratio of player's tiles to total tiles (fewer tiles = better)
        double tileRatio = 1.0 - (playerTileCount / totalTileCount);

        // Factor 2: Diversity of numbers in player's hand
        HashSet<Integer> numbersInHand = new HashSet<>();
        for (Tile tile : playerHand) {
            numbersInHand.add(tile.getA());
            numbersInHand.add(tile.getB());
        }
        double diversityScore = numbersInHand.size() / 13.0; // Normalize by max possible numbers (0-6)

        // Factor 3: Matchability with current board ends
        double matchabilityScore = 0.0;
        if (leftEnd != -1 && rightEnd != -1) { // Ensure board has been initialized
            int matchingTiles = 0;
            for (Tile tile : playerHand) {
                if (tile.getA() == leftEnd || tile.getB() == leftEnd ||
                        tile.getA() == rightEnd || tile.getB() == rightEnd) {
                    matchingTiles++;
                }
            }
            matchabilityScore = (double) matchingTiles / playerHand.size();
        }

        // Combine factors (weights can be tuned)
        double combinedScore = 0.6 * tileRatio + 0.2 * diversityScore + 0.2 * matchabilityScore;
        return Math.max(0.0, Math.min(1.0, combinedScore)); // Ensure score is between 0 and 1
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
    }
}