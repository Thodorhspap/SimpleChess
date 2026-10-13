package main;

public class Knight extends piece {
    public Knight (int color,int col,int row){
        super (color,col,row);
        type = Type.KNIGHT;

    if (color == GamePanel.WHITE){
        image=getImage("/piece/w-knight");
    }else if (color == GamePanel.BLACK){
        image=getImage("/piece/b-knight");
    }
}

    // σχήμα Γ: 2 τετράγωνα προς μία κατεύθυνση και 1 προς την άλλη
    @Override
    public boolean attacks(int targetCol,int targetRow){
        return Math.abs(targetCol-col)*Math.abs(targetRow-row)==2;
    }
}
