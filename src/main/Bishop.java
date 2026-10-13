package main;

public class Bishop extends piece {
    public Bishop(int color,int col,int row){
        super (color,col,row);
        type = Type.BISHOP;

        if (color == GamePanel.WHITE){
            image=getImage("/piece/w-bishop");
        }else{
            image=getImage("/piece/b-bishop");
        }

    }

    @Override
    public boolean attacks(int targetCol,int targetRow){
        return isDiagonalLine(targetCol,targetRow) && isPathClear(targetCol,targetRow);
    }
}
