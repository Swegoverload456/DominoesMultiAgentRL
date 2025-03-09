package src;

public class Tile implements Comparable<Tile>{
    private int side1 = -1;
    private int side2 = -1;

    public Tile(int a, int b){
        side1 = a;
        side2 = b;
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
