package src;

public class Tile implements Comparable<Tile>{
    private int side1 = -1;
    private int side2 = -1;
    private int left = 1;

    public Tile(int a, int b){
        side1 = a;
        side2 = b;
    }

    public void setSide(int s){
        left = s;
    }

    public int getSide(){
        return left;
    }

    public int getA(){
        return side1;
    }

    public int getB(){
        return side2;
    }

    public int sum(){
        return side1 + side2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Tile)) return false;
        Tile other = (Tile) obj;
        return (side1 == other.side1 && side2 == other.side2) || 
               (side1 == other.side2 && side2 == other.side1); // Consider [1:2] == [2:1] if desired
    }

    @Override
    public int hashCode() {
        int min = Math.min(side1, side2);
        int max = Math.max(side1, side2);
        return 31 * min + max; // Consistent hash for [1:2] and [2:1]
    }

    public String toString(){
        return side1 + ":" + side2;
    }

    public int compareTo(Tile b){
        if(b.getB() == getB()){
            if(b.getA() < getA()){
                return 1;
            }
            else if(b.getA() > getA()){
                return -1;
            }
            else{
                return 0;
            }
        }

        if(b.getB() < getB()){
            return 1;
        }
        else if(b.getB() > getB()){
            return -1;
        }
        else{
            return 0;
        } 
    }
}
