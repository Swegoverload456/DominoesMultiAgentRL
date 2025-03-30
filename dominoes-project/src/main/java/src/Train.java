package src;

import java.io.IOException;

public class Train {
    static Game g = new Game();

    public static void main(String[] args) throws IOException {
        g.trainAgents(100000);
    }
}
