package src;

public class MoveResult {
    private Tile tile;
    private double winRate;

    public MoveResult(Tile tile, double winRate) {
        this.tile = tile;
        this.winRate = winRate;
    }

    public Tile getTile() {
        return tile;
    }

    public double getWinRate() {
        return winRate;
    }
}