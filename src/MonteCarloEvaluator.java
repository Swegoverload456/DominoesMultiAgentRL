package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

public class MonteCarloEvaluator implements Callable<MoveResult> {
    private ArrayList<Tile> board;
    private ArrayList<Tile> playerHand;
    private Tile tile;
    private int leftEnd;
    private int rightEnd;
    private boolean playOnLeft;
    private ArrayList<Tile> remainingTiles;
    private static final int SIMULATION_COUNT = 10000; // Number of simulations per move
    private static final int MAX_PASSES = 4; // Maximum consecutive passes before stalemate
    private static final Random random = new Random();

    public MonteCarloEvaluator(ArrayList<Tile> board, ArrayList<Tile> playerHand, Tile tile, 
                              int leftEnd, int rightEnd, boolean playOnLeft, 
                              ArrayList<Tile> remainingTiles) {
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
        double totalUtility = 0.0;
        HashMap<String, Double> regretSum = new HashMap<>();
        HashMap<String, Double> strategySum = new HashMap<>();
        int validSimulations = 0;

        for (int i = 0; i < SIMULATION_COUNT; i++) {
            ArrayList<Tile> simBoard = new ArrayList<>(board);
            ArrayList<Tile> simPlayerHand = new ArrayList<>(playerHand);
            ArrayList<Tile> simOpponentHand = new ArrayList<>(remainingTiles);

            simPlayerHand.remove(tile);
            int newLeftEnd = leftEnd;
            int newRightEnd = rightEnd;

            if (simBoard.isEmpty()) {
                if (playOnLeft) {
                    simBoard.add(new Tile(tile.getA(), tile.getB()));
                    newLeftEnd = tile.getA();
                    newRightEnd = tile.getB();
                } else {
                    simBoard.add(new Tile(tile.getB(), tile.getA()));
                    newLeftEnd = tile.getB();
                    newRightEnd = tile.getA();
                }
            } else {
                if (playOnLeft) {
                    if (tile.getA() == leftEnd) {
                        simBoard.add(0, new Tile(tile.getB(), tile.getA()));
                        newLeftEnd = tile.getB();
                    } else if (tile.getB() == leftEnd) {
                        simBoard.add(0, new Tile(tile.getA(), tile.getB()));
                        newLeftEnd = tile.getA();
                    } else {
                        continue; // Invalid move
                    }
                } else {
                    if (tile.getB() == rightEnd) {
                        simBoard.add(new Tile(tile.getA(), tile.getB()));
                        newRightEnd = tile.getB();
                    } else if (tile.getA() == rightEnd) {
                        simBoard.add(new Tile(tile.getB(), tile.getA()));
                        newRightEnd = tile.getA();
                    } else {
                        continue; // Invalid move
                    }
                }
            }

            double utility = simulateGame(simBoard, simPlayerHand, simOpponentHand, 
                                        newLeftEnd, newRightEnd, regretSum, strategySum);
            totalUtility += utility;
            validSimulations++;
        }

        double winRate = validSimulations > 0 ? totalUtility / validSimulations : 0.0;
        return new MoveResult(tile, winRate);
    }

    private double simulateGame(ArrayList<Tile> board, ArrayList<Tile> playerHand, 
                               ArrayList<Tile> opponentHand, int leftEnd, int rightEnd,
                               HashMap<String, Double> regretSum, HashMap<String, Double> strategySum) {
        int consecutivePasses = 0;
        boolean playerTurn = false;

        while (true) {
            ArrayList<Tile> currentHand = playerTurn ? playerHand : opponentHand;
            String stateKey = generateStateKey(board, currentHand, leftEnd, rightEnd, playerTurn);

            if (playerHand.isEmpty()) return 1.0;
            if (opponentHand.isEmpty()) return 0.0;
            if (consecutivePasses >= MAX_PASSES) {
                return evaluateStalemate(playerHand, opponentHand, leftEnd, rightEnd);
            }

            ArrayList<Tile> playableTiles = getPlayableTiles(currentHand, leftEnd, rightEnd);
            if (playableTiles.isEmpty()) {
                consecutivePasses++;
                playerTurn = !playerTurn;
                continue;
            }

            consecutivePasses = 0;

            Tile tileToPlay = selectTileWithRegretMatching(playableTiles, stateKey, regretSum, strategySum, 
                                                          playerTurn, leftEnd, rightEnd);
            boolean playLeft = canPlayTile(tileToPlay, leftEnd) && 
                              (!canPlayTile(tileToPlay, rightEnd) || random.nextBoolean());

            if (!playerTurn) {
                playLeft = evaluateBestSide(board, playerHand, tileToPlay, leftEnd, rightEnd);
            }

            int newLeftEnd = leftEnd;
            int newRightEnd = rightEnd;
            if (playLeft) {
                if (tileToPlay.getA() == leftEnd) {
                    board.add(0, new Tile(tileToPlay.getB(), tileToPlay.getA()));
                    newLeftEnd = tileToPlay.getB();
                } else {
                    board.add(0, new Tile(tileToPlay.getA(), tileToPlay.getB()));
                    newLeftEnd = tileToPlay.getA();
                }
            } else {
                if (tileToPlay.getB() == rightEnd) {
                    board.add(new Tile(tileToPlay.getA(), tileToPlay.getB()));
                    newRightEnd = tileToPlay.getB();
                } else {
                    board.add(new Tile(tileToPlay.getB(), tileToPlay.getA()));
                    newRightEnd = tileToPlay.getA();
                }
            }

            currentHand.remove(tileToPlay);

            double utility = simulateGame(board, playerHand, opponentHand, newLeftEnd, newRightEnd, 
                                         regretSum, strategySum);
            updateRegrets(stateKey, tileToPlay, playLeft, utility, playableTiles, leftEnd, rightEnd, 
                         regretSum, strategySum, playerTurn);

            return utility;
        }
    }

