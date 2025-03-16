package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

public class CFRAlgorithm {
    private static final Random random = new Random();
    private static final int ITERATIONS = 10000; // Number of iterations to train CFR
    private static final int MAX_PASSES = 4; // Maximum consecutive passes before stalemate
    private HashMap<String, Node> nodeMap = new HashMap<>(); // Regret and strategy table
    private HashMap<String, Double> utilityCache = new HashMap<>(); // Cache for computed utilities

    private static class Node {
        String infoSet;
        HashMap<String, Double> regretSum;
        HashMap<String, Double> strategySum;
        ArrayList<String> actions;

        Node(String infoSet, ArrayList<String> actions) {
            this.infoSet = infoSet;
            this.actions = actions;
            this.regretSum = new HashMap<>();
            this.strategySum = new HashMap<>();
            for (String action : actions) {
                regretSum.put(action, 0.0);
                strategySum.put(action, 0.0);
            }
        }

        double[] getStrategy(double realizationWeight) {
            double[] strategy = new double[actions.size()];
            double normalizingSum = 0.0;

            for (int a = 0; a < actions.size(); a++) {
                strategy[a] = Math.max(regretSum.get(actions.get(a)), 0);
                normalizingSum += strategy[a];
            }

            if (normalizingSum > 0) {
                for (int a = 0; a < actions.size(); a++) {
                    strategy[a] /= normalizingSum;
                }
            } else {
                for (int a = 0; a < actions.size(); a++) {
                    strategy[a] = 1.0 / actions.size();
                }
            }

            for (int a = 0; a < actions.size(); a++) {
                strategySum.put(actions.get(a), strategySum.get(actions.get(a)) + realizationWeight * strategy[a]);
            }

            return strategy;
        }

        double[] getAverageStrategy() {
            double[] avgStrategy = new double[actions.size()];
            double normalizingSum = 0.0;

            for (int a = 0; a < actions.size(); a++) {
                normalizingSum += strategySum.get(actions.get(a));
            }

            if (normalizingSum > 0) {
                for (int a = 0; a < actions.size(); a++) {
                    avgStrategy[a] = strategySum.get(actions.get(a)) / normalizingSum;
                }
            } else {
                for (int a = 0; a < actions.size(); a++) {
                    avgStrategy[a] = 1.0 / actions.size();
                }
            }

            return avgStrategy;
        }
    }

    public void train() {
        ArrayList<Tile> allTiles = generateAllTiles();
        for (int i = 0; i < ITERATIONS; i++) {
            ArrayList<Tile> remainingTiles = new ArrayList<>(allTiles);
            ArrayList<Tile> playerHand = dealHand(remainingTiles, 7);
            ArrayList<Tile> opponentHand = dealHand(remainingTiles, 7);
            ArrayList<Tile> board = new ArrayList<>();
            int leftEnd = -1;
            int rightEnd = -1;
            int passCount = 0;

            double utility = cfr(board, playerHand, opponentHand, leftEnd, rightEnd, 1.0, 1.0, passCount);
            if (i % 1000 == 0) {
                System.out.println("Iteration " + i + " of " + ITERATIONS + ", Utility: " + utility + ", Cache size: " + utilityCache.size());
            }
        }
    }

