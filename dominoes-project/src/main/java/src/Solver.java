package src;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api")
public class Solver {

    public static ArrayList<Tile> board = new ArrayList<>();
    public static Player player = new Player();
    public static int leftEnd = -1;
    public static int rightEnd = -1;

    private static MultiLayerNetwork qNetwork;
    static {
        try {
            qNetwork = MultiLayerNetwork.load(new File("dominoes_qnetworkSelfPlay.zip"), true);
            System.out.println("Q-network model loaded successfully.");
        } catch (IOException e) {
            System.err.println("Failed to load Q-network model: " + e.getMessage());
            qNetwork = null;
        }
    }

    // DTO for game state
    public static class GameState {
        @JsonProperty("board")
        private ArrayList<Tile> board;
        @JsonProperty("hand")
        private ArrayList<Tile> hand;
        @JsonProperty("leftEnd")
        private int leftEnd;
        @JsonProperty("rightEnd")
        private int rightEnd;

        public ArrayList<Tile> getBoard() { return board; }
        public void setBoard(ArrayList<Tile> board) { this.board = board; }
        public ArrayList<Tile> getHand() { return hand; }
        public void setHand(ArrayList<Tile> hand) { this.hand = hand; }
        public int getLeftEnd() { return leftEnd; }
        public void setLeftEnd(int leftEnd) { this.leftEnd = leftEnd; }
        public int getRightEnd() { return rightEnd; }
        public void setRightEnd(int rightEnd) { this.rightEnd = rightEnd; }
    }

    // DTO for Tile (ensure JSON serialization)
    public static class Tile {
        @JsonProperty("side1")
        private int side1;
        @JsonProperty("side2")
        private int side2;

        public Tile() {}

        public Tile(int side1, int side2) {
            this.side1 = side1;
            this.side2 = side2;
        }

        public int getSide1() { return side1; }
        public void setSide1(int side1) { this.side1 = side1; }
        public int getSide2() { return side2; }
        public void setSide2(int side2) { this.side2 = side2; }

        public int getA() { return side1; }
        public int getB() { return side2; }
    }

    @PostMapping("/predict")
    public String predictMove(@RequestBody GameState gameState) {
        // Update game state
        board.clear();
        for (Tile t : gameState.getBoard()) {
            board.add(new Tile(t.getSide1(), t.getSide2()));
        }
        player = new Player();
        for (Tile t : gameState.getHand()) {
            player.add(new Tile(t.getSide1(), t.getSide2()));
        }
        leftEnd = gameState.getLeftEnd();
        rightEnd = gameState.getRightEnd();

        return getQNetworkSuggestion(player, board, leftEnd, rightEnd);
    }

    private static String getQNetworkSuggestion(Player player, ArrayList<Tile> board, int leftEnd, int rightEnd) {
        if (qNetwork == null) {
            return "Model not loaded";
        }

        double[] state = new double[490];
        player.getState(board, leftEnd, rightEnd, state);
        INDArray input = Nd4j.createFromArray(new double[][]{state});
        INDArray qValues = qNetwork.output(input);

        boolean[] valid = player.getValidActions(leftEnd, rightEnd);

        for (int i = 0; i < 57; i++) {
            if (!valid[i]) {
                qValues.putScalar(new int[]{0, i}, Double.NEGATIVE_INFINITY);
            }
        }

        int bestAction = qValues.argMax(1).getInt(0);
        double bestQValue = qValues.getDouble(0, bestAction);
        
        if (bestAction == 56) {
            return String.format("Pass (Q: %.3f)", bestQValue);
        } else {
            int tileIdx = bestAction % 28;
            Tile tile = Game.allTiles.get(tileIdx);
            String side = (bestAction < 28) ? "Left" : "Right";
            return String.format("%s on %s (Q: %.3f)", tile.toString(), side, bestQValue);
        }
    }

    public static void addToBoard(Tile t, int side) {
        if (board.size() == 0) {
            board.add(new Tile(t.getA(), t.getB()));
            leftEnd = t.getA();
            rightEnd = t.getB();
            return;
        }

        if (side > 0) {
            if (side == 1) {
                if (t.getA() == leftEnd) {
                    board.add(0, new Tile(t.getB(), t.getA()));
                    leftEnd = t.getB();
                } else if (t.getB() == leftEnd) {
                    board.add(0, new Tile(t.getA(), t.getB()));
                    leftEnd = t.getA();
                } else {
                    board.add(0, new Tile(t.getA(), t.getB()));
                    leftEnd = t.getA();
                }
                return;
            } else if (side == 2) {
                if (t.getA() == rightEnd) {
                    board.add(board.size(), new Tile(t.getA(), t.getB()));
                    rightEnd = t.getB();
                } else if (t.getB() == rightEnd) {
                    board.add(board.size(), new Tile(t.getB(), t.getA()));
                    rightEnd = t.getA();
                } else {
                    board.add(board.size(), new Tile(t.getA(), t.getB()));
                    rightEnd = t.getB();
                }
                return;
            }
        }

        if (t.getA() == leftEnd || t.getB() == leftEnd) {
            if (t.getA() == leftEnd) {
                board.add(0, new Tile(t.getB(), t.getA()));
                leftEnd = t.getB();
            } else if (t.getB() == leftEnd) {
                board.add(0, new Tile(t.getA(), t.getB()));
                leftEnd = t.getA();
            } else {
                board.add(0, new Tile(t.getA(), t.getB()));
                leftEnd = t.getA();
            }
            return;
        }

        if (t.getA() == rightEnd || t.getB() == rightEnd) {
            if (t.getA() == rightEnd) {
                board.add(board.size(), new Tile(t.getA(), t.getB()));
                rightEnd = t.getB();
            } else if (t.getB() == rightEnd) {
                board.add(board.size(), new Tile(t.getB(), t.getA()));
                rightEnd = t.getA();
            } else {
                board.add(board.size(), new Tile(t.getA(), t.getB()));
                rightEnd = t.getB();
            }
            return;
        }
    }
}