    private Tile selectTileWithRegretMatching(ArrayList<Tile> playableTiles, String stateKey,
                                             HashMap<String, Double> regretSum, HashMap<String, Double> strategySum,
                                             boolean playerTurn, int leftEnd, int rightEnd) {
        double totalPositiveRegret = 0.0;
        HashMap<String, Double> regrets = new HashMap<>();

        for (Tile tile : playableTiles) {
            boolean canLeft = canPlayTile(tile, leftEnd);
            boolean canRight = canPlayTile(tile, rightEnd);
            if (canLeft) {
                String actionKey = stateKey + "|" + tile.toString() + "|Left";
                double regret = regretSum.getOrDefault(actionKey, 0.0);
                regrets.put(actionKey, Math.max(0, regret));
                totalPositiveRegret += regrets.get(actionKey);
            }
            if (canRight) {
                String actionKey = stateKey + "|" + tile.toString() + "|Right";
                double regret = regretSum.getOrDefault(actionKey, 0.0);
                regrets.put(actionKey, Math.max(0, regret));
                totalPositiveRegret += regrets.get(actionKey);
            }
        }

        if (totalPositiveRegret <= 0) {
            return playerTurn ? playableTiles.get(random.nextInt(playableTiles.size())) : 
                              selectOpponentTile(playableTiles, playerHand, null, leftEnd, rightEnd);
        }

        double r = random.nextDouble() * totalPositiveRegret;
        double cumulative = 0.0;
        for (Tile tile : playableTiles) {
            if (canPlayTile(tile, leftEnd)) {
                String actionKey = stateKey + "|" + tile.toString() + "|Left";
                cumulative += regrets.get(actionKey);
                if (r <= cumulative) {
                    strategySum.put(actionKey, strategySum.getOrDefault(actionKey, 0.0) + 1.0);
                    return tile;
                }
            }
            if (canPlayTile(tile, rightEnd)) {
                String actionKey = stateKey + "|" + tile.toString() + "|Right";
                cumulative += regrets.get(actionKey);
                if (r <= cumulative) {
                    strategySum.put(actionKey, strategySum.getOrDefault(actionKey, 0.0) + 1.0);
                    return tile;
                }
            }
        }

        return playableTiles.get(random.nextInt(playableTiles.size()));
    }

    private void updateRegrets(String stateKey, Tile playedTile, boolean playedLeft, double utility,
                              ArrayList<Tile> playableTiles, int leftEnd, int rightEnd,
                              HashMap<String, Double> regretSum, HashMap<String, Double> strategySum,
                              boolean playerTurn) {
        String playedActionKey = stateKey + "|" + playedTile.toString() + "|" + (playedLeft ? "Left" : "Right");
        double playedUtility = utility;

        for (Tile tile : playableTiles) {
            if (canPlayTile(tile, leftEnd)) {
                String actionKey = stateKey + "|" + tile.toString() + "|Left";
                double regret = playedUtility - utility; // Simplified regret
                regretSum.put(actionKey, regretSum.getOrDefault(actionKey, 0.0) + regret);
            }
            if (canPlayTile(tile, rightEnd)) {
                String actionKey = stateKey + "|" + tile.toString() + "|Right";
                double regret = playedUtility - utility; // Simplified regret
                regretSum.put(actionKey, regretSum.getOrDefault(actionKey, 0.0) + regret);
            }
        }
    }

    private String generateStateKey(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd, boolean playerTurn) {
        return leftEnd + ":" + rightEnd + "|" + hand.size() + "|" + (playerTurn ? "P" : "O");
    }