    private double cfr(ArrayList<Tile> board, ArrayList<Tile> playerHand, ArrayList<Tile> opponentHand,
                       int leftEnd, int rightEnd, double p0, double p1, int passCount) {
        if (playerHand.isEmpty()) {
            return 1.0; // Player wins
        }
        if (opponentHand.isEmpty()) {
            return 0.0; // Opponent wins
        }
        if (passCount >= MAX_PASSES) {
            return evaluateStalemate(playerHand, opponentHand); // Stalemate
        }

        boolean playerTurn = p0 > 0;
        ArrayList<Tile> currentHand = playerTurn ? playerHand : opponentHand;

        // Generate a cache key with sorted hand
        String cacheKey = generateCacheKey(board, currentHand, leftEnd, rightEnd, playerTurn, passCount);
        if (utilityCache.containsKey(cacheKey)) {
            return utilityCache.get(cacheKey);
        }

        String infoSet = generateInfoSet(board, currentHand, leftEnd, rightEnd, playerTurn);
        ArrayList<String> actions = getActions(currentHand, leftEnd, rightEnd);

        if (actions.isEmpty()) {
            double nextP0 = playerTurn ? 0.0 : 1.0;
            double nextP1 = playerTurn ? 1.0 : 0.0;
            double utility = cfr(board, playerHand, opponentHand, leftEnd, rightEnd, nextP0, nextP1, passCount + 1);
            utilityCache.put(cacheKey, utility);
            return utility;
        }

        Node node = nodeMap.computeIfAbsent(infoSet, k -> new Node(infoSet, actions));
        double[] strategy = node.getStrategy(playerTurn ? p0 : p1);
        double[] utilities = new double[actions.size()];
        double nodeUtility = 0.0;

        for (int a = 0; a < actions.size(); a++) {
            String action = actions.get(a);
            ArrayList<Tile> nextBoard = new ArrayList<>(board);
            ArrayList<Tile> nextPlayerHand = new ArrayList<>(playerHand);
            ArrayList<Tile> nextOpponentHand = new ArrayList<>(opponentHand);

            Tile tile = parseTile(action.substring(0, action.indexOf("|")));
            boolean playLeft = action.endsWith("Left");
            playMove(nextBoard, nextPlayerHand, nextOpponentHand, tile, playLeft, playerTurn);

            int nextLeftEnd = nextBoard.isEmpty() ? -1 : nextBoard.get(0).getA();
            int nextRightEnd = nextBoard.isEmpty() ? -1 : nextBoard.get(nextBoard.size() - 1).getB();

            utilities[a] = playerTurn ?
                cfr(nextBoard, nextPlayerHand, nextOpponentHand, nextLeftEnd, nextRightEnd, p0 * strategy[a], p1, 0) :
                -cfr(nextBoard, nextPlayerHand, nextOpponentHand, nextLeftEnd, nextRightEnd, p0, p1 * strategy[a], 0);
            nodeUtility += strategy[a] * utilities[a];
        }

        if (playerTurn) {
            for (int a = 0; a < actions.size(); a++) {
                double regret = utilities[a] - nodeUtility;
                node.regretSum.put(actions.get(a), node.regretSum.get(actions.get(a)) + p1 * regret);
            }
        }

        utilityCache.put(cacheKey, nodeUtility);
        return nodeUtility;
    }

    public Tile bestMove(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        String infoSet = generateInfoSet(board, hand, leftEnd, rightEnd, true);
        Node node = nodeMap.get(infoSet);
        if (node == null || node.actions.isEmpty()) {
            return hand.get(0); // Fallback
        }

        double[] avgStrategy = node.getAverageStrategy();
        int bestActionIdx = 0;
        for (int i = 1; i < avgStrategy.length; i++) {
            if (avgStrategy[i] > avgStrategy[bestActionIdx]) {
                bestActionIdx = i;
            }
        }

        String bestAction = node.actions.get(bestActionIdx);
        Tile tile = parseTile(bestAction.substring(0, bestAction.indexOf("|")));
        tile.setSide(bestAction.endsWith("Left") ? 1 : 2);
        return tile;
    }

