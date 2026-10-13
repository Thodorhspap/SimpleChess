package main;

public class Queen extends piece {
    public Queen(int color, int col, int row) {
        super(color, col, row);
        type = Type.QUEEN;
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-queen");
        } else {
            image = getImage("/piece/b-queen");
        }
    }

    @Override
    public boolean attacks(int targetCol,int targetRow){
        return (isStraightLine(targetCol,targetRow) || isDiagonalLine(targetCol,targetRow))
                && isPathClear(targetCol,targetRow);
    }
}
