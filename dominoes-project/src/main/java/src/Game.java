package src;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class Game {
    
    public static ArrayList<Tile> set = new ArrayList<>();

    public static MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
        .updater(new Adam(0.001))
        .list()
        .layer(0, new DenseLayer.Builder().nIn(490).nOut(256).activation(Activation.RELU).build())
        .layer(1, new DenseLayer.Builder().nIn(256).nOut(256).activation(Activation.RELU).build())
        .layer(2, new DenseLayer.Builder().nIn(256).nOut(256).activation(Activation.RELU).build())
        .layer(3, new DenseLayer.Builder().nIn(256).nOut(256).activation(Activation.RELU).build())
        .layer(4, new DenseLayer.Builder().nIn(256).nOut(128).activation(Activation.RELU).build())
        .layer(5, new OutputLayer.Builder().nIn(128).nOut(57).activation(Activation.IDENTITY)
            .lossFunction(LossFunctions.LossFunction.MSE).build())
        .build();

    public static MultiLayerNetwork qNetwork;
    public static MultiLayerNetwork targetNetwork;

    static {
        qNetwork = new MultiLayerNetwork(conf);
        qNetwork.init();
        targetNetwork = qNetwork.clone();
    }

    public static ArrayList<Tile> allTiles = new ArrayList<>();
    static {
        for (int i = 0; i < 7; i++) {
            for (int j = i; j < 7; j++) {
                allTiles.add(new Tile(i, j));
            }
        }
    }

    public static Player[] players = new Player[4];
    public static ArrayList<Tile> board = new ArrayList<>();
    public static int leftEnd = -1, rightEnd = -1;

    public static void trainAgents(int episodes) throws IOException {
        ArrayDeque<double[]> replayBuffer = new ArrayDeque<>(60000);
        Random rand = new Random();
        double epsilon = 1.0, epsilonMin = 0.01, epsilonDecay = 0.999; // Slower decay
        int batchSize = 32, targetUpdateFreq = 100;
        int step = 0;

        int percentageInterval = episodes / 100;

        double[][] stateBuffers = new double[4][490];
        double[][] nextStateBuffers = new double[4][490];
        ArrayList<ArrayList<double[]>> pending = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            stateBuffers[i] = new double[490];
            nextStateBuffers[i] = new double[490];
            pending.add(new ArrayList<>(10));
        }

        for (int ep = 0; ep < episodes; ep++) {
            if (ep % (percentageInterval / 10) == 0) {
                System.out.printf("\r%.1f%% done", (ep * 100.0) / episodes);
            }

            set.clear();
            for (int i = 0; i < 7; i++) for (int j = i; j < 7; j++) set.add(new Tile(i, j));
            Collections.shuffle(set);
            for (int i = 0; i < 4; i++) players[i] = new Player();
            int c = 0;
            for (int i = 0; i < 4; i++) for (int j = 0; j < 7; j++) players[i].add(set.get(c++));
            board.clear();
            leftEnd = -1;
            rightEnd = -1;
            int consecutivePasses = 0;
            int turn = rand.nextInt(4);

            for (int i = 0; i < 4; i++) pending.get(i).clear();

            while (true) {
                Player p = players[turn];
                Arrays.fill(stateBuffers[turn], 0);
                p.getState(board, leftEnd, rightEnd, stateBuffers[turn]);
                boolean[] valid = p.getValidActions(leftEnd, rightEnd);
                int a;

                INDArray qValues = qNetwork.output(Nd4j.createFromArray(new double[][]{stateBuffers[turn]}));
                if (rand.nextDouble() < epsilon) {
                    List<Integer> validIdx = new ArrayList<>();
                    for (int i = 0; i < 57; i++) if (valid[i]) validIdx.add(i);
                    a = validIdx.get(rand.nextInt(validIdx.size()));
                } else {
                    for (int i = 0; i < 57; i++) if (!valid[i]) qValues.putScalar(new int[]{0, i}, Double.NEGATIVE_INFINITY);
                    a = qValues.argMax(1).getInt(0);
                }

                int side = a < 28 ? 1 : (a == 56 ? 0 : 2);
                Tile tile = a == 56 ? null : Game.allTiles.get(a % 28);
                double reward = (tile != null) ? 0.1 : -0.1; // Intermediate rewards
                if (tile != null) {
                    addToBoard(tile, side);
                    p.remove(tile.getA(), tile.getB());
                    consecutivePasses = 0;
                } else {
                    consecutivePasses++;
                }

                Arrays.fill(nextStateBuffers[turn], 0);
                p.getState(board, leftEnd, rightEnd, nextStateBuffers[turn]);
                pending.get(turn).add(createExperience(stateBuffers[turn], a, reward, nextStateBuffers[turn], 0.0));

                int winner = -1;
                if (p.size() == 0) {
                    winner = turn;
                } else if (consecutivePasses == 4) {
                    winner = 0;
                    for (int i = 1; i < 4; i++) if (players[i].sum() < players[winner].sum()) winner = i;
                }
                if (winner != -1) {
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < pending.get(i).size(); j++) {
                            double[] exp = pending.get(i).get(j);
                            double r = (i == winner) ? 1.0 + exp[491] : exp[491]; // Add final reward to intermediates
                            replayBuffer.add(createExperience(exp, (int) exp[490], r, null, 1.0));
                        }
                    }
                    break;
                } else if (pending.get(turn).size() >= 4) {
                    double[] prev = pending.get(turn).remove(0);
                    replayBuffer.add(prev);
                    if (replayBuffer.size() > 5000) replayBuffer.removeFirst();
                }

                turn = (turn + 1) % 4;
            }

            if (replayBuffer.size() >= batchSize) {
                step++;
                List<double[]> batch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && !replayBuffer.isEmpty(); i++) {
                    batch.add(replayBuffer.poll());
                }
                double[][] inputs = new double[batchSize][490];
                double[][] targets = new double[batchSize][57];
                for (int i = 0; i < batch.size(); i++) {
                    double[] sample = batch.get(i);
                    System.arraycopy(sample, 0, inputs[i], 0, 490);
                    int action = (int) sample[490];
                    double reward = sample[491];
                    double[] nextS = sample[492] == -1 ? null : new double[490];
                    if (nextS != null) System.arraycopy(sample, 492, nextS, 0, 490);
                    double done = sample[982];

                    INDArray input = Nd4j.createFromArray(new double[][]{inputs[i]});
                    INDArray targetQ = qNetwork.output(input).dup();
                    if (done == 1.0) {
                        targetQ.putScalar(0, action, reward);
                    } else {
                        INDArray nextQ = targetNetwork.output(Nd4j.createFromArray(new double[][]{nextS}));
                        double maxNextQ = nextQ.maxNumber().doubleValue();
                        targetQ.putScalar(0, action, reward + 0.99 * maxNextQ);
                    }
                    targets[i] = targetQ.getRow(0).toDoubleVector();
                }
                qNetwork.fit(Nd4j.create(inputs), Nd4j.create(targets));

                if (step % targetUpdateFreq == 0) {
                    targetNetwork = qNetwork.clone();
                }
            }
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        System.out.println("Training complete. Saving model...");
        qNetwork.save(new File("dominoes_qnetworkSelfPlay2.zip"));
    }
    
    private static double[] createExperience(double[] state, int action, double reward, double[] nextState, double done) {
        double[] exp = new double[983];
        System.arraycopy(state, 0, exp, 0, 490);
        exp[490] = action;
        exp[491] = reward;
        if (nextState != null) {
            System.arraycopy(nextState, 0, exp, 492, 490);
        } else {
            for (int i = 492; i < 982; i++) exp[i] = -1;
        }
        exp[982] = done;
        return exp;
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

    // Removed main method to keep focus on training
}