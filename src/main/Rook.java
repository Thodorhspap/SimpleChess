package main;

public class Rook extends piece {
    public Rook(int color,int col ,int row){
        super (color ,col,row);
        type = Type.ROOK;
        if (color == GamePanel.WHITE){
            image=getImage("/piece/w-rook");
        }else{
            image=getImage("/piece/b-rook");
        }
    }

    @Override
    public boolean attacks(int targetCol,int targetRow){
        return isStraightLine(targetCol,targetRow) && isPathClear(targetCol,targetRow);
    }
}