    private Tile selectOpponentTile(ArrayList<Tile> playableTiles, ArrayList<Tile> playerHand,
                                   ArrayList<Tile> opponentHand, int leftEnd, int rightEnd) {
        HashMap<Integer, Integer> numberFrequency = calculateNumberFrequency(playerHand, opponentHand);
        
        Tile bestTile = null;
        double bestScore = -Double.MAX_VALUE;
        
        for (Tile tile : playableTiles) {
            double score = 0.0;
            int freqA = numberFrequency.getOrDefault(tile.getA(), 0);
            int freqB = numberFrequency.getOrDefault(tile.getB(), 0);
            score += (freqA <= 2) ? (3.0 / (freqA + 1)) : 0;
            score += (freqB <= 2) ? (3.0 / (freqB + 1)) : 0;
            if (tile.getA() == tile.getB()) score += 2.0;
            if (tile.getA() == leftEnd || tile.getA() == rightEnd) score += (freqA <= 2) ? 2.0 : 0.5;
            if (tile.getB() == leftEnd || tile.getB() == rightEnd) score += (freqB <= 2) ? 2.0 : 0.5;
            score += (tile.getA() + tile.getB()) * 0.1;
            
            if (score > bestScore) {
                bestScore = score;
                bestTile = tile;
            }
        }
        
        return bestTile != null ? bestTile : playableTiles.get(random.nextInt(playableTiles.size()));
    }

    private HashMap<Integer, Integer> calculateNumberFrequency(ArrayList<Tile> playerHand, 
                                                             ArrayList<Tile> opponentHand) {
        HashMap<Integer, Integer> frequency = new HashMap<>();
        for (Tile tile : playerHand) {
            frequency.put(tile.getA(), frequency.getOrDefault(tile.getA(), 0) + 1);
            frequency.put(tile.getB(), frequency.getOrDefault(tile.getB(), 0) + 1);
        }
        if (opponentHand != null) {
            for (Tile tile : opponentHand) {
                frequency.put(tile.getA(), frequency.getOrDefault(tile.getA(), 0) + 1);
                frequency.put(tile.getB(), frequency.getOrDefault(tile.getB(), 0) + 1);
            }
        }
        return frequency;
    }

    private boolean evaluateBestSide(ArrayList<Tile> board, ArrayList<Tile> playerHand, 
                                   Tile tile, int leftEnd, int rightEnd) {
        HashMap<Integer, Integer> numberFrequency = calculateNumberFrequency(playerHand, new ArrayList<>());
        double leftScore = 0.0, rightScore = 0.0;
        int leftEndAfter = leftEnd; // Default initialization
        int rightEndAfter = rightEnd; // Default initialization

        // Evaluate left side
        if (tile.getA() == leftEnd) {
            leftEndAfter = tile.getB();
            rightEndAfter = rightEnd;
            int freq = numberFrequency.getOrDefault(leftEndAfter, 0);
            leftScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        } else if (tile.getB() == leftEnd) {
            leftEndAfter = tile.getA();
            rightEndAfter = rightEnd;
            int freq = numberFrequency.getOrDefault(leftEndAfter, 0);
            leftScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        }
        int leftPlayableCount = getPlayableTiles(playerHand, leftEndAfter, rightEndAfter).size();
        leftScore -= leftPlayableCount * 0.5;

        // Reset for right side evaluation
        leftEndAfter = leftEnd; // Reset to default
        rightEndAfter = rightEnd; // Reset to default

        // Evaluate right side
        if (tile.getB() == rightEnd) {
            leftEndAfter = leftEnd;
            rightEndAfter = tile.getB();
            int freq = numberFrequency.getOrDefault(rightEndAfter, 0);
            rightScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        } else if (tile.getA() == rightEnd) {
            leftEndAfter = leftEnd;
            rightEndAfter = tile.getA();
            int freq = numberFrequency.getOrDefault(rightEndAfter, 0);
            rightScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        }
        int rightPlayableCount = getPlayableTiles(playerHand, leftEndAfter, rightEndAfter).size();
        rightScore -= rightPlayableCount * 0.5;

        return leftScore >= rightScore;
    }

    private double evaluateStalemate(ArrayList<Tile> playerHand, ArrayList<Tile> opponentHand, 
                                    int leftEnd, int rightEnd) {
        int playerSum = sumTiles(playerHand);
        int opponentSum = sumTiles(opponentHand);
        int playerHandSize = playerHand.size();
        int opponentHandSize = opponentHand.size();

        double baseScore = 0.5;
        if (playerSum < opponentSum) baseScore += 0.2;
        else if (playerSum > opponentSum) baseScore -= 0.2;

        if (playerHandSize < opponentHandSize) baseScore += 0.2;
        else if (playerHandSize > opponentHandSize) baseScore -= 0.2;

        int playerPlayable = getPlayableTiles(playerHand, leftEnd, rightEnd).size();
        int opponentPlayable = getPlayableTiles(opponentHand, leftEnd, rightEnd).size();
        if (playerPlayable > opponentPlayable) baseScore += 0.1;
        else if (playerPlayable < opponentPlayable) baseScore -= 0.1;

        return Math.max(0.0, Math.min(1.0, baseScore));
    }

    private int sumTiles(ArrayList<Tile> tiles) {
        int sum = 0;
        for (Tile tile : tiles) {
            sum += tile.getA() + tile.getB();
        }
        return sum;
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