package src;

import java.util.Random;

public class temo {
    public static void main(String[] args) {
        int[] w = {0,0,0,0};
        Random r = new Random();

        int offset = 3;
        int winner = 2;

        System.out.println("Offset: " + offset + "\tWinner: " + winner);

        int pos = 0;

        for(int j = 0; j < 4; j++){
            if(offset == winner){
                j = 4;
            }
            else{
                offset++;
                pos++;
                if(offset > 3){
                    offset = 0;
                }
            }
        }

        System.out.println("Pos: " + pos);

    }
}
