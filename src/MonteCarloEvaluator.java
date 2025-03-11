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
    private static final int SIMULATION_COUNT = 1000; // Number of simulations per move
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
        double totalScore = 0.0;
        
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            // Create simulation copies
            ArrayList<Tile> simBoard = new ArrayList<>(board);
            ArrayList<Tile> simPlayerHand = new ArrayList<>(playerHand);
            ArrayList<Tile> simOpponentHand = new ArrayList<>(remainingTiles);
            
            // Play the initial tile
            simPlayerHand.remove(tile);
            int newLeftEnd = leftEnd;
            int newRightEnd = rightEnd;
            
            // Handle empty board case
            if (simBoard.isEmpty()) {
                // When the board is empty, set the ends based on the tile
                // playOnLeft determines orientation, but it's arbitrary for the first tile
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
                // Existing logic for non-empty board
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
            }

            double score = simulateGame(simBoard, simPlayerHand, simOpponentHand, 
                                      newLeftEnd, newRightEnd);
            totalScore += score;
        }

        double winRate = totalScore / SIMULATION_COUNT;
        return new MoveResult(tile, winRate);
    }

    private double simulateGame(ArrayList<Tile> board, ArrayList<Tile> playerHand, 
                               ArrayList<Tile> opponentHand, int leftEnd, int rightEnd) {
        int consecutivePasses = 0;
        boolean playerTurn = false; // Start with opponent's turn after our move
        
        while (true) {
            ArrayList<Tile> currentHand = playerTurn ? playerHand : opponentHand;
            
            // Check win condition
            if (playerHand.isEmpty()) return 1.0; // Player wins by emptying hand
            if (opponentHand.isEmpty()) return 0.0; // Opponent wins by emptying hand
            
            // Check stalemate condition
            if (consecutivePasses >= MAX_PASSES) {
                return evaluateStalemate(playerHand, opponentHand, leftEnd, rightEnd);
            }

            ArrayList<Tile> playableTiles = getPlayableTiles(currentHand, leftEnd, rightEnd);
            
            if (playableTiles.isEmpty()) {
                consecutivePasses++;
                playerTurn = !playerTurn;
                continue;
            }

            // Reset passes on a play
            consecutivePasses = 0;

            // Choose tile to play
            Tile tileToPlay;
            if (!playerTurn) {
                tileToPlay = selectOpponentTile(playableTiles, playerHand, opponentHand, leftEnd, rightEnd);
            } else {
                tileToPlay = playableTiles.get(random.nextInt(playableTiles.size()));
            }
            
            // Decide which side to play on
            boolean playLeft = canPlayTile(tileToPlay, leftEnd);
            if (canPlayTile(tileToPlay, rightEnd) && canPlayTile(tileToPlay, leftEnd)) {
                if (!playerTurn) {
                    playLeft = evaluateBestSide(board, playerHand, tileToPlay, leftEnd, rightEnd);
                } else {
                    playLeft = random.nextBoolean();
                }
            }

            // Play the tile
            if (playLeft) {
                if (tileToPlay.getA() == leftEnd) {
                    board.add(0, new Tile(tileToPlay.getB(), tileToPlay.getA()));
                    leftEnd = tileToPlay.getB();
                } else {
                    board.add(0, new Tile(tileToPlay.getA(), tileToPlay.getB()));
                    leftEnd = tileToPlay.getA();
                }
            } else {
                if (tileToPlay.getB() == rightEnd) {
                    board.add(new Tile(tileToPlay.getA(), tileToPlay.getB()));
                    rightEnd = tileToPlay.getB();
                } else {
                    board.add(new Tile(tileToPlay.getB(), tileToPlay.getA()));
                    rightEnd = tileToPlay.getA();
                }
            }

            // Remove the played tile
            currentHand.remove(tileToPlay);
            playerTurn = !playerTurn;
        }
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
            
            if (tile.getA() == leftEnd || tile.getA() == rightEnd) {
                score += (freqA <= 2) ? 2.0 : 0.5;
            }
            if (tile.getB() == leftEnd || tile.getB() == rightEnd) {
                score += (freqB <= 2) ? 2.0 : 0.5;
            }
            
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
        
        for (Tile tile : opponentHand) {
            frequency.put(tile.getA(), frequency.getOrDefault(tile.getA(), 0) + 1);
            frequency.put(tile.getB(), frequency.getOrDefault(tile.getB(), 0) + 1);
        }
        
        return frequency;
    }

    private boolean evaluateBestSide(ArrayList<Tile> board, ArrayList<Tile> playerHand, 
                                   Tile tile, int leftEnd, int rightEnd) {
        HashMap<Integer, Integer> numberFrequency = calculateNumberFrequency(playerHand, new ArrayList<>());
        
        int leftEndAfter, rightEndAfter;
        double leftScore = 0.0, rightScore = 0.0;
        
        if (tile.getA() == leftEnd) {
            leftEndAfter = tile.getB();
            rightEndAfter = rightEnd;
            int freq = numberFrequency.getOrDefault(leftEndAfter, 0);
            leftScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        } else {
            leftEndAfter = tile.getA();
            rightEndAfter = rightEnd;
            int freq = numberFrequency.getOrDefault(leftEndAfter, 0);
            leftScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        }
        int leftPlayableCount = getPlayableTiles(playerHand, leftEndAfter, rightEndAfter).size();
        leftScore -= leftPlayableCount * 0.5;
        
        if (tile.getB() == rightEnd) {
            leftEndAfter = leftEnd;
            rightEndAfter = tile.getB();
            int freq = numberFrequency.getOrDefault(rightEndAfter, 0);
            rightScore += (freq <= 2) ? (3.0 / (freq + 1)) : 0;
        } else {
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
        if (playerSum < opponentSum) {
            baseScore += 0.2;
        } else if (playerSum > opponentSum) {
            baseScore -= 0.2;
        }
        
        if (playerHandSize < opponentHandSize) {
            baseScore += 0.2;
        } else if (playerHandSize > opponentHandSize) {
            baseScore -= 0.2;
        }
        
        int playerPlayable = getPlayableTiles(playerHand, leftEnd, rightEnd).size();
        int opponentPlayable = getPlayableTiles(opponentHand, leftEnd, rightEnd).size();
        if (playerPlayable > opponentPlayable) {
            baseScore += 0.1;
        } else if (playerPlayable < opponentPlayable) {
            baseScore -= 0.1;
        }
        
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