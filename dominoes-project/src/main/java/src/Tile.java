package src;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tile implements Comparable<Tile> {
    @JsonProperty("side1")
    private int side1 = -1;
    @JsonProperty("side2")
    private int side2 = -1;
    private int left = 1;

    public Tile() {} // Default constructor for Jackson

    public Tile(int a, int b) {
        side1 = a;
        side2 = b;
    }

    public void setSide(int s) {
        left = s;
    }

    public int getSide() {
        return left;
    }

    public int getA() {
        return side1;
    }

    public int getB() {
        return side2;
    }

    public int sum() {
        return side1 + side2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Tile)) return false;
        Tile other = (Tile) obj;
        return (side1 == other.side1 && side2 == other.side2) || 
               (side1 == other.side2 && side2 == other.side1);
    }

    @Override
    public int hashCode() {
        int min = Math.min(side1, side2);
        int max = Math.max(side1, side2);
        return 31 * min + max;
    }

    public String toString() {
        return side1 + ":" + side2;
    }

    public int compareTo(Tile b) {
        if (b.getB() == getB()) {
            if (b.getA() < getA()) {
                return 1;
            } else if (b.getA() > getA()) {
                return -1;
            } else {
                return 0;
            }
        }

        if (b.getB() < getB()) {
            return 1;
        } else if (b.getB() > getB()) {
            return -1;
        } else {
            return 0;
        } 
    }

    // Getters and setters for Jackson
    public int getSide1() { return side1; }
    public void setSide1(int side1) { this.side1 = side1; }
    public int getSide2() { return side2; }
    public void setSide2(int side2) { this.side2 = side2; }
}