    private ArrayList<Tile> generateAllTiles() {
        ArrayList<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j <= i; j++) {
                tiles.add(new Tile(j, i));
            }
        }
        return tiles;
    }

    private ArrayList<Tile> dealHand(ArrayList<Tile> pool, int count) {
        ArrayList<Tile> hand = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            hand.add(pool.remove(random.nextInt(pool.size())));
        }
        return hand;
    }

    private String generateInfoSet(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd, boolean playerTurn) {
        ArrayList<Tile> sortedHand = new ArrayList<>(hand);
        Collections.sort(sortedHand, Comparator.comparingInt(Tile::getA).thenComparingInt(Tile::getB));
        StringBuilder sb = new StringBuilder();
        sb.append(leftEnd).append(":").append(rightEnd).append("|");
        for (Tile t : sortedHand) {
            sb.append(t.toString()).append(",");
        }
        sb.append("|").append(playerTurn ? "P" : "O");
        return sb.toString();
    }

    private String generateCacheKey(ArrayList<Tile> board, ArrayList<Tile> hand, int leftEnd, int rightEnd, boolean playerTurn, int passCount) {
        ArrayList<Tile> sortedHand = new ArrayList<>(hand);
        Collections.sort(sortedHand, Comparator.comparingInt(Tile::getA).thenComparingInt(Tile::getB));
        StringBuilder sb = new StringBuilder();
        sb.append(leftEnd).append(":").append(rightEnd).append("|");
        for (Tile t : board) {
            sb.append(t.toString()).append(",");
        }
        sb.append("|");
        for (Tile t : sortedHand) {
            sb.append(t.toString()).append(",");
        }
        sb.append("|").append(playerTurn ? "P" : "O").append("|").append(passCount);
        return sb.toString();
    }

    private ArrayList<String> getActions(ArrayList<Tile> hand, int leftEnd, int rightEnd) {
        ArrayList<String> actions = new ArrayList<>();
        for (Tile tile : hand) {
            if (leftEnd == -1 && rightEnd == -1) { // Empty board
                actions.add(tile.toString() + "|Left");
                actions.add(tile.toString() + "|Right");
            } else {
                if (tile.getA() == leftEnd || tile.getB() == leftEnd) {
                    actions.add(tile.toString() + "|Left");
                }
                if (tile.getA() == rightEnd || tile.getB() == rightEnd) {
                    actions.add(tile.toString() + "|Right");
                }
            }
        }
        return actions;
    }

    private void playMove(ArrayList<Tile> board, ArrayList<Tile> playerHand, ArrayList<Tile> opponentHand,
                          Tile tile, boolean playLeft, boolean playerTurn) {
        ArrayList<Tile> hand = playerTurn ? playerHand : opponentHand;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getA() == tile.getA() && hand.get(i).getB() == tile.getB()) {
                hand.remove(i);
                break;
            }
        }

        if (board.isEmpty()) {
            board.add(new Tile(tile.getA(), tile.getB()));
        } else if (playLeft) {
            int currentLeft = board.get(0).getA();
            if (tile.getA() == currentLeft) {
                board.add(0, new Tile(tile.getB(), tile.getA()));
            } else if (tile.getB() == currentLeft) {
                board.add(0, new Tile(tile.getA(), tile.getB()));
            }
        } else {
            int currentRight = board.get(board.size() - 1).getB();
            if (tile.getB() == currentRight) {
                board.add(new Tile(tile.getA(), tile.getB()));
            } else if (tile.getA() == currentRight) {
                board.add(new Tile(tile.getB(), tile.getA()));
            }
        }
    }

    private Tile parseTile(String action) {
        String[] parts = action.split("\\|")[0].split(":");
        return new Tile(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private double evaluateStalemate(ArrayList<Tile> playerHand, ArrayList<Tile> opponentHand) {
        int playerSum = sumTiles(playerHand);
        int opponentSum = sumTiles(opponentHand);
        double baseScore = 0.5;
        if (playerSum < opponentSum) baseScore += 0.2;
        else if (playerSum > opponentSum) baseScore -= 0.2;
        return Math.max(0.0, Math.min(1.0, baseScore));
    }

    private int sumTiles(ArrayList<Tile> tiles) {
        int sum = 0;
        for (Tile tile : tiles) {
            sum += tile.getA() + tile.getB();
        }
        return sum;
    }

    public static void main(String[] args) {
        CFRAlgorithm cfr = new CFRAlgorithm();
        System.out.println("Training CFR...");
        cfr.train();
        System.out.println("Training complete.");

        ArrayList<Tile> board = new ArrayList<>();
        ArrayList<Tile> hand = new ArrayList<>();
        hand.add(new Tile(0, 0));
        hand.add(new Tile(1, 3));
        hand.add(new Tile(4, 5));
        Tile bestMove = cfr.bestMove(board, hand, -1, -1);
        System.out.println("Best move: " + bestMove + " (Side: " + (bestMove.getSide() == 1 ? "Left" : "Right") + ")");
    }